package dev.zm.rankup.requirement;

import dev.zm.rankup.util.NumberFormatter;
import dev.zm.rankup.zMRankup;
import org.bukkit.entity.Player;

import java.util.Locale;

public abstract class Requirement {

    protected final String type;
    protected final String displayName;
    protected final String configName;

    protected Requirement(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
        this.configName = "Requisito";
    }

    protected Requirement(String type, org.bukkit.configuration.ConfigurationSection config) {
        this.type = type;
        this.displayName = config.getString("display", config.getString("display_name"));
        this.configName = formatConfigName(config.getName());
    }

    public static String formatConfigName(String name) {
        if (name == null || name.isEmpty()) return "Requisito";
        String clean = name.replace("_", " ").trim();
        String[] words = clean.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String getType() {
        return type;
    }

    public abstract boolean check(Player player);

    public abstract double getProgress(Player player);

    public abstract double getRequired();

    public double getProgressPercentage(Player player) {
        double req = getRequired();
        if (req <= 0) return 1.0;
        return Math.min(1.0, getProgress(player) / req);
    }

    public String getDisplay() {
        return displayName != null ? displayName : getDefaultDisplay();
    }

    protected abstract String getDefaultDisplay();

    protected String lang(String key, String... replacements) {
        String message = zMRankup.getInstance().getConfigManager().getLangMessage(key);
        if (message == null) {
            return "";
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            if (placeholder != null && value != null) {
                message = message.replace("{" + placeholder + "}", value);
            }
        }
        return message;
    }

    protected String translateNamedValue(String category, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        String normalized = normalizeLookupKey(key);
        var langConfig = zMRankup.getInstance().getConfigManager().getLangConfig();
        if (langConfig != null) {
            String translated = langConfig.getString("translations." + category + "." + normalized);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }

        return formatConfigName(normalized);
    }

    protected String normalizeLookupKey(String key) {
        if (key == null) {
            return "";
        }

        String normalized = key.toLowerCase(Locale.ROOT).trim();
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceIndex + 1);
        }

        return normalized.replace(' ', '_');
    }

    public String getProgressDisplay(Player player) {
        return NumberFormatter.formatShort(getProgress(player)) + "/" + NumberFormatter.formatShort(getRequired());
    }
}
