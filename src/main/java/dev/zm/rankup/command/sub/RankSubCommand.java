package dev.zm.rankup.command.sub;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class RankSubCommand implements SubCommand {

    /** Sub-commands that require a target rank argument. */
    private static final Set<String> NEEDS_RANK = Set.of("add", "set");

    private final zMRankup plugin;
    private final PlayerResolver resolver;

    public RankSubCommand(zMRankup plugin) {
        this.plugin = plugin;
        this.resolver = new PlayerResolver(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // args layout: [0]=admin [1]=add|set|remove|reset [2]=player [3]=system
        // [4]=rank (optional)
        String action = args[1].toLowerCase();
        boolean needsRank = NEEDS_RANK.contains(action);

        if (needsRank && args.length < 5) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }
        if (!needsRank && args.length < 4) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }

        String targetName = args[2];
        String systemId = args[3];

        RankupSystem system = plugin.getSystemManager().getSystem(systemId);
        if (system == null) {
            plugin.getMessageManager().sendRaw(sender, "<red>System not found: " + systemId);
            return;
        }

        Rank targetRank = null;
        if (needsRank) {
            String rankId = args[4];
            targetRank = system.getRank(rankId);
            if (targetRank == null) {
                plugin.getMessageManager().send(sender, "admin-rank-not-found", "rank", rankId, "system",
                        system.getId());
                return;
            }
        }

        final Rank finalRank = targetRank;

        resolver.resolve(sender, targetName,
                (data, online) -> applyAction(sender, resolveDisplayName(targetName, online),
                        action, system, finalRank, data, online));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // args[2] → player name
        if (args.length == 3) {
            return filterOnlinePlayers(args[2]);
        }
        // args[3] → system id
        if (args.length == 4) {
            return filterSystems(args[3]);
        }
        // args[4] → rank id (only for add / set)
        if (args.length == 5 && NEEDS_RANK.contains(args[1].toLowerCase())) {
            RankupSystem system = plugin.getSystemManager().getSystem(args[3]);
            if (system == null)
                return List.of();
            return filterRanks(system, args[4]);
        }
        return List.of();
    }

    /**
     * Applies the requested action to {@code data}.
     * Called on the main thread by {@link PlayerResolver}.
     */
    private void applyAction(CommandSender sender, String playerName, String action,
            RankupSystem system, Rank targetRank,
            PlayerData data, Player online) {

        String currentRankId = data.getCurrentRankId(system.getId());
        List<Rank> allRanks = system.getAllRanks();

        switch (action) {
            case "add", "set" -> {
                if (targetRank == null)
                    return;

                if (online != null && action.equals("add")) {
                    // forceRankup handles setCurrentRankId, incrementRankups,
                    // save, rewards and events in one call.
                    plugin.getRankManager().forceRankup(system, online, targetRank);
                    refreshPermissions(online);
                } else {
                    data.setCurrentRankId(system.getId(), targetRank.getId());
                    data.incrementRankups();
                    data.setLastRankupTime(System.currentTimeMillis());
                    if (online != null) {
                        plugin.getPlayerDataCache().save(online.getUniqueId());
                        refreshPermissions(online);
                    }
                }
                plugin.getMessageManager().send(sender, "admin-rank-set", "player", playerName, "rank",
                        targetRank.getDisplayName(), "system", system.getId());
            }

            case "remove" -> {
                if (currentRankId == null || allRanks.isEmpty()) {
                    plugin.getMessageManager().sendRaw(sender,
                            "<red>" + playerName + " doesn't have a rank to remove in this system.");
                    return;
                }
                Rank current = system.getRank(currentRankId);
                if (current == null) {
                    data.setCurrentRankId(system.getId(), null);
                    plugin.getMessageManager().sendRaw(sender,
                            "<green>Removed " + playerName + "'s rank in system " + system.getId());
                    return;
                }
                int idx = allRanks.indexOf(current);
                if (idx <= 0) {
                    data.setCurrentRankId(system.getId(), null);
                    plugin.getMessageManager().sendRaw(sender,
                            "<green>Removed " + playerName + "'s rank in system "
                                    + system.getId() + " (was lowest rank)");
                } else {
                    Rank previous = allRanks.get(idx - 1);
                    data.setCurrentRankId(system.getId(), previous.getId());
                    plugin.getMessageManager().sendRaw(sender,
                            "<green>Downgraded " + playerName + " to rank "
                                    + previous.getDisplayName() + " in system " + system.getId());
                }
                if (online != null)
                    refreshPermissions(online);
            }

            case "reset" -> {
                data.setCurrentRankId(system.getId(), null);
                plugin.getMessageManager().send(sender, "admin-rank-reset", "player", playerName, "system",
                        system.getId());
                if (online != null)
                    refreshPermissions(online);
            }
        }
    }

    private void refreshPermissions(Player player) {
        if (plugin.getPermissionManager() != null) {
            plugin.getPermissionManager().refreshPlayer(player);
        }
    }

    /** Uses the live player name when available; falls back to the typed name. */
    private String resolveDisplayName(String fallback, Player online) {
        return online != null ? online.getName() : fallback;
    }

    private List<String> filterOnlinePlayers(String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(lower))
                result.add(p.getName());
        }
        return result;
    }

    private List<String> filterSystems(String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (RankupSystem sys : plugin.getSystemManager().getAllSystems()) {
            String targetId = sys.getTargetSystemId();
            if (targetId != null && !targetId.isBlank())
                continue;
            if (sys.getId().toLowerCase().startsWith(lower))
                result.add(sys.getId());
        }
        return result;
    }

    private List<String> filterRanks(RankupSystem system, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Rank rank : system.getAllRanks()) {
            if (rank.getId().toLowerCase().startsWith(lower))
                result.add(rank.getId());
        }
        return result;
    }
}