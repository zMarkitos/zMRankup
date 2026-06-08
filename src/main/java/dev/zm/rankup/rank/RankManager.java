package dev.zm.rankup.rank;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.event.RankupEvent;
import dev.zm.rankup.event.RankupFailEvent;
import dev.zm.rankup.event.RankupSuccessEvent;
import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.requirement.impl.VaultBalanceRequirement;
import dev.zm.rankup.reward.Reward;
import dev.zm.rankup.reward.RewardExecutor;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.config.PlaceholderContext;
import dev.zm.rankup.system.RankupSystem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class RankManager {

    private final zMRankup plugin;

    public RankManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void loadRanks() {
        // No longer used, systems are loaded by SystemManager
    }

    public Rank getRank(String id) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? def.getRank(id) : null;
    }

    public List<Rank> getAllRanks() {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? def.getAllRanks() : Collections.emptyList();
    }

    public Rank getCurrentRank(Player player) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? getCurrentRank(def, player) : null;
    }

    public Rank getCurrentRank(RankupSystem system, Player player) {
        if (system == null) return null;
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        String rankId = data.getCurrentRankId(system.getId());
        if (rankId == null) return null;
        return system.getRank(rankId);
    }

    public Rank getNextRank(Player player) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? getNextRank(def, player) : null;
    }

    public Rank getNextRank(RankupSystem system, Player player) {
        if (system == null) return null;
        Rank current = getCurrentRank(system, player);
        if (current == null) {
            List<Rank> all = system.getAllRanks();
            return all.isEmpty() ? null : all.get(0);
        }

        List<Rank> all = system.getAllRanks();
        int idx = all.indexOf(current);
        if (idx < 0 || idx + 1 >= all.size()) return null;
        return all.get(idx + 1);
    }

    public int getRankIndex(String rankId) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? def.getRankIndex(rankId) : -1;
    }

    public int getMaxPage() {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? def.getMaxPage() : 1;
    }

    public int rankupToMax(Player player) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null ? rankupToMax(def, player) : 0;
    }

    public int rankupToMax(RankupSystem system, Player player) {
        if (system == null) return 0;
        int count = 0;
        for (int i = 0; i < 64; i++) {
            Rank next = getNextRank(system, player);
            if (next == null || !next.isAvailable(player)) {
                break;
            }
            if (!attemptRankup(system, player)) {
                break;
            }
            count++;
        }
        return count;
    }

    public boolean hasAutoRankupPermission(Player player) {
        return player.hasPermission("zmrankup.autorankup") || player.hasPermission("zmrankups.autorankup");
    }

    public boolean attemptRankup(Player player) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null && attemptRankup(def, player);
    }

    public boolean attemptRankup(RankupSystem system, Player player) {
        if (system == null) return false;
        Rank next = getNextRank(system, player);
        if (next == null) {
            plugin.getMessageManager().send(player, "rankup-max");
            return false;
        }

        Rank current = getCurrentRank(system, player);
        RankupEvent event = new RankupEvent(player, current, next);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        RankProgress progress = next.getProgress(player);
        if (!progress.isComplete()) {
            RankupFailEvent failEvent = new RankupFailEvent(player, next, progress.getMissingRequirements());
            Bukkit.getPluginManager().callEvent(failEvent);
            plugin.getMessageManager().send(player, "rankup-fail");
            if (plugin.getConfigManager().isSoundsEnabled()) {
                plugin.getMessageManager().playSound(player, "rankup-fail");
            }
            return false;
        }

        for (Requirement req : next.getRequirements()) {
            if (req instanceof VaultBalanceRequirement vbr && vbr.isWithdraw()) {
                if (plugin.getHookManager().isVaultEnabled()) {
                    plugin.getHookManager().getVault().withdraw(player, vbr.getRequired());
                }
            }
        }

        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        data.setCurrentRankId(system.getId(), next.getId());
        data.incrementRankups();
        data.setLastRankupTime(System.currentTimeMillis());
        plugin.getPlayerDataCache().save(player.getUniqueId());
        if (plugin.getPermissionManager() != null) {
            plugin.getPermissionManager().refreshPlayer(player);
        }

        PlaceholderContext context = PlaceholderContext.withPosition(next.getListPosition());

        RewardExecutor.executeAll(player, next.getRewards(), context);
        RewardExecutor.executeCommands(player, next.getSuccessActions(), context);

        RankupSuccessEvent successEvent = new RankupSuccessEvent(player, current, next, next.getRewards());
        Bukkit.getPluginManager().callEvent(successEvent);

        plugin.getMessageManager().send(player, context, "rankup-success", "rank", next.getDisplayName());

        if (plugin.getConfigManager().isTitlesEnabled()) {
            ConfigurationSection titleSection = plugin.getConfigManager().getConfig().getConfigurationSection("titles.rankup-success");
            if (titleSection != null) {
                String title = plugin.getMessageManager().replacePlaceholders(player, context, titleSection.getString("title", ""), "rank", next.getDisplayName());
                String subtitle = plugin.getMessageManager().replacePlaceholders(player, context, titleSection.getString("subtitle", ""), "rank", next.getDisplayName());
                int fadeIn = titleSection.getInt("fade-in", 10);
                int stay = titleSection.getInt("stay", 40);
                int fadeOut = titleSection.getInt("fade-out", 10);
                plugin.getMessageManager().sendTitle(player, context, title, subtitle, fadeIn, stay, fadeOut);
            }
        }

        if (plugin.getConfigManager().isSoundsEnabled()) {
            plugin.getMessageManager().playSound(player, "rankup-success");
        }

        return true;
    }

    public boolean forceRankup(Player player, Rank rank) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        return def != null && forceRankup(def, player, rank);
    }

    public boolean forceRankup(RankupSystem system, Player player, Rank rank) {
        if (system == null || rank == null) return false;
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        Rank oldRank = getCurrentRank(system, player);
        data.setCurrentRankId(system.getId(), rank.getId());
        data.incrementRankups();
        data.setLastRankupTime(System.currentTimeMillis());
        plugin.getPlayerDataCache().save(player.getUniqueId());
        if (plugin.getPermissionManager() != null) {
            plugin.getPermissionManager().refreshPlayer(player);
        }
        
        PlaceholderContext context = PlaceholderContext.withPosition(rank.getListPosition());
        RewardExecutor.executeAll(player, rank.getRewards(), context);
        RewardExecutor.executeCommands(player, rank.getSuccessActions(), context);
        
        RankupSuccessEvent event = new RankupSuccessEvent(player, oldRank, rank, rank.getRewards());
        Bukkit.getPluginManager().callEvent(event);
        return true;
    }
}
