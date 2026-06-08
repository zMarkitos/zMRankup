package dev.zm.rankup.command;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.system.RankupSystem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ZMRankupsCommand implements CommandExecutor, TabCompleter {

    private final zMRankup plugin;

    public ZMRankupsCommand(zMRankup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zmrankup.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (args.length < 2) {
                plugin.getMessageManager().send(sender, "admin-usage");
                return true;
            }

            String subCommand = args[1].toLowerCase();

            switch (subCommand) {
                case "reload":
                    plugin.reloadPlugin();
                    plugin.getMessageManager().send(sender, "config-reloaded");
                    return true;

                case "version":
                    plugin.getMessageManager().sendRaw(sender, "<yellow>Checking version...");
                    plugin.getVersionChecker().refresh();
                    if (sender instanceof Player p) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getVersionChecker().notifyPlayer(p), 20L);
                    }
                    return true;

                case "add":
                case "set":
                case "remove":
                case "reset":
                    handleDataModification(sender, subCommand, args);
                    return true;

                default:
                    plugin.getMessageManager().send(sender, "admin-usage");
                    return true;
            }
        }

        plugin.getMessageManager().send(sender, "admin-usage");
        return true;
    }

    private void handleDataModification(CommandSender sender, String action, String[] args) {
        // Expected args: admin <action> <user> <system> [rank]
        if (args.length < 4 && (action.equals("add") || action.equals("set") || action.equals("remove") || action.equals("reset"))) {
            if (args.length < 5 && (action.equals("add") || action.equals("set"))) {
                plugin.getMessageManager().sendRaw(sender, "<red>Usage: /zmrankups admin " + action + " <user> <system> <rank>");
                return;
            } else if (args.length < 4) {
                plugin.getMessageManager().sendRaw(sender, "<red>Usage: /zmrankups admin " + action + " <user> <system>");
                return;
            }
        }

        String targetName = args[2];
        String systemId = args[3];
        RankupSystem system = plugin.getSystemManager().getSystem(systemId);

        if (system == null) {
            plugin.getMessageManager().sendRaw(sender, "<red>System not found: " + systemId);
            return;
        }

        String targetRankId = args.length >= 5 ? args[4] : null;
        Rank targetRank = null;

        if (targetRankId != null) {
            targetRank = system.getRank(targetRankId);
            if (targetRank == null) {
                plugin.getMessageManager().sendRaw(sender, "<red>Rank not found: " + targetRankId);
                return;
            }
        }

        // Must be effectively final for lambda
        final Rank finalTargetRank = targetRank;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            UUID targetUuid = offlinePlayer.getUniqueId();

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player onlinePlayer = offlinePlayer.getPlayer();

                if (onlinePlayer != null) {
                    // Player is online, manipulate cache and fire events safely
                    PlayerData data = plugin.getPlayerDataCache().getOrCreate(targetUuid);
                    applyAction(sender, onlinePlayer.getName(), action, system, finalTargetRank, data, onlinePlayer);
                    plugin.getPlayerDataCache().save(targetUuid);
                } else {
                    // Player is offline, load from DB, manipulate, save
                    plugin.getStorageManager().loadPlayerData(targetUuid).thenAccept(data -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            applyAction(sender, offlinePlayer.getName(), action, system, finalTargetRank, data, null);
                            plugin.getStorageManager().savePlayerData(data);
                        });
                    });
                }
            });
        });
    }

    private void applyAction(CommandSender sender, String playerName, String action, RankupSystem system, Rank targetRank, PlayerData data, @Nullable Player onlinePlayer) {
        String currentRankId = data.getCurrentRankId(system.getId());
        List<Rank> allRanks = system.getAllRanks();

        switch (action) {
            case "add":
            case "set":
                if (targetRank == null) return;
                data.setCurrentRankId(system.getId(), targetRank.getId());
                if (onlinePlayer != null) {
                    plugin.getRankManager().forceRankup(system, onlinePlayer, targetRank);
                } else {
                    data.incrementRankups();
                    data.setLastRankupTime(System.currentTimeMillis());
                }
                plugin.getMessageManager().sendRaw(sender, "<green>Set " + playerName + "'s rank to " + targetRank.getDisplayName() + " in system " + system.getId());
                break;

            case "remove":
                if (currentRankId == null || allRanks.isEmpty()) {
                    plugin.getMessageManager().sendRaw(sender, "<red>" + playerName + " doesn't have a rank to remove in this system.");
                    return;
                }
                Rank current = system.getRank(currentRankId);
                if (current == null) {
                    data.getSystemRanks().remove(system.getId());
                    plugin.getMessageManager().sendRaw(sender, "<green>Removed " + playerName + "'s rank in system " + system.getId());
                    return;
                }
                int idx = allRanks.indexOf(current);
                if (idx <= 0) {
                    // Lowest rank, just remove entirely
                    data.getSystemRanks().remove(system.getId());
                    plugin.getMessageManager().sendRaw(sender, "<green>Removed " + playerName + "'s rank in system " + system.getId() + " (was lowest rank)");
                } else {
                    Rank previous = allRanks.get(idx - 1);
                    data.setCurrentRankId(system.getId(), previous.getId());
                    plugin.getMessageManager().sendRaw(sender, "<green>Downgraded " + playerName + " to rank " + previous.getDisplayName() + " in system " + system.getId());
                }
                data.markClean(); // force dirty later
                data.setCurrentRankId(system.getId(), data.getCurrentRankId(system.getId())); // mark dirty
                break;

            case "reset":
                data.getSystemRanks().remove(system.getId());
                data.setCurrentRankId(system.getId(), null); // mark dirty hack
                data.getSystemRanks().remove(system.getId());
                plugin.getMessageManager().sendRaw(sender, "<green>Reset " + playerName + "'s ranks in system " + system.getId());
                break;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zmrankup.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("help", "admin"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(Arrays.asList("reload", "version", "add", "set", "remove", "reset"), args[1]);
        }

        String subCommand = args[1].toLowerCase();

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (Arrays.asList("add", "set", "remove", "reset").contains(subCommand)) {
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return filter(players, args[2]);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            if (Arrays.asList("add", "set", "remove", "reset").contains(subCommand)) {
                List<String> systems = new ArrayList<>();
                for (RankupSystem sys : plugin.getSystemManager().getAllSystems()) {
                    systems.add(sys.getId());
                }
                return filter(systems, args[3]);
            }
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("admin")) {
            if (Arrays.asList("add", "set").contains(subCommand)) {
                RankupSystem system = plugin.getSystemManager().getSystem(args[3]);
                if (system == null) return List.of();
                List<String> ranks = new ArrayList<>();
                for (Rank rank : system.getAllRanks()) {
                    ranks.add(rank.getId());
                }
                return filter(ranks, args[4]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
