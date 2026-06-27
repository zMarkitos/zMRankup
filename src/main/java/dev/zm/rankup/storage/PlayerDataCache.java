package dev.zm.rankup.storage;

import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
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
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-LOAD-START] Iniciando carga de DB para " + uuid);
        }
        storageManager.loadPlayerData(uuid).thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-LOAD-END] DB devolvió: prestiges="
                        + data.getPrestiges() + " ranks=" + data.getSystemRanks());
            }

            PlayerData current = cache.get(uuid);
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-LOAD-CACHE] Cache actual: "
                        + (current == null ? "null"
                                : "prestiges=" + current.getPrestiges()
                                        + " isDirty=" + current.isDirty()));
            }

            if (pendingQuit.remove(uuid) != null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-LOAD-PENDINGQUIT] pendingQuit para " + uuid);
                }
                storageManager.savePlayerData(data);
                return;
            }

            if (!player.isOnline()) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-LOAD-OFFLINE] Jugador offline al cargar " + uuid);
                }
                storageManager.savePlayerData(data);
                return;
            }

            if (current == null) {
                data.syncRealStatistics(player);
                cache.put(uuid, data);
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-LOAD-PUT] Puesto en cache: prestiges=" + data.getPrestiges());
                }
                if (plugin.getPermissionManager() != null)
                    plugin.getPermissionManager().refreshPlayer(player);
                return;
            }

            if (current.isDirty()) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-LOAD-MERGE] Haciendo mergeFrom");
                }
                current.mergeFrom(data);
            } else {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-LOAD-REPLACE] Haciendo replaceWith");
                }
                current.replaceWith(data);
            }

            current.syncRealStatistics(player);
            if (plugin.getPermissionManager() != null)
                plugin.getPermissionManager().refreshPlayer(player);
        }));
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.isDirty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-SAVE] Guardando " + uuid
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
                plugin.getLogger().info("[DEBUG-QUIT] Guardando al salir " + uuid
                        + " prestiges=" + data.getPrestiges()
                        + " ranks=" + data.getSystemRanks());
            }
            storageManager.savePlayerData(data);
        } else {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-QUIT] NULL en cache para " + uuid + " -> pendingQuit");
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

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
        pendingQuit.remove(uuid);
    }
}