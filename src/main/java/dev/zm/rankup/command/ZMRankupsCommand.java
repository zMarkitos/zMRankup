package dev.zm.rankup.command;

import dev.zm.rankup.command.sub.GeneralSubCommand;
import dev.zm.rankup.command.sub.PrestigeSubCommand;
import dev.zm.rankup.command.sub.RankSubCommand;
import dev.zm.rankup.command.sub.SubCommand;
import dev.zm.rankup.zMRankup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZMRankupsCommand implements CommandExecutor, TabCompleter {

    /** Maps lower-case sub-command names → their handler. */
    private final Map<String, SubCommand> registry = new HashMap<>();

    /** Sub-commands routed to {@link GeneralSubCommand}. */
    private static final List<String> GENERAL_SUBS = List.of("reload", "version");

    /** Sub-commands routed to {@link RankSubCommand}. */
    private static final List<String> RANK_SUBS = List.of("set", "reset");
    private static final Map<String, Integer> MAX_ARGS = Map.of(
            "reload", 2,
            "version", 2,
            "set", 5,
            "reset", 4);

    private final zMRankup plugin;

    public ZMRankupsCommand(zMRankup plugin) {
        this.plugin = plugin;
        buildRegistry();
    }

    /**
     * Registers all sub-command handlers.
     * To add a new sub-command, instantiate it here and call {@code register}.
     */
    private void buildRegistry() {
        GeneralSubCommand general = new GeneralSubCommand(plugin);
        RankSubCommand rank = new RankSubCommand(plugin);
        PrestigeSubCommand prestige = new PrestigeSubCommand(plugin);

        for (String name : GENERAL_SUBS)
            register(name, general);
        for (String name : RANK_SUBS)
            register(name, rank);
        register("prestige", prestige);
    }

    private void register(String name, SubCommand handler) {
        registry.put(name.toLowerCase(), handler);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (!sender.hasPermission("zmrankup.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        // /zmrankups or /zmrankups help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        // Must start with "admin"
        if (!args[0].equalsIgnoreCase("admin")) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        // /zmrankups admin (no sub-command)
        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        SubCommand handler = registry.get(args[1].toLowerCase());
        if (handler == null) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        Integer maxArgs = MAX_ARGS.get(args[1].toLowerCase());
        if (maxArgs != null && args.length > maxArgs) {
            plugin.getMessageManager().send(sender, "admin-usage");
            return true;
        }

        handler.execute(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {

        if (!sender.hasPermission("zmrankup.admin"))
            return List.of();

        // arg[0]: "help" or "admin"
        if (args.length == 1) {
            return filter(List.of("help", "admin"), args[0]);
        }

        if (!args[0].equalsIgnoreCase("admin"))
            return List.of();

        // arg[1]: sub-command name
        if (args.length == 2) {
            return filter(new ArrayList<>(registry.keySet()), args[1]);
        }

        // Delegate deeper completions to the sub-command handler
        SubCommand handler = registry.get(args[1].toLowerCase());
        if (handler == null)
            return List.of();

        return handler.tabComplete(sender, args);
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String v : values) {
            if (v.toLowerCase().startsWith(lower))
                result.add(v);
        }
        return result;
    }
}