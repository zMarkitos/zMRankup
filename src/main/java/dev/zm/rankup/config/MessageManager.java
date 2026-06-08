package dev.zm.rankup.config;

import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.zMRankup;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class MessageManager {

    private final zMRankup plugin;

    public MessageManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String messageKey, String... replacements) {
        send(sender, PlaceholderContext.empty(), messageKey, replacements);
    }

    public void send(CommandSender sender, PlaceholderContext context, String messageKey, String... replacements) {
        if (sender == null) return;
        String message = plugin.getConfigManager().getLangMessage(messageKey);
        if (message == null || message.isBlank()) return;

        String fullMessage = replacePlaceholders(sender instanceof Player player ? player : null, context, message, replacements);
        sender.sendMessage(ColorUtil.parse(fullMessage));
    }

    public void sendRaw(CommandSender sender, String text, String... replacements) {
        sendRaw(sender, PlaceholderContext.empty(), text, replacements);
    }

    public void sendRaw(CommandSender sender, PlaceholderContext context, String text, String... replacements) {
        if (sender == null || text == null || text.isBlank()) return;
        String fullMessage = replacePlaceholders(sender instanceof Player player ? player : null, context, text, replacements);
        sender.sendMessage(ColorUtil.parse(fullMessage));
    }

    public void sendTitle(Player player, String titleText, String subtitleText, int fadeIn, int stay, int fadeOut) {
        sendTitle(player, PlaceholderContext.empty(), titleText, subtitleText, fadeIn, stay, fadeOut);
    }

    public void sendTitle(Player player, PlaceholderContext context, String titleText, String subtitleText, int fadeIn, int stay, int fadeOut) {
        Component title = ColorUtil.parse(replacePlaceholders(player, context, titleText));
        Component subtitle = ColorUtil.parse(replacePlaceholders(player, context, subtitleText));
        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
        );
        net.kyori.adventure.title.Title adventureTitle = net.kyori.adventure.title.Title.title(title, subtitle, times);
        player.showTitle(adventureTitle);
    }

    public void playSound(Player player, String soundKey) {
        String soundName = plugin.getConfigManager().getSound(soundKey);
        if (soundName == null || soundName.isEmpty()) return;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Invalid sound name: " + soundName);
            }
        }
    }

    public void sendActionBar(Player player, String text) {
        sendActionBar(player, PlaceholderContext.empty(), text);
    }

    public void sendActionBar(Player player, PlaceholderContext context, String text) {
        if (text == null || text.isBlank()) return;
        String parsed = replacePlaceholders(player, context, text);
        player.sendActionBar(ColorUtil.parse(parsed));
    }

    public String replacePlaceholders(Player player, String text, String... replacements) {
        return replacePlaceholders(player, PlaceholderContext.empty(), text, replacements);
    }

    public String replacePlaceholders(Player player, PlaceholderContext context, String text, String... replacements) {
        if (text == null) return null;

        for (int i = 0; i < replacements.length - 1; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            if (key != null && value != null) {
                text = text.replace("{" + key + "}", value);
            }
        }

        text = text.replace("{prefix}", plugin.getConfigManager().getPrefix());

        if (context != null) {
            for (var entry : context.values().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    text = text.replace("{" + key + "}", value);
                }
            }
        }

        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
