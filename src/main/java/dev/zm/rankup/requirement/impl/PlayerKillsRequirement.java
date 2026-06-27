package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.util.FormatUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlayerKillsRequirement extends Requirement {

    private final long amount;

    public PlayerKillsRequirement(ConfigurationSection config) {
        super("player_kills", config.getString("display", config.getString("display_name")));
        this.amount = config.getLong("amount", 0);
    }

    @Override
    public boolean check(Player player) {
        return getProgress(player) >= amount;
    }

    @Override
    public double getProgress(Player player) {
        return player.getStatistic(Statistic.PLAYER_KILLS);
    }

    @Override
    public double getRequired() {
        return amount;
    }

    @Override
    protected String getDefaultDisplay() {
        return lang("requirements.display.player_kills", "amount", FormatUtil.formatShort(amount));
    }
}
