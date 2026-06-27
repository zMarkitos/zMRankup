package dev.zm.rankup.api;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.rank.RankProgress;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.storage.PlayerData;
import org.bukkit.entity.Player;

import java.util.List;

public class ZMRankupAPI {

    private static zMRankup plugin;

    public static void init(zMRankup instance) {
        plugin = instance;
    }

    public static Rank getCurrentRank(Player player) {
        return plugin.getRankManager().getCurrentRank(player);
    }

    public static Rank getNextRank(Player player) {
        return plugin.getRankManager().getNextRank(player);
    }

    public static RankProgress getProgress(Player player, Rank rank) {
        if (rank == null)
            return null;
        return rank.getProgress(player);
    }

    public static boolean forceRankup(Player player, String rankId) {
        Rank rank = plugin.getRankManager().getRank(rankId);
        if (rank == null)
            return false;
        return plugin.getRankManager().forceRankup(player, rank);
    }

    public static void registerRequirement(String type,
            dev.zm.rankup.requirement.RequirementRegistry.RequirementFactory factory) {
        plugin.getRequirementRegistry().register(type, factory);
    }

    public static void registerReward(String type, dev.zm.rankup.reward.RewardRegistry.RewardFactory factory) {
        plugin.getRewardRegistry().register(type, factory);
    }

    public static Rank getRank(String id) {
        return plugin.getRankManager().getRank(id);
    }

    public static List<Rank> getAllRanks() {
        return plugin.getRankManager().getAllRanks();
    }

    public static PlayerData getPlayerData(Player player) {
        return plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
    }

    public static int getPrestigeLevel(Player player, String systemId) {
        RankupSystem system = plugin.getSystemManager().getSystem(systemId);
        if (system == null) {
            return 0;
        }
        return plugin.getPrestigeManager() != null ? plugin.getPrestigeManager().getPrestigeLevel(player, system) : 0;
    }

    public static boolean prestige(Player player, String systemId) {
        RankupSystem system = plugin.getSystemManager().getSystem(systemId);
        return system != null && plugin.getPrestigeManager() != null
                && plugin.getPrestigeManager().prestige(player, system);
    }
}
