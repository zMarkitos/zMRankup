package dev.zm.rankup.storage;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private UUID uuid;
    private Map<String, String> systemRanks;
    private int totalRankups;
    private long lastRankupTime;
    private Map<String, Long> mobKills;
    private Map<String, Long> blocksMined;
    private Map<String, Integer> prestiges;
    private long totalBlocksMined;
    private long totalMobKills;
    private long playerKills;
    private long deaths;
    private boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.systemRanks = new ConcurrentHashMap<>();
        this.mobKills = new ConcurrentHashMap<>();
        this.blocksMined = new ConcurrentHashMap<>();
        this.prestiges = new ConcurrentHashMap<>();
        this.dirty = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        this.dirty = true;
    }

    public String getCurrentRankId(String systemId) {
        return systemRanks.get(systemId);
    }

    public void setCurrentRankId(String systemId, String rankId) {
        if (rankId == null) {
            this.systemRanks.remove(systemId);
        } else {
            this.systemRanks.put(systemId, rankId);
        }
        this.dirty = true;
    }

    public Map<String, String> getSystemRanks() {
        return systemRanks;
    }

    public void setSystemRanks(Map<String, String> systemRanks) {
        this.systemRanks = systemRanks;
        this.dirty = true;
    }

    public int getTotalRankups() {
        return totalRankups;
    }

    public void setTotalRankups(int totalRankups) {
        this.totalRankups = totalRankups;
        this.dirty = true;
    }

    public long getLastRankupTime() {
        return lastRankupTime;
    }

    public void setLastRankupTime(long lastRankupTime) {
        this.lastRankupTime = lastRankupTime;
        this.dirty = true;
    }

    public Map<String, Long> getMobKills() {
        return mobKills;
    }

    public void setMobKills(Map<String, Long> mobKills) {
        this.mobKills = mobKills;
        this.dirty = true;
    }

    public Map<String, Long> getBlocksMined() {
        return blocksMined;
    }

    public void setBlocksMined(Map<String, Long> blocksMined) {
        this.blocksMined = blocksMined;
        this.dirty = true;
    }

    public Map<String, Integer> getPrestiges() {
        return prestiges;
    }

    public void setPrestiges(Map<String, Integer> prestiges) {
        this.prestiges = prestiges;
        this.dirty = true;
    }

    public int getPrestigeLevel(String systemId) {
        if (systemId == null) {
            return 0;
        }
        return prestiges.getOrDefault(systemId, 0);
    }

    public void setPrestigeLevel(String systemId, int level) {
        if (systemId == null) {
            return;
        }
        if (level <= 0) {
            this.prestiges.remove(systemId);
        } else {
            this.prestiges.put(systemId, level);
        }
        this.dirty = true;
    }

    public void incrementPrestige(String systemId) {
        if (systemId == null) {
            return;
        }
        this.prestiges.put(systemId, getPrestigeLevel(systemId) + 1);
        this.dirty = true;
    }

    public void decrementPrestige(String systemId) {
        if (systemId == null) {
            return;
        }
        int current = getPrestigeLevel(systemId);
        if (current <= 1) {
            this.prestiges.remove(systemId);
        } else {
            this.prestiges.put(systemId, current - 1);
        }
        this.dirty = true;
    }

    public void resetPrestige(String systemId) {
        if (systemId == null) {
            return;
        }
        this.prestiges.remove(systemId);
        this.dirty = true;
    }

    public long getTotalBlocksMined() {
        return totalBlocksMined;
    }

    public void setTotalBlocksMined(long totalBlocksMined) {
        this.totalBlocksMined = totalBlocksMined;
        this.dirty = true;
    }

    public long getTotalMobKills() {
        return totalMobKills;
    }

    public void setTotalMobKills(long totalMobKills) {
        this.totalMobKills = totalMobKills;
        this.dirty = true;
    }

    public long getPlayerKills() {
        return playerKills;
    }

    public void setPlayerKills(long playerKills) {
        this.playerKills = playerKills;
        this.dirty = true;
    }

    public long getDeaths() {
        return deaths;
    }

    public void setDeaths(long deaths) {
        this.deaths = deaths;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }



    public void incrementRankups() {
        this.totalRankups++;
        this.dirty = true;
    }

    /**
     * Increments the specific mob kill count, and also automatically
     * increments the totalMobKills count. Do not call incrementTotalMobKills
     * in addition to this method to avoid double-counting.
     */
    public void incrementMobKills(String mobType, long amount) {
        this.mobKills.put(mobType, this.mobKills.getOrDefault(mobType, 0L) + amount);
        this.totalMobKills += amount;
        this.dirty = true;
    }

    /**
     * Increments the specific block mined count, and also automatically
     * increments the totalBlocksMined count. Do not call incrementTotalBlocksMined
     * in addition to this method to avoid double-counting.
     */
    public void incrementBlocksMined(String blockType, long amount) {
        this.blocksMined.put(blockType, this.blocksMined.getOrDefault(blockType, 0L) + amount);
        this.totalBlocksMined += amount;
        this.dirty = true;
    }

    public void incrementPlayerKills(long amount) {
        this.playerKills += amount;
        this.dirty = true;
    }

    public void incrementDeaths(long amount) {
        this.deaths += amount;
        this.dirty = true;
    }

    public void incrementTotalBlocksMined(long amount) {
        this.totalBlocksMined += amount;
        this.dirty = true;
    }

    public void incrementTotalMobKills(long amount) {
        this.totalMobKills += amount;
        this.dirty = true;
    }



    public long getSpecificMobKills(String mobType) {
        return mobKills.getOrDefault(mobType, 0L);
    }

    public long getSpecificBlocksMined(String blockType) {
        return blocksMined.getOrDefault(blockType, 0L);
    }

    public void syncRealStatistics(Player player) {
        if (player == null) {
            return;
        }

        setIfChanged(player.getStatistic(Statistic.PLAYER_KILLS), this.playerKills, value -> this.playerKills = value);
        setIfChanged(player.getStatistic(Statistic.DEATHS), this.deaths, value -> this.deaths = value);
        setIfChanged(getLiveTotalBlocksMined(player), this.totalBlocksMined, value -> this.totalBlocksMined = value);
        setIfChanged(player.getStatistic(Statistic.MOB_KILLS), this.totalMobKills, value -> this.totalMobKills = value);

        syncTrackedMobKills(player);
        syncTrackedBlocksMined(player);
    }

    public long getLiveTotalBlocksMined(Player player) {
        if (player == null) {
            return 0L;
        }

        long total = 0L;
        for (String materialName : blocksMined.keySet()) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && material.isBlock()) {
                try {
                    total += player.getStatistic(Statistic.MINE_BLOCK, material);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return total > this.totalBlocksMined ? total : this.totalBlocksMined;
    }

    void replaceWith(PlayerData other) {
        if (other == null) {
            return;
        }

        this.uuid = other.uuid;
        this.systemRanks = new ConcurrentHashMap<>(other.systemRanks);
        this.totalRankups = other.totalRankups;
        this.lastRankupTime = other.lastRankupTime;
        this.mobKills = new ConcurrentHashMap<>(other.mobKills);
        this.blocksMined = new ConcurrentHashMap<>(other.blocksMined);
        this.prestiges = new ConcurrentHashMap<>(other.prestiges);
        this.totalBlocksMined = other.totalBlocksMined;
        this.totalMobKills = other.totalMobKills;
        this.playerKills = other.playerKills;
        this.deaths = other.deaths;
        this.dirty = other.dirty;
    }

    void mergeFrom(PlayerData other) {
        if (other == null) {
            return;
        }

        for (java.util.Map.Entry<String, String> entry : other.systemRanks.entrySet()) {
            this.systemRanks.putIfAbsent(entry.getKey(), entry.getValue());
        }
        this.totalRankups += other.totalRankups;
        this.lastRankupTime = Math.max(this.lastRankupTime, other.lastRankupTime);
        mergeLongMap(this.mobKills, other.mobKills);
        mergeLongMap(this.blocksMined, other.blocksMined);
        mergeIntMap(this.prestiges, other.prestiges);
        this.totalBlocksMined += other.totalBlocksMined;
        this.totalMobKills += other.totalMobKills;
        this.playerKills += other.playerKills;
        this.deaths += other.deaths;
        this.dirty = this.dirty || other.dirty;
    }

    private void syncTrackedMobKills(Player player) {
        for (Entry<String, Long> entry : mobKills.entrySet()) {
            EntityType entityType = parseEntityType(entry.getKey());
            if (entityType != null) {
                long realValue = player.getStatistic(Statistic.KILL_ENTITY, entityType);
                if (realValue != entry.getValue()) {
                    entry.setValue(realValue);
                    dirty = true;
                }
            }
        }
    }

    private void syncTrackedBlocksMined(Player player) {
        for (Entry<String, Long> entry : blocksMined.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey());
            if (material != null) {
                long realValue = player.getStatistic(Statistic.MINE_BLOCK, material);
                if (realValue != entry.getValue()) {
                    entry.setValue(realValue);
                    dirty = true;
                }
            }
        }
    }

    private void mergeLongMap(Map<String, Long> target, Map<String, Long> source) {
        for (Entry<String, Long> entry : source.entrySet()) {
            target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0L) + entry.getValue());
        }
    }

    private void mergeIntMap(Map<String, Integer> target, Map<String, Integer> source) {
        for (Entry<String, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    private void setIfChanged(long realValue, long currentValue, LongSetter setter) {
        if (realValue != currentValue) {
            setter.set(realValue);
            dirty = true;
        }
    }

    @FunctionalInterface
    private interface LongSetter {
        void set(long value);
    }

    private EntityType parseEntityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return EntityType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            try {
                return EntityType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ignoredToo) {
                return null;
            }
        }
    }
}
