package dev.zm.rankup.api;

import dev.zm.rankup.requirement.Requirement;
import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface RequirementFactory {
    Requirement create(ConfigurationSection config);
}
