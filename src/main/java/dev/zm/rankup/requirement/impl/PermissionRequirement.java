package dev.zm.rankup.requirement.impl;

import dev.zm.rankup.requirement.Requirement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PermissionRequirement extends Requirement {

    private final String permission;

    public PermissionRequirement(ConfigurationSection config) {
        super("permission", config.getString("display", config.getString("display_name")));
        this.permission = config.getString("permission", "");
    }

    @Override
    public boolean check(Player player) {
        return player.hasPermission(permission);
    }

    @Override
    public double getProgress(Player player) {
        return player.hasPermission(permission) ? 1.0 : 0.0;
    }

    @Override
    public double getRequired() {
        return 1.0;
    }

    @Override
    public String getProgressDisplay(Player player) {
        return "";
    }

    @Override
    protected String getDefaultDisplay() {
        return lang("requirements.display.permission", "permission", permission);
    }
}
