package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.FormatUtil;

import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PlaytimeRequirement extends Requirement {

    private final long amount;
    private final boolean isHours;

    public PlaytimeRequirement(ConfigurationSection config) {
        super(config.getString("type", "playtime_hours"), config.getString("display", config.getString("display_name")));
        this.amount = config.getLong("amount", 0);
        this.isHours = "playtime_hours".equalsIgnoreCase(getType());
    }

    @Override
    public boolean check(Player player) {
        return getProgress(player) >= amount;
    }

    @Override
    public double getProgress(Player player) {
        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        if (isHours) {
            return (double) ticks / 20 / 60 / 60;
        } else {
            return (double) ticks / 20 / 60;
        }
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    public String getProgressDisplay(Player player) {
        String unit = isHours ? "h" : "m";
        return FormatUtil.formatShort(getProgress(player)) + unit + "/" + FormatUtil.formatShort(getRequired()) + unit;
    }

    @Override
    protected String getDefaultDisplay() {
        return isHours
                ? lang("requirements.display.playtime_hours", "amount", FormatUtil.formatShort(amount))
                : lang("requirements.display.playtime_minutes", "amount", FormatUtil.formatShort(amount));
    }
}
