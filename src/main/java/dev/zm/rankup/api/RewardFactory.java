package dev.zm.rankup.api;

import dev.zm.rankup.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface RewardFactory {
    Reward create(String id, ConfigurationSection config);
}
