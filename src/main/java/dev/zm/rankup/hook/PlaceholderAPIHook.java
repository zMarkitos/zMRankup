package dev.zm.rankup.hook;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.rank.RankManager;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.util.FormatUtil;
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
        if (offlinePlayer == null || !offlinePlayer.isOnline())
            return "";
        Player player = offlinePlayer.getPlayer();
        if (player == null)
            return "";

        RankManager rm = plugin.getRankManager();
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());

        String paramLower = params.toLowerCase();

        RankupSystem system = plugin.getSystemManager().getDefaultSystem();
        String metric = paramLower;

        for (RankupSystem sys : plugin.getSystemManager().getAllSystems()) {
            String prefix = sys.getId() + "_";
            if (paramLower.startsWith(prefix)) {
                system = sys;
                metric = paramLower.substring(prefix.length());
                break;
            }
        }

        if (system == null)
            return "";

        boolean isPrestigeMetric = metric.startsWith("prestige") || metric.equals("can_prestige");
        RankupSystem prestigeSystem = system;
        if (isPrestigeMetric && !system.isPrestigeEnabled()) {
            prestigeSystem = plugin.getSystemManager().getAllSystems().stream()
                    .filter(RankupSystem::isPrestigeEnabled)
                    .findFirst()
                    .orElse(system);
        }

        return switch (metric) {
            case "rank" -> {
                Rank current = rm.getCurrentRank(system, player);
                String none = system.getDefaultRank();
                if (none == null || none.isEmpty())
                    none = "&c✖";
                yield current != null ? ColorUtil.toLegacy(current.getDisplayName()) : ColorUtil.toLegacy(none);
            }
            case "rank_id" ->
                data.getCurrentRankId(system.getId()) != null ? data.getCurrentRankId(system.getId()) : "none";
            case "next_rank" -> {
                Rank next = rm.getNextRank(system, player);
                yield next != null ? ColorUtil.toLegacy(next.getDisplayName()) : "Max";
            }
            case "next_rank_id" -> {
                Rank next = rm.getNextRank(system, player);
                yield next != null ? next.getId() : "none";
            }
            case "progress" -> {
                Rank next = rm.getNextRank(system, player);
                if (next == null)
                    yield "100.0";
                yield FormatUtil.formatPercentage(next.getProgress(player).getOverallProgress() * 100);
            }
            case "progress_bar" -> {
                Rank next = rm.getNextRank(system, player);
                double pct = next == null ? 1.0 : next.getProgress(player).getOverallProgress();
                yield ColorUtil.toLegacy(FormatUtil.createDefaultProgressBar(
                        pct,
                        plugin.getConfigManager().getProgressBarLength(),
                        plugin.getConfigManager().getProgressBarFilledChar(),
                        plugin.getConfigManager().getProgressBarEmptyChar(),
                        plugin.getConfigManager().getProgressBarFilledColor(),
                        plugin.getConfigManager().getProgressBarEmptyColor()));
            }
            case "total_rankups" -> String.valueOf(data.getTotalRankups());
            case "total_ranks" -> String.valueOf(system.getAllRanks().size());

            case "prestige" -> plugin.getPrestigeManager() != null
                    ? ColorUtil.toLegacy(plugin.getPrestigeManager().formatPrestige(
                            prestigeSystem, data.getPrestigeLevel(prestigeSystem.getId())))
                    : String.valueOf(data.getPrestigeLevel(prestigeSystem.getId()));

            case "prestige_level" -> String.valueOf(data.getPrestigeLevel(prestigeSystem.getId()));

            case "next_prestige" -> plugin.getPrestigeManager() != null
                    ? ColorUtil.toLegacy(plugin.getPrestigeManager().formatPrestige(prestigeSystem,
                            data.getPrestigeLevel(prestigeSystem.getId()) + 1))
                    : "P" + (data.getPrestigeLevel(prestigeSystem.getId()) + 1);

            case "can_prestige" ->
                plugin.getPrestigeManager() != null
                        && plugin.getPrestigeManager().canPrestige(player, prestigeSystem) ? "yes" : "no";

            case "prestige_max" -> String.valueOf(prestigeSystem.getAllRanks().size());

            case "prestige_next_level" -> {
                int cur = data.getPrestigeLevel(prestigeSystem.getId());
                int max = prestigeSystem.getAllRanks().size();
                yield cur >= max ? "MAX" : String.valueOf(cur + 1);
            }

            case "prestige_progress" -> {
                int cur = data.getPrestigeLevel(prestigeSystem.getId());
                int max = prestigeSystem.getAllRanks().size();
                yield cur + "/" + max;
            }

            default -> null;
        };
    }
}