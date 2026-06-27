package dev.zm.rankup.menu;

import dev.zm.rankup.config.MessageManager;
import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MenuActionExecutor {

    private static final Pattern ACTION_PATTERN = Pattern.compile("^\\[(?<type>[^\\]]+)](?:\\s*(?<value>.*))?$");

    private MenuActionExecutor() {
    }

    public static void execute(zMRankup plugin, Player player, List<String> actions) {
        if (player == null)
            return;
        Runnable task = () -> runActions(plugin, player, actions);
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private static void runActions(zMRankup plugin, Player player, List<String> actions) {
        if (actions == null || actions.isEmpty())
            return;

        // Phase 1 – immediate actions (sound, message, title): run right away.
        // Phase 2 – deferred actions (player_command, console_command, open-system,
        // close):
        // close the current inventory first, then execute on the next tick so Bukkit
        // can finish closing before a new inventory is opened.

        List<String> deferredActions = new ArrayList<>();

        for (String action : actions) {
            if (action == null || action.isBlank())
                continue;

            Matcher matcher = ACTION_PATTERN.matcher(action.trim());
            if (!matcher.matches())
                continue;

            String type = matcher.group("type").toLowerCase(Locale.ROOT).trim();
            String value = matcher.group("value") == null ? "" : matcher.group("value").trim();

            switch (type) {
                case "message" -> sendMessage(plugin, player, value);
                case "sound" -> playSound(plugin, player, value);
                case "title" -> sendTitle(plugin, player, value);
                case "close", "console_command", "player_command", "open-system" -> deferredActions.add(action);
                default -> {
                    /* unknown action type – ignore */ }
            }
        }

        if (!deferredActions.isEmpty()) {
            // Close the current inventory so that commands/menus that open a new one
            // are not immediately overwritten by the caller's own closeInventory() call.
            player.closeInventory();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String action : deferredActions) {
                    Matcher matcher = ACTION_PATTERN.matcher(action.trim());
                    if (!matcher.matches())
                        continue;
                    String type = matcher.group("type").toLowerCase(Locale.ROOT).trim();
                    String value = matcher.group("value") == null ? "" : matcher.group("value").trim();

                    switch (type) {
                        case "close" -> player.closeInventory();
                        case "console_command" -> dispatchCommand(plugin, value, player, false);
                        case "player_command" -> dispatchCommand(plugin, value, player, true);
                        case "open-system" -> {
                            dev.zm.rankup.system.RankupSystem sys = plugin.getSystemManager().getSystem(value);
                            if (sys != null) {
                                plugin.getMenuManager().openMenu(player, sys, 1);
                            }
                        }
                        default -> {
                            /* sound/message/title already handled in phase 1 */ }
                    }
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void dispatchCommand(zMRankup plugin, String command, Player player, boolean asPlayer) {
        if (command == null || command.isBlank())
            return;

        String parsed = plugin.getMessageManager().replacePlaceholders(player, command,
                "player", player.getName(),
                "player_name", player.getName(),
                "player_uuid", player.getUniqueId().toString());

        if (asPlayer) {
            player.performCommand(stripSlash(parsed));
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(parsed));
        }
    }

    private static void sendMessage(zMRankup plugin, Player player, String message) {
        if (message == null || message.isBlank())
            return;
        MessageManager mm = plugin.getMessageManager();
        String parsed = mm.replacePlaceholders(player, message,
                "player", player.getName(),
                "player_name", player.getName(),
                "player_uuid", player.getUniqueId().toString());
        player.sendMessage(ColorUtil.parse(parsed));
    }

    private static void sendTitle(zMRankup plugin, Player player, String value) {
        // Format: "[title] Title text;Subtitle text"
        String[] parts = value.split(";", 2);
        String titleText = parts[0];
        String subtitleText = parts.length > 1 ? parts[1] : "";
        // MessageManager.sendTitle handles PAPI + Adventure parsing correctly.
        plugin.getMessageManager().sendTitle(player, titleText, subtitleText, 10, 70, 20);
    }

    private static void playSound(zMRankup plugin, Player player, String soundName) {
        if (soundName == null || soundName.isBlank())
            return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Invalid menu action sound: " + soundName);
            }
        }
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
