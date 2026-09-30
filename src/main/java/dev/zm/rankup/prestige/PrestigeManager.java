package dev.zm.rankup.prestige;

import dev.zm.rankup.config.PlaceholderContext;
import dev.zm.rankup.menu.MenuBuilder;
import dev.zm.rankup.menu.MenuItem;
import dev.zm.rankup.menu.MenuManager;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.util.ItemBuilder;
import dev.zm.rankup.zMRankup;
import dev.zm.rankup.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.zm.rankup.menu.MenuBuilder;
import dev.zm.rankup.menu.MenuItem;
import dev.zm.rankup.menu.MenuActionExecutor;
import dev.zm.rankup.menu.MenuManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;

public class PrestigeManager {

    private static final List<Integer> CONTENT_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43);

    private final zMRankup plugin;
    private org.bukkit.configuration.file.FileConfiguration prestigeConfig;

    public PrestigeManager(zMRankup plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "prestiges.yml");
        this.prestigeConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public List<RankupSystem> getPrestigeSystems() {
        return plugin.getSystemManager().getAllSystems().stream()
                .filter(RankupSystem::isPrestigeEnabled)
                .sorted(Comparator.comparing(RankupSystem::getId, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public RankupSystem getTargetSystem(RankupSystem prestigeSystem) {
        if (prestigeSystem == null)
            return null;
        String targetId = prestigeSystem.getTargetSystemId();
        if (targetId == null || targetId.isBlank())
            return null;
        return plugin.getSystemManager().getSystem(targetId);
    }

    public boolean canPrestige(Player player, RankupSystem system) {
        if (player == null || system == null || !system.isPrestigeEnabled())
            return false;

        int nextPrestigeLevel = getPrestigeLevel(player, system) + 1;
        Rank prestigeRank = system.getRank(String.valueOf(nextPrestigeLevel));

        if (prestigeRank == null) {
            // No rank defined for this level — check if player is at the last rank in the
            // target system
            RankupSystem targetSystem = getTargetSystem(system);
            if (targetSystem == null)
                return false;
            List<Rank> targetRanks = targetSystem.getAllRanks();
            if (targetRanks.isEmpty())
                return false;
            Rank currentInTarget = plugin.getRankManager().getCurrentRank(targetSystem, player);
            Rank lastInTarget = targetRanks.get(targetRanks.size() - 1);
            return currentInTarget != null && currentInTarget.getId().equals(lastInTarget.getId());
        }

        for (Requirement req : prestigeRank.getRequirementsMap().values()) {
            if (!req.check(player))
                return false;
        }
        return true;
    }

    public int getPrestigeLevel(Player player, RankupSystem system) {
        if (player == null || system == null)
            return 0;
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        return data.getPrestigeLevel(system.getId());
    }

    public String formatPrestige(RankupSystem system, int level) {
        if (system == null)
            return level > 0 ? "P" + level : ColorUtil.translateLegacy("&c✖");
            
        if (level <= 0) {
            String def = system.getDefaultRank();
            return def != null && !def.isEmpty() ? ColorUtil.translateLegacy(def) : ColorUtil.translateLegacy("&c✖");
        }
        
        String formatted = system.getPrestigeFormat();
        if (formatted == null || formatted.isBlank())
            formatted = "P{level}";
        return ColorUtil.translateLegacy(formatted.replace("{level}", String.valueOf(level)));
    }

    public boolean prestige(Player player, RankupSystem system) {
        if (!canPrestige(player, system))
            return false;

        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        int newLevel = getPrestigeLevel(player, system) + 1;
        data.incrementPrestige(system.getId());
        data.setLastRankupTime(System.currentTimeMillis());

        // Reset rank only in the TARGET system (not in the prestige system itself)
        RankupSystem targetSystem = getTargetSystem(system);
        if (targetSystem != null && system.isPrestigeResetRank()) {
            String startRankId = system.getPrestigeStartRankId();
            Rank startRank = startRankId != null ? targetSystem.getRank(startRankId) : null;
            data.setCurrentRankId(targetSystem.getId(), startRank != null ? startRank.getId() : null);
        }
        // Removed the else-if branch that was setting currentRankId on the prestige
        // system itself,
        // which corrupted getCurrentRank() and caused the menu to show the wrong
        // prestige as available.

        plugin.getPlayerDataCache().save(player.getUniqueId());
        if (plugin.getPermissionManager() != null) {
            plugin.getPermissionManager().refreshPlayer(player);
        }

        Rank prestigeRank = system.getRank(String.valueOf(newLevel));
        if (prestigeRank != null) {
            dev.zm.rankup.config.PlaceholderContext ctx = dev.zm.rankup.config.PlaceholderContext
                    .withPosition(newLevel);
            dev.zm.rankup.reward.RewardExecutor.executeAll(player, prestigeRank.getRewards(), ctx);
            dev.zm.rankup.reward.RewardExecutor.executeCommands(player, prestigeRank.getSuccessActions(), ctx);
        }

        plugin.getMessageManager().send(player, "prestige-success",
                "level", String.valueOf(newLevel),
                "prestige", formatPrestige(system, newLevel));

        if (plugin.getConfigManager().isTitlesEnabled()) {
            org.bukkit.configuration.ConfigurationSection titleSection = plugin.getConfigManager().getConfig()
                    .getConfigurationSection("titles.prestige-success");
            if (titleSection != null) {
                dev.zm.rankup.config.PlaceholderContext ctx = dev.zm.rankup.config.PlaceholderContext
                        .withPosition(newLevel);
                String title = plugin.getMessageManager().replacePlaceholders(player, ctx,
                        titleSection.getString("title", ""), "prestige", formatPrestige(system, newLevel));
                String subtitle = plugin.getMessageManager().replacePlaceholders(player, ctx,
                        titleSection.getString("subtitle", ""), "prestige", formatPrestige(system, newLevel));
                int fadeIn = titleSection.getInt("fade-in", 10);
                int stay = titleSection.getInt("stay", 40);
                int fadeOut = titleSection.getInt("fade-out", 10);
                plugin.getMessageManager().sendTitle(player, ctx, title, subtitle, fadeIn, stay, fadeOut);
            }
        }

        if (plugin.getConfigManager().isSoundsEnabled()) {
            plugin.getMessageManager().playSound(player, "prestige-success");
        }

        if (plugin.getConfigManager().getConfig().getBoolean("prestige.close-menu-on-success", true)) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        }

        return true;
    }

    // Returns which prestige rank should be shown as "available" for the player.
    // This is used by MenuManager to determine the correct template
    // (locked/available/current).
    public int getNextPrestigeLevel(Player player, RankupSystem system) {
        return getPrestigeLevel(player, system) + 1;
    }

    public void openMenu(Player player) {
        if (player == null)
            return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
            return;
        }

        // prestiges.yml is a standalone menu config, not a RankupSystem.
        // Build and open it directly from prestigeConfig.
        if (prestigeConfig == null) {
            player.sendMessage(dev.zm.rankup.util.ColorUtil.parse("<red>Prestige menu not configured."));
            return;
        }

        String titleRaw = prestigeConfig.getString("menu.title", "Prestiges");
        int rows = prestigeConfig.getInt("menu.rows", 3);
        String title = plugin.getMessageManager().replacePlaceholders(player, titleRaw);

        MenuBuilder builder = new MenuBuilder(title, rows);

        // Decoration
        ConfigurationSection deco = prestigeConfig.getConfigurationSection("items.decoration");
        if (deco != null && deco.getBoolean("enabled", true)) {
            String mat = deco.getString("material", "GRAY_STAINED_GLASS_PANE");
            String name = deco.getString("display_name", " ");
            List<Integer> slots = MenuManager.parseSlots(deco.getStringList("slots"));
            ItemStack fillerItem = new dev.zm.rankup.util.ItemBuilder(mat, player).name(name).build();
            MenuItem filler = new MenuItem(fillerItem);
            for (int slot : slots)
                builder.setItem(slot, filler);
        }

        // All other items
        ConfigurationSection itemsSection = prestigeConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equalsIgnoreCase("decoration"))
                    continue;
                ConfigurationSection itemConf = itemsSection.getConfigurationSection(key);
                if (itemConf == null || !itemConf.getBoolean("enabled", true))
                    continue;

                int slot = itemConf.getInt("slot", -1);
                if (slot < 0)
                    continue;

                String mat = itemConf.getString("material", "STONE");
                String rawName = itemConf.getString("display_name", " ");
                List<String> rawLore = itemConf.getStringList("lore");
                boolean glow = itemConf.getBoolean("glow", false);
                List<String> actions = itemConf.getStringList("actions");

                String name = plugin.getMessageManager().replacePlaceholders(player, rawName);
                List<String> lore = new ArrayList<>();
                for (String line : rawLore) {
                    lore.add(plugin.getMessageManager().replacePlaceholders(player, line));
                }

                ItemStack item = new dev.zm.rankup.util.ItemBuilder(mat, player)
                        .name(name).lore(lore).glow(glow).build();

                builder.setItem(slot, new MenuItem(item, e -> MenuActionExecutor.execute(plugin, player, actions)));
            }
        }

        player.openInventory(builder.getInventory());
    }
}