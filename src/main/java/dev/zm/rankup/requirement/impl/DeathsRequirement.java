package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.FormatUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class DeathsRequirement extends Requirement {

    private final long amount;

    public DeathsRequirement(ConfigurationSection config) {
        super("deaths", config.getString("display", config.getString("display_name")));
        this.amount = config.getLong("amount", 0);
    }

    @Override
    public boolean check(Player player) {
        return getProgress(player) >= amount;
    }

    @Override
    public double getProgress(Player player) {
        return player.getStatistic(Statistic.DEATHS);
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    protected String getDefaultDisplay() {
        return lang("requirements.display.deaths", "amount", FormatUtil.formatShort(amount));
    }
}
