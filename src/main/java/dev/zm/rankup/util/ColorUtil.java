package dev.zm.rankup.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)&x((?:&[0-9a-f]){6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private ColorUtil() {
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(translateLegacy(text));
    }

    public static String translateLegacy(String text) {
        if (text == null) {
            return null;
        }

        // Convert legacy hex (&x&F&F&0&0&0&0) to MiniMessage (<color:#FF0000>)
        Matcher legacyHexMatcher = LEGACY_HEX_PATTERN.matcher(text);
        StringBuilder legacyHexBuilder = new StringBuilder();
        while (legacyHexMatcher.find()) {
            String chunk = legacyHexMatcher.group(1).replace("&", "");
            legacyHexMatcher.appendReplacement(legacyHexBuilder, "<color:#" + chunk + ">");
        }
        legacyHexMatcher.appendTail(legacyHexBuilder);
        text = legacyHexBuilder.toString();

        // Convert Hex (&#RRGGBB) to MiniMessage (<color:#RRGGBB>)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<color:#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        text = sb.toString();

        // Convert standard legacy ampersand codes to MiniMessage tags
        text = text.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        return text;
    }

    public static String stripColor(String text) {
        if (text == null) return null;
        Component component = parse(text);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String toLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        Component component = parse(text);
        return LEGACY_AMPERSAND.serialize(component);
    }

    public static String toLegacy(Component component) {
        if (component == null) return "";
        return LEGACY_AMPERSAND.serialize(component);
    }
}
