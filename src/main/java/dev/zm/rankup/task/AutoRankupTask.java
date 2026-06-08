package dev.zm.rankup.task;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoRankupTask extends BukkitRunnable {

    private final zMRankup plugin;

    public AutoRankupTask(zMRankup plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isAutoRankupEnabled()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getPlayerDataCache().get(player.getUniqueId()) != null) {
                if (!plugin.getRankManager().hasAutoRankupPermission(player)) {
                    continue;
                }

                for (dev.zm.rankup.system.RankupSystem system : plugin.getSystemManager().getAllSystems()) {
                    Rank next = plugin.getRankManager().getNextRank(system, player);
                    if (next != null && next.isAvailable(player)) {
                        plugin.getRankManager().rankupToMax(system, player);
                    }
                }
            }
        }
    }
}
