package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderRequirement extends Requirement {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile("^\\s*(.+?)\\s*(>=|<=|==|!=|>|<)\\s*(.+?)\\s*$");

    private final String expression;

    public PlaceholderRequirement(ConfigurationSection config) {
        super("placeholder", config);
        this.expression = config.getString("expression", "");
    }

    @Override
    public boolean check(Player player) {
        Comparison comparison = resolve(player);
        if (comparison == null) return false;

        if (comparison.numericLeft != null && comparison.numericRight != null) {
            double left = comparison.numericLeft;
            double right = comparison.numericRight;
            return switch (comparison.operator) {
                case ">=" -> left >= right;
                case "<=" -> left <= right;
                case "==" -> left == right;
                case "!=" -> left != right;
                case ">" -> left > right;
                case "<" -> left < right;
                default -> false;
            };
        }

        String left = comparison.left;
        String right = comparison.right;
        return switch (comparison.operator) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            case "equals_ignore_case" -> left.equalsIgnoreCase(right);
            default -> false;
        };
    }

    @Override
    public double getProgress(Player player) {
        Comparison comparison = resolve(player);
        if (comparison == null || comparison.numericLeft == null) return check(player) ? 1.0 : 0.0;
        return comparison.numericLeft;
    }

    @Override
    public double getRequired() {
        Comparison comparison = resolve(null);
        if (comparison == null || comparison.numericRight == null) return 1.0;
        return comparison.numericRight;
    }

    @Override
    public String getProgressDisplay(Player player) {
        Comparison comparison = resolve(player);
        if (comparison == null || comparison.numericLeft == null || comparison.numericRight == null) {
            return "";
        }
        return dev.zm.rankup.util.NumberFormatter.formatShort(comparison.numericLeft) + "/" + dev.zm.rankup.util.NumberFormatter.formatShort(comparison.numericRight);
    }

    @Override
    protected String getDefaultDisplay() {
        // Extract friendly name from placeholder and required value
        String friendlyName = extractFriendlyName();
        Comparison comparison = resolve(null);
        if (comparison != null && comparison.numericRight != null) {
            return friendlyName + ": " + NumberFormatter.formatShort(comparison.numericRight);
        }
        if (comparison != null && comparison.right != null) {
            return friendlyName + ": " + comparison.right;
        }
        return friendlyName;
    }

    /**
     * Extracts a user-friendly name from the placeholder in the expression.
     * For example: "%playerpoints_points% >= 60" -> "Playerpoints Points"
     *              "%vault_eco_balance% >= 100" -> "Vault Eco Balance"
     * Falls back to configName if no placeholder is found.
     */
    private String extractFriendlyName() {
        if (expression == null || expression.isBlank()) return configName;
        Matcher phMatcher = Pattern.compile("%([^%]+)%").matcher(expression);
        if (phMatcher.find()) {
            String raw = phMatcher.group(1); // e.g. "playerpoints_points"
            return Requirement.formatConfigName(raw);
        }
        return configName;
    }

    private Comparison resolve(Player player) {
        if (expression == null || expression.isBlank()) return null;

        String parsed = expression;
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            parsed = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, parsed);
        }

        Matcher matcher = COMPARISON_PATTERN.matcher(parsed);
        if (!matcher.matches()) return null;

        String left = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        String right = matcher.group(3).trim();

        Double numericLeft = parseDouble(left);
        Double numericRight = parseDouble(right);
        return new Comparison(left, operator, right, numericLeft, numericRight);
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record Comparison(String left, String operator, String right, Double numericLeft, Double numericRight) {
    }
}
