package dev.zm.rankup.storage;

import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataCache {

    private final zMRankup plugin;
    private final ConcurrentHashMap<UUID, PlayerData> cache;
    private final StorageManager storageManager;

    public PlayerDataCache(zMRankup plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
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
        storageManager.loadPlayerData(uuid).thenAccept(data ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    PlayerData current = cache.get(uuid);
                    if (current == null) {
                        data.syncRealStatistics(player);
                        cache.put(uuid, data);
                        return;
                    }

                    if (current.isDirty()) {
                        current.mergeFrom(data);
                    } else {
                        current.replaceWith(data);
                    }

                    current.syncRealStatistics(player);
                })
        );
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.isDirty()) {
            storageManager.savePlayerData(data);
            data.markClean();
        }
    }

    public void saveAndRemove(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            storageManager.savePlayerData(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                storageManager.savePlayerData(data);
                data.markClean();
            }
        }
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}
