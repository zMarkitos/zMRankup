package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.FormatUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;

public class MobKillsRequirement extends Requirement {

    private final long amount;
    private final String mobType;

    public MobKillsRequirement(ConfigurationSection config) {
        super("mob_kills", config.getString("display", config.getString("display_name")));
        this.amount = config.getLong("amount", 0);
        this.mobType = config.getString("mob", null);
    }

    @Override
    public boolean check(Player player) {
        return getProgress(player) >= amount;
    }

    @Override
    public double getProgress(Player player) {
        if (mobType != null) {
            EntityType entityType = parseEntityType(mobType);
            if (entityType == null) {
                return 0.0;
            }
            return player.getStatistic(Statistic.KILL_ENTITY, entityType);
        }
        return player.getStatistic(Statistic.MOB_KILLS);
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    protected String getDefaultDisplay() {
        String mobName = mobType != null ? translateNamedValue("mobs", mobType) : "";
        return lang("requirements.display.mob_kills",
                "amount", FormatUtil.formatShort(amount),
                "mob_suffix", mobType != null ? " (" + mobName + ")" : lang("requirements.display.generic_mobs", " mobs"));
    }

    private EntityType parseEntityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceIndex + 1);
        }

        try {
            return EntityType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
