package dev.zm.rankup.hook;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.placeholder.PlaceholderListsExpansion;
import org.bukkit.Bukkit;

public class HookManager {

    private final zMRankup plugin;
    private VaultHook vaultHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private PlaceholderListsExpansion placeholderListsExpansion;

    public HookManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (plugin.getConfigManager().getConfig().getBoolean("integrations.vault.enabled", true) &&
                Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook();
            if (vaultHook.setup(plugin)) {
                plugin.getLogger().info("Vault hooked successfully.");
            } else {
                vaultHook = null;
                plugin.getLogger().warning("Vault found but failed to hook.");
            }
        }

        if (plugin.getConfigManager().getConfig().getBoolean("integrations.placeholderapi.enabled", true) &&
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (placeholderAPIHook == null) {
                placeholderAPIHook = new PlaceholderAPIHook(plugin);
                if (placeholderAPIHook.register()) {
                    plugin.getLogger().info("PlaceholderAPI hooked successfully.");
                }
            }
            if (placeholderListsExpansion == null) {
                placeholderListsExpansion = new PlaceholderListsExpansion(plugin);
                if (placeholderListsExpansion.register()) {
                    plugin.getLogger().info("PlaceholderLists hooked successfully.");
                }
            }
        }
    }

    public VaultHook getVault() {
        return vaultHook;
    }

    public PlaceholderAPIHook getPlaceholderAPI() {
        return placeholderAPIHook;
    }

    public boolean isVaultEnabled() {
        return vaultHook != null && vaultHook.isEnabled();
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIHook != null;
    }
}
