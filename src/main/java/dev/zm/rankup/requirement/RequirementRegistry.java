package dev.zm.rankup.requirement;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.requirement.impl.*;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequirementRegistry {

    @FunctionalInterface
    public interface RequirementFactory {
        Requirement create(ConfigurationSection config);
    }

    private final zMRankup plugin;
    private final Map<String, RequirementFactory> factories = new HashMap<>();

    public RequirementRegistry(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void register(String type, RequirementFactory factory) {
        factories.put(type.toLowerCase(), factory);
    }

    public void registerDefaults() {
        register("vault_balance", VaultBalanceRequirement::new);
        register("playtime_hours", PlaytimeRequirement::new);
        register("playtime_minutes", PlaytimeRequirement::new);
        register("permission", PermissionRequirement::new);
        register("mob_kills", MobKillsRequirement::new);
        register("player_kills", PlayerKillsRequirement::new);
        register("deaths", DeathsRequirement::new);
        register("blocks_mined", BlocksMinedRequirement::new);
        register("placeholder", PlaceholderRequirement::new);
        register("rankup_rank", SystemRankRequirement::new);
    }

    public Requirement create(String type, ConfigurationSection config) {
        if (type == null) return null;
        RequirementFactory factory = factories.get(type.toLowerCase());
        if (factory == null) {
            plugin.getLogger().warning("Unknown requirement type: " + type);
            return null;
        }
        return factory.create(config);
    }

    public boolean hasType(String type) {
        return type != null && factories.containsKey(type.toLowerCase());
    }

    public Set<String> getRegisteredTypes() {
        return factories.keySet();
    }
}
