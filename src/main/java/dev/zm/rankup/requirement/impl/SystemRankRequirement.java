package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.system.RankupSystem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Requirement that checks if a player has reached a specific rank in a given
 * rankup system.
 *
 * YAML config example:
 * rank:
 * type: rankup_rank
 * value: "10" # The rank ID/name the player must have reached
 * system: rankups # Optional: which system to check (default: "rankups")
 */
public class SystemRankRequirement extends Requirement {

    private final String targetRankId;
    private final String targetSystemId;

    public SystemRankRequirement(ConfigurationSection config) {
        super("rankup_rank", config);
        this.targetRankId = config.getString("value", "");
        this.targetSystemId = config.getString("system", "rankups");
    }

    @Override
    public boolean check(Player player) {
        if (player == null || targetRankId.isEmpty())
            return false;
        zMRankup plugin = zMRankup.getInstance();
        RankupSystem targetSystem = plugin.getSystemManager().getSystem(targetSystemId);
        if (targetSystem == null)
            return false;

        Rank currentRank = plugin.getRankManager().getCurrentRank(targetSystem, player);
        if (currentRank == null)
            return false;

        Rank requiredRank = targetSystem.getRank(targetRankId);
        if (requiredRank == null) {
            // Try to match by list position if the value is a number
            try {
                int pos = Integer.parseInt(targetRankId);
                return currentRank.getOrder() + 1 >= pos;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return currentRank.getOrder() >= requiredRank.getOrder();
    }

    @Override
    public double getProgress(Player player) {
        if (player == null)
            return 0;
        zMRankup plugin = zMRankup.getInstance();
        RankupSystem targetSystem = plugin.getSystemManager().getSystem(targetSystemId);
        if (targetSystem == null)
            return 0;
        Rank currentRank = plugin.getRankManager().getCurrentRank(targetSystem, player);
        return currentRank != null ? currentRank.getOrder() + 1 : 0;
    }

    @Override
    public double getRequired() {
        zMRankup plugin = zMRankup.getInstance();
        RankupSystem targetSystem = plugin.getSystemManager().getSystem(targetSystemId);
        if (targetSystem == null)
            return 0;
        Rank requiredRank = targetSystem.getRank(targetRankId);
        if (requiredRank != null)
            return requiredRank.getOrder() + 1;
        try {
            return Integer.parseInt(targetRankId);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected String getDefaultDisplay() {
        return lang("requirements.display.rankup_rank",
                "value", targetRankId,
                "system", targetSystemId);
    }
}
