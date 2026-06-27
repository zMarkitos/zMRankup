package dev.zm.rankup.command;

import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.zMRankup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SystemCommand implements CommandExecutor, TabCompleter {

    private final zMRankup plugin;
    private final String systemId;

    public SystemCommand(zMRankup plugin, String systemId) {
        this.plugin = plugin;
        this.systemId = systemId;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "console-only");
            return true;
        }

        RankupSystem system = plugin.getSystemManager().getSystem(systemId);
        if (system == null) {
            plugin.getMessageManager().sendRaw(player, "&cError: System '" + systemId + "' is no longer loaded.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("auto")) {
            if (!plugin.getRankManager().hasAutoRankupPermission(player)) {
                plugin.getMessageManager().send(player, "no-permission");
                return true;
            }

            int rankedUp = plugin.getRankManager().rankupToMax(system, player);
            if (rankedUp <= 0) {
                if (plugin.getRankManager().getNextRank(system, player) == null) {
                    boolean canPrestige = plugin.getSystemManager().getAllSystems().stream()
                            .filter(RankupSystem::isPrestigeEnabled)
                            .anyMatch(ps -> {
                                dev.zm.rankup.system.RankupSystem target = plugin.getPrestigeManager()
                                        .getTargetSystem(ps);
                                return target != null && target.getId().equals(system.getId())
                                        && plugin.getPrestigeManager().canPrestige(player, ps);
                            });
                    if (canPrestige) {
                        plugin.getMessageManager().send(player, "rankup-max-prestige-available");
                    } else {
                        plugin.getMessageManager().send(player, "rankup-max");
                    }
                } else {
                    plugin.getMessageManager().send(player, "rankup-fail");
                }
                return true;
            }

            plugin.getMessageManager().send(player, "auto-rankup-complete", "count", String.valueOf(rankedUp));
            return true;
        }

        plugin.getMenuManager().openMenu(player, system, 1);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("zmrankup.use")) {
            return List.of();
        }

        if (args.length == 1) {
            return args[0].isEmpty() ? List.of("auto")
                    : ("auto".startsWith(args[0].toLowerCase()) ? List.of("auto") : List.of());
        }

        return List.of();
    }
}
