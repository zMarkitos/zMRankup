package dev.zm.rankup.hook;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.rank.RankManager;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.util.NumberFormatter;
import dev.zm.rankup.util.ProgressBar;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final zMRankup plugin;

    public PlaceholderAPIHook(zMRankup plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zmrankup";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zM";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        RankManager rm = plugin.getRankManager();
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());

        String paramLower = params.toLowerCase();
        
        dev.zm.rankup.system.RankupSystem system = plugin.getSystemManager().getDefaultSystem();
        String metric = paramLower;

        for (dev.zm.rankup.system.RankupSystem sys : plugin.getSystemManager().getAllSystems()) {
            String prefix = sys.getId() + "_";
            if (paramLower.startsWith(prefix)) {
                system = sys;
                metric = paramLower.substring(prefix.length());
                break;
            }
        }

        if (system == null) return "";

        return switch (metric) {
            case "rank" -> {
                Rank current = rm.getCurrentRank(system, player);
                yield current != null ? dev.zm.rankup.util.ColorUtil.toLegacy(current.getDisplayName()) : "Ninguno";
            }
            case "rank_id" -> data.getCurrentRankId(system.getId()) != null ? data.getCurrentRankId(system.getId()) : "none";
            case "next_rank" -> {
                Rank next = rm.getNextRank(system, player);
                yield next != null ? dev.zm.rankup.util.ColorUtil.toLegacy(next.getDisplayName()) : "Máximo";
            }
            case "next_rank_id" -> {
                Rank next = rm.getNextRank(system, player);
                yield next != null ? next.getId() : "none";
            }
            case "progress" -> {
                Rank next = rm.getNextRank(system, player);
                if (next == null) yield "100.0";
                yield NumberFormatter.formatPercentage(next.getProgress(player).getOverallProgress() * 100);
            }
            case "progress_bar" -> {
                Rank next = rm.getNextRank(system, player);
                if (next == null) {
                    yield ColorUtil.toLegacy(ProgressBar.createDefault(
                            1.0,
                            plugin.getConfigManager().getProgressBarLength(),
                            plugin.getConfigManager().getProgressBarFilledChar(),
                            plugin.getConfigManager().getProgressBarEmptyChar(),
                            plugin.getConfigManager().getProgressBarFilledColor(),
                            plugin.getConfigManager().getProgressBarEmptyColor()
                    ));
                }
                yield ColorUtil.toLegacy(ProgressBar.createDefault(
                        next.getProgress(player).getOverallProgress(),
                        plugin.getConfigManager().getProgressBarLength(),
                        plugin.getConfigManager().getProgressBarFilledChar(),
                        plugin.getConfigManager().getProgressBarEmptyChar(),
                        plugin.getConfigManager().getProgressBarFilledColor(),
                        plugin.getConfigManager().getProgressBarEmptyColor()
                ));
            }
            case "total_rankups" -> String.valueOf(data.getTotalRankups()); // Total across all systems?
            case "total_ranks" -> String.valueOf(system.getAllRanks().size());
            default -> null;
        };
    }
}
