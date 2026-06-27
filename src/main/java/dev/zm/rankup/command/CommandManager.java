package dev.zm.rankup.command;

import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CommandManager {

    private final zMRankup plugin;

    private final List<Command> dynamicCommands = new ArrayList<>();

    private java.lang.reflect.Field knownCommandsField;
    private java.lang.reflect.Method knownCommandsMethod;

    public CommandManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (plugin.getCommand("zmrankup") != null) {
            ZMRankupsCommand cmd = new ZMRankupsCommand(plugin);
            plugin.getCommand("zmrankup").setExecutor(cmd);
            plugin.getCommand("zmrankup").setTabCompleter(cmd);
        }

        // Old static prestige command is removed in favor of the dynamic one.

        reloadDynamicCommands();
    }

    public void reloadDynamicCommands() {
        try {
            CommandMap commandMap = Bukkit.getCommandMap();
            Map<String, Command> knownCommands = getKnownCommands(commandMap);

            // Unregister old commands from knownCommands map
            for (Command cmd : dynamicCommands) {
                cmd.unregister(commandMap);
                if (knownCommands != null) {
                    // Remove the primary name (both plain and prefixed)
                    knownCommands.remove(cmd.getName().toLowerCase());
                    knownCommands.remove(plugin.getName().toLowerCase() + ":" + cmd.getName().toLowerCase());
                    // Remove all aliases
                    for (String alias : cmd.getAliases()) {
                        knownCommands.remove(alias.toLowerCase());
                        knownCommands.remove(plugin.getName().toLowerCase() + ":" + alias.toLowerCase());
                    }
                }
            }
            dynamicCommands.clear();

            // Register new system commands
            for (RankupSystem system : plugin.getSystemManager().getAllSystems()) {
                if (!system.isRegisterCommand())
                    continue;
                List<String> cmds = system.getOpenCommands();
                if (cmds.isEmpty())
                    continue;

                String primary = cmds.get(0);
                List<String> aliases = cmds.size() > 1 ? cmds.subList(1, cmds.size()) : new ArrayList<>();

                SystemCommand executor = new SystemCommand(plugin, system.getId());

                Command dynamicCommand = new Command(primary, "Open " + system.getId() + " menu", "/" + primary,
                        aliases) {
                    @Override
                    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
                            @NotNull String[] args) {
                        return executor.onCommand(sender, this, commandLabel, args);
                    }

                    @NotNull
                    @Override
                    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                            @NotNull String[] args) throws IllegalArgumentException {
                        List<String> completions = executor.onTabComplete(sender, this, alias, args);
                        return completions != null ? completions : new ArrayList<>();
                    }
                };

                commandMap.register(plugin.getName(), dynamicCommand);
                dynamicCommands.add(dynamicCommand);
            }

            // Register Master Prestige Command
            List<String> prestigeCmds = plugin.getConfigManager().getConfig()
                    .getStringList("settings.prestige-commands");
            if (prestigeCmds != null && !prestigeCmds.isEmpty()) {
                String primary = prestigeCmds.get(0);
                List<String> aliases = prestigeCmds.size() > 1 ? prestigeCmds.subList(1, prestigeCmds.size())
                        : new ArrayList<>();
                Command dynamicPrestigeCommand = new Command(primary, "Open master prestige menu", "/" + primary,
                        aliases) {
                    @Override
                    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
                            @NotNull String[] args) {
                        if (!(sender instanceof Player player)) {
                            plugin.getMessageManager().send(sender, "console-only");
                            return true;
                        }
                        if (!player.hasPermission("zmrankup.use")) {
                            plugin.getMessageManager().send(player, "no-permission");
                            return true;
                        }
                        plugin.getPrestigeManager().openMenu(player);
                        return true;
                    }

                    @NotNull
                    @Override
                    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                            @NotNull String[] args) throws IllegalArgumentException {
                        return new ArrayList<>();
                    }
                };
                commandMap.register(plugin.getName(), dynamicPrestigeCommand);
                dynamicCommands.add(dynamicPrestigeCommand);
            }

            // Sync commands for all online players so they see the changes in tab
            // completion
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.updateCommands();
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register dynamic commands: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            if (knownCommandsField == null && knownCommandsMethod == null) {
                try {
                    knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    knownCommandsMethod = commandMap.getClass().getMethod("getKnownCommands");
                }
            }

            if (knownCommandsField != null) {
                return (Map<String, Command>) knownCommandsField.get(commandMap);
            } else if (knownCommandsMethod != null) {
                return (Map<String, Command>) knownCommandsMethod.invoke(commandMap);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not access knownCommands for command cleanup: " + e.getMessage());
        }
        return null;
    }
}
