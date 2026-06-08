package dev.zm.rankup.reward;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.api.RewardFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardRegistry {

    private final zMRankup plugin;
    private final Map<String, RewardFactory> factories = new HashMap<>();

    public RewardRegistry(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void register(String type, RewardFactory factory) {
        factories.put(type.toLowerCase(), factory);
    }

    public Reward create(String id, ConfigurationSection config) {
        String display = config.getString("display.name", id);
        if (config.isString("display")) {
            display = config.getString("display");
        }
        
        List<String> description = config.getStringList("description");
        if (description.isEmpty() && config.isList("displays")) {
            description = config.getStringList("displays");
        }
        List<String> commands = config.getStringList("commands");

        return new Reward(id, display, description, commands);
    }

    public List<Reward> loadRewards(ConfigurationSection rewardsSection) {
        List<Reward> rewards = new ArrayList<>();
        if (rewardsSection == null) return rewards;

        if (rewardsSection.isList("displays") || rewardsSection.isList("commands") || rewardsSection.isList("description")) {
            // It's a single reward object directly under "rewards:"
            rewards.add(create("default", rewardsSection));
            return rewards;
        }

        for (String key : rewardsSection.getKeys(false)) {
            ConfigurationSection config = rewardsSection.getConfigurationSection(key);
            if (config != null) {
                rewards.add(create(key, config));
            }
        }
        return rewards;
    }
}
