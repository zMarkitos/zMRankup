package dev.zm.rankup.command.sub;

import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.zMRankup;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PrestigeSubCommand implements SubCommand {

    private final zMRankup plugin;
    private final PlayerResolver resolver;

    public PrestigeSubCommand(zMRankup plugin) {
        this.plugin = plugin;
        this.resolver = new PlayerResolver(plugin);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // args: [0]=admin [1]=prestige [2]=action [3]=player [4]=system [5]=amount(opt)
        if (args.length < 3 || !Set.of("set", "reset").contains(args[2].toLowerCase())) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }

        String action = args[2].toLowerCase();

        if (args.length < 4) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }
        if (args.length < 5) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }

        String targetName = args[3];
        String systemId = args[4];

        RankupSystem system = plugin.getSystemManager().getSystem(systemId);
        if (system == null) {
            plugin.getMessageManager().send(sender, "admin-system-not-found", "system", systemId);
            return;
        }

        // Validate it's actually a prestige system
        String targetId = system.getTargetSystemId();
        if (targetId == null || targetId.isBlank()) {
            plugin.getMessageManager().sendRaw(sender,
                    "<red>System <white>" + systemId + " <red>is not a prestige system.");
            return;
        }

        int amount = 0;
        if (action.equals("set")) {
            if (args.length < 6) {
                plugin.getMessageManager().send(sender, "admin-usage");
                return;
            }
            try {
                amount = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendRaw(sender, "<red>Invalid amount: " + args[5]);
                return;
            }
            if (amount < 0) {
                plugin.getMessageManager().sendRaw(sender, "<red>Amount must be 0 or positive.");
                return;
            }
            int maxPrestige = system.getAllRanks().size();
            if (amount > maxPrestige) {
                plugin.getMessageManager().send(sender, "admin-prestige-level-not-found",
                        "level", String.valueOf(amount),
                        "max", String.valueOf(maxPrestige),
                        "system", systemId);
                return;
            }
        }

        int maxAllowed = action.equals("set") ? 7 : 5;
        if (args.length > maxAllowed) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return;
        }

        final int finalAmount = amount;
        final boolean resetRanks = action.equals("set") && args.length == 7 ? Boolean.parseBoolean(args[6])
                : system.isPrestigeResetRank();

        resolver.resolve(sender, targetName, (data, online) -> {
            switch (action) {
                case "set" -> {
                    data.setPrestigeLevel(system.getId(), finalAmount);

                    // NUEVO: resetear el rango del sistema target igual que
                    // hace PrestigeManager.prestige() — sin esto el jugador
                    // conserva el rango que tenía y el prestige queda inconsistente.
                    RankupSystem targetSystem = plugin.getPrestigeManager().getTargetSystem(system);
                    if (targetSystem != null && resetRanks) {
                        String startRankId = system.getPrestigeStartRankId();
                        dev.zm.rankup.rank.Rank startRank = startRankId != null
                                ? targetSystem.getRank(startRankId)
                                : null;
                        data.setCurrentRankId(targetSystem.getId(),
                                startRank != null ? startRank.getId() : null);
                    }
                }
                case "reset" -> {
                    data.resetPrestige(system.getId());

                    // NUEVO: al resetear prestige también limpiamos el rango
                    // del target system para dejar todo desde cero.
                    RankupSystem targetSystem = plugin.getPrestigeManager().getTargetSystem(system);
                    if (targetSystem != null && system.isPrestigeResetRank()) {
                        String startRankId = system.getPrestigeStartRankId();
                        dev.zm.rankup.rank.Rank startRank = startRankId != null
                                ? targetSystem.getRank(startRankId)
                                : null;
                        data.setCurrentRankId(targetSystem.getId(),
                                startRank != null ? startRank.getId() : null);
                    }
                }
            }

            if (online != null) {
                plugin.getPlayerDataCache().save(online.getUniqueId());
                if (plugin.getPermissionManager() != null)
                    plugin.getPermissionManager().refreshPlayer(online);
            }

            String displayName = online != null ? online.getName() : targetName;
            plugin.getMessageManager().send(sender, "admin-prestige-updated",
                    "player", displayName,
                    "system", system.getId());
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 3)
            return filter(List.of("set", "reset"), args[2]);
        if (args.length == 4)
            return filterOnlinePlayers(args[3]);
        if (args.length == 5)
            return filterPrestigeSystems(args[4]);
        if (args.length == 6 && args[2].equalsIgnoreCase("set")) {
            RankupSystem system = plugin.getSystemManager().getSystem(args[4]);
            if (system == null)
                return List.of();

            List<String> prestigeLevels = new ArrayList<>();
            if (system.getTargetSystemId() != null && !system.getTargetSystemId().isBlank()) {
                for (dev.zm.rankup.rank.Rank rank : system.getAllRanks()) {
                    int level = rank.getOrder() + 1;
                    prestigeLevels.add(String.valueOf(level));
                }
            }
            return filter(prestigeLevels, args[5]);
        }
        if (args.length == 7 && args[2].equalsIgnoreCase("set")) {
            return filter(List.of("true", "false"), args[6]);
        }
        return List.of();
    }

    private List<String> filterOnlinePlayers(String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
            if (p.getName().toLowerCase().startsWith(lower))
                result.add(p.getName());
        return result;
    }

    // Only systems with a targetSystemId (prestige systems)
    private List<String> filterPrestigeSystems(String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (RankupSystem sys : plugin.getSystemManager().getAllSystems()) {
            String targetId = sys.getTargetSystemId();
            if (targetId == null || targetId.isBlank())
                continue;
            if (sys.getId().toLowerCase().startsWith(lower))
                result.add(sys.getId());
        }
        return result;
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String v : values)
            if (v.toLowerCase().startsWith(lower))
                result.add(v);
        return result;
    }
}