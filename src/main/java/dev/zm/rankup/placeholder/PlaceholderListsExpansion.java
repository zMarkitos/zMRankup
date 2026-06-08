package dev.zm.rankup.placeholder;

import dev.zm.rankup.zMRankup;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderListsExpansion extends PlaceholderExpansion {

    private final zMRankup plugin;

    public PlaceholderListsExpansion(zMRankup plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zmrankups";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zM";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        return plugin.getPlaceholderListManager().resolvePlaceholder(params);
    }
}
