package dev.zm.rankup.storage;

import dev.zm.rankup.util.SchedulerUtil;
import dev.zm.rankup.zMRankup;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataCache {

    private final zMRankup plugin;
    private final ConcurrentHashMap<UUID, PlayerData> cache;
    private final ConcurrentHashMap<UUID, Boolean> pendingQuit;
    private final StorageManager storageManager;

    public PlayerDataCache(zMRankup plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.pendingQuit = new ConcurrentHashMap<>();
        this.storageManager = plugin.getStorageManager();
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public void load(UUID uuid) {
        storageManager.loadPlayerData(uuid).thenAccept(data -> cache.put(uuid, data));
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();
        storageManager.loadPlayerData(uuid).thenAccept(data -> {
            // Thread-safe fast paths: the player left or already has a pending quit.
            if (pendingQuit.remove(uuid) != null || !player.isOnline()) {
                storageManager.savePlayerData(data);
                return;
            }
            // Player is still online: sync statistics and refresh permissions on its
            // thread.
            SchedulerUtil.runOnEntity(plugin, player, () -> applyLoadedData(player, uuid, data));
        });
    }

    private void applyLoadedData(Player player, UUID uuid, PlayerData data) {
        if (pendingQuit.remove(uuid) != null || !player.isOnline()) {
            storageManager.savePlayerData(data);
            return;
        }

        PlayerData current = cache.get(uuid);
        if (current == null) {
            data.syncRealStatistics(player);
            cache.put(uuid, data);
        } else if (current.isDirty()) {
            current.mergeFrom(data);
            current.syncRealStatistics(player);
        } else {
            current.replaceWith(data);
            current.syncRealStatistics(player);
        }

        if (plugin.getPermissionManager() != null)
            plugin.getPermissionManager().refreshPlayer(player);
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.isDirty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-SAVE] saved " + uuid
                        + " prestiges=" + data.getPrestiges()
                        + " ranks=" + data.getSystemRanks());
            }
            storageManager.savePlayerData(data).thenRun(data::markClean);
        } else {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-SAVE] SKIP " + uuid
                        + " data=" + (data == null ? "null" : "isDirty=" + data.isDirty()));
            }
        }
    }

    public void saveAndRemove(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-QUIT] exit saved " + uuid
                        + " prestiges=" + data.getPrestiges()
                        + " ranks=" + data.getSystemRanks());
            }
            storageManager.savePlayerData(data);
        } else {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-QUIT] NULL in cache for " + uuid + " -> pendingQuit");
            }
            pendingQuit.put(uuid, Boolean.TRUE);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                storageManager.savePlayerData(data).thenRun(data::markClean);
            }
        }
    }
}