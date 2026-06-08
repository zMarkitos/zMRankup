package dev.zm.rankup.menu;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.rank.RankManager;
import dev.zm.rankup.rank.TemplateManager;
import dev.zm.rankup.config.PlaceholderContext;
import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.reward.Reward;
import dev.zm.rankup.util.ColorUtil;
import dev.zm.rankup.util.ItemBuilder;
import dev.zm.rankup.util.NumberFormatter;
import dev.zm.rankup.util.ProgressBar;
import dev.zm.rankup.zMRankup;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.zm.rankup.system.RankupSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager {

    private final zMRankup plugin;
    private final Map<String, StaticMenuItemData> staticItems = new HashMap<>();

    public MenuManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void loadMenuConfig() {
        // Now loaded per-system dynamically or globally if needed.
        // Static items are now parsed directly when opening the menu to allow per-system customization.
    }

    private Map<String, StaticMenuItemData> getSystemStaticItems(RankupSystem system) {
        Map<String, StaticMenuItemData> items = new HashMap<>();
        ConfigurationSection navSection = system.getConfig().getConfigurationSection("items.navigation");
        if (navSection == null) {
            // Fallback to global config if system doesn't define it
            ConfigurationSection menuSection = plugin.getConfigManager().getMenuSection();
            if (menuSection != null) navSection = menuSection.getConfigurationSection("navigation");
        }

        if (navSection != null) {
            for (String key : navSection.getKeys(false)) {
                ConfigurationSection itemConf = navSection.getConfigurationSection(key);
                if (itemConf == null || !itemConf.getBoolean("enabled", true)) continue;

                String material = itemConf.getString("material", "STONE");

                items.put(key.toLowerCase(), new StaticMenuItemData(
                        material,
                        itemConf.getString("display_name", " "),
                        itemConf.getStringList("lore"),
                        itemConf.getBoolean("glow", false),
                        itemConf.getStringList("actions")
                ));
            }
        }
        return items;
    }

    private List<Integer> parseSlots(List<String> slotsRaw) {
        List<Integer> result = new ArrayList<>();
        if (slotsRaw == null) return result;
        for (String s : slotsRaw) {
            if (s.contains("-")) {
                String[] split = s.split("-");
                try {
                    int start = Integer.parseInt(split[0]);
                    int end = Integer.parseInt(split[1]);
                    for (int i = start; i <= end; i++) result.add(i);
                } catch (NumberFormatException ignored) {
                }
            } else {
                try {
                    result.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    public void openMenu(Player player, int page) {
        RankupSystem def = plugin.getSystemManager().getDefaultSystem();
        if (def != null) {
            openMenu(player, def, page);
        }
    }

    public void openMenu(Player player, RankupSystem system, int page) {
        final RankupSystem targetSystem = system;
        final int targetPage = page;
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, targetSystem, targetPage));
            return;
        }

        RankManager rm = plugin.getRankManager();
        TemplateManager tm = plugin.getTemplateManager();

        List<Rank> allRanks = system.getAllRanks();
        int rows = system.getMenuRows();

        int maxPages = system.getMaxPage();
        if (maxPages < 1) maxPages = 1;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        final int currentPage = page;
        final int totalPages = maxPages;

        String title = plugin.getMessageManager().replacePlaceholders(player, system.getMenuTitle(),
                "page", String.valueOf(currentPage),
                "max_page", String.valueOf(totalPages));
        MenuBuilder builder = new MenuBuilder(title, rows);

        ConfigurationSection decoSection = system.getConfig().getConfigurationSection("items.DECORATION");
        if (decoSection == null) {
            ConfigurationSection menuSection = plugin.getConfigManager().getMenuSection();
            if (menuSection != null) decoSection = menuSection.getConfigurationSection("DECORATION");
        }

        if (decoSection != null && decoSection.getBoolean("enabled", false)) {
            String fillerMat = decoSection.getString("material", "GRAY_STAINED_GLASS_PANE");
            String decoName = decoSection.getString("display_name", " ");
            List<Integer> slots = parseSlots(decoSection.getStringList("slots"));
            ItemStack fillerItem = new ItemBuilder(fillerMat, player).name(decoName).build();
            MenuItem filler = new MenuItem(fillerItem);
            for (int slot : slots) {
                builder.setItem(slot, filler);
            }
        }

        Map<String, StaticMenuItemData> sysItems = getSystemStaticItems(system);
        ConfigurationSection navSection = system.getConfig().getConfigurationSection("items.navigation");
        if (navSection == null) {
            ConfigurationSection menuSection = plugin.getConfigManager().getMenuSection();
            if (menuSection != null) navSection = menuSection.getConfigurationSection("navigation");
        }

        if (navSection != null) {
                for (String key : navSection.getKeys(false)) {
                    int slot = navSection.getInt(key + ".slot", -1);
                    StaticMenuItemData itemData = sysItems.get(key.toLowerCase());
                    if (slot == -1 || itemData == null) continue;

                    ItemStack item = buildStaticItem(player, itemData, currentPage, maxPages);

                    List<String> actions = itemData.actions();
                    if ("previous-page".equalsIgnoreCase(key)) {
                        builder.setItem(slot, new MenuItem(item, e -> {
                            MenuActionExecutor.execute(plugin, player, actions);
                            if (currentPage > 1) {
                                openMenu(player, system, currentPage - 1);
                            }
                        }));
                    } else if ("next-page".equalsIgnoreCase(key)) {
                        builder.setItem(slot, new MenuItem(item, e -> {
                            MenuActionExecutor.execute(plugin, player, actions);
                            if (currentPage < totalPages) {
                                openMenu(player, system, currentPage + 1);
                            }
                        }));
                    } else if ("close".equalsIgnoreCase(key)) {
                        builder.setItem(slot, new MenuItem(item, e -> {
                            MenuActionExecutor.execute(plugin, player, actions);
                            if (actions == null || actions.stream().noneMatch(a -> a != null && a.toLowerCase().contains("[close]"))) {
                                player.closeInventory();
                            }
                        }));
                    } else {
                        builder.setItem(slot, new MenuItem(item, e -> MenuActionExecutor.execute(plugin, player, actions)));
                    }
                }
            }

        Rank currentRank = rm.getCurrentRank(system, player);
        int currentRankIndex = currentRank != null ? system.getRankIndex(currentRank.getId()) : -1;

        for (Rank rank : allRanks) {
            if (rank.getPage() != currentPage) continue;

            int slot = rank.getSlot();
            if (slot < 0) continue;

            int rankIndex = system.getRankIndex(rank.getId());
            String templateKey = "locked";
            if (rankIndex <= currentRankIndex) {
                templateKey = "current";
            } else if (rankIndex == currentRankIndex + 1) {
                templateKey = "available";
            }

            if (rank.getTemplateOverride() != null) {
                templateKey = rank.getTemplateOverride();
            }

            // Get template from system, fallback to global
            TemplateManager.TemplateData template = system.getTemplate(templateKey);
            if (template == null) {
                template = tm.getTemplate(templateKey);
            }
            if (template == null) continue;

            RankProgressData progressData = buildRankProgressData(player, rank, templateKey, currentRankIndex);
            PlaceholderContext context = PlaceholderContext.withPosition(rank.getListPosition());

            String name = plugin.getMessageManager().replacePlaceholders(player, context, template.getName(),
                    "rank_display", rank.getDisplayName(),
                    "progress", progressData.progressText,
                    "progress_bar", progressData.progressBar,
                    "page", String.valueOf(currentPage),
                    "max_page", String.valueOf(totalPages));

            List<String> lore = new ArrayList<>();
            for (String line : template.getLore()) {
                if (line.contains("{requirements}")) {
                    lore.addAll(buildRequirementsLore(player, system, rank, context));
                    continue;
                }

                if (line.contains("{rewards}")) {
                    lore.addAll(buildRewardsLore(player, rank, context));
                    continue;
                }

                String parsed = plugin.getMessageManager().replacePlaceholders(player, context, line,
                        "rank_display", rank.getDisplayName(),
                        "progress", progressData.progressText,
                        "progress_bar", progressData.progressBar,
                        "page", String.valueOf(currentPage),
                        "max_page", String.valueOf(totalPages));
                lore.add(parsed);
            }

            String mat = rank.getMaterial() != null && !"DEFAULT".equalsIgnoreCase(rank.getMaterial())
                    ? rank.getMaterial() : template.getMaterial();
            ItemStack item = new ItemBuilder(mat, player)
                    .amount(rank.getAmount())
                    .name(name)
                    .lore(lore)
                    .glow(template.isGlow())
                    .build();

            MenuItem rankItem;
            if ("available".equals(templateKey)) {
                rankItem = new MenuItem(item, e -> {
                    plugin.getRankManager().attemptRankup(system, player);
                    openMenu(player, system, currentPage);
                });
            } else {
                rankItem = new MenuItem(item);
            }

            builder.setItem(slot, rankItem);
        }

        player.openInventory(builder.getInventory());
    }

    private ItemStack buildStaticItem(Player player, StaticMenuItemData data, int page, int maxPages) {
        String name = plugin.getMessageManager().replacePlaceholders(player, data.displayName,
                "page", String.valueOf(page),
                "max_page", String.valueOf(maxPages));

        List<String> lore = new ArrayList<>();
        for (String line : data.lore) {
            lore.add(plugin.getMessageManager().replacePlaceholders(player, line,
                    "page", String.valueOf(page),
                    "max_page", String.valueOf(maxPages)));
        }

        return new ItemBuilder(data.material, player)
                .name(name)
                .lore(lore)
                .glow(data.glow)
                .build();
    }

    private List<String> buildRewardsLore(Player player, Rank rank, PlaceholderContext context) {
        List<String> lore = new ArrayList<>();
        if (rank.getRewards().isEmpty()) {
            lore.add("  <gray>Ninguna");
            return lore;
        }

        for (Reward reward : rank.getRewards()) {
            for (String line : reward.getDescription()) {
                lore.add("  " + plugin.getMessageManager().replacePlaceholders(player, context, line));
            }
        }
        return lore;
    }

    private List<String> buildRequirementsLore(Player player, RankupSystem system, Rank rank, PlaceholderContext context) {
        List<String> lore = new ArrayList<>();
        ConfigurationSection reqLoreSection = plugin.getConfigManager().getRequirementLoreSection();

        String emptyText = reqLoreSection != null
                ? reqLoreSection.getString("empty", "<gray>Ninguno")
                : "<gray>Ninguno";
        String format = reqLoreSection != null
                ? reqLoreSection.getString("format", "  {status} {display} <gray>({progress})")
                : "  {status} {display} <gray>({progress})";
        String colorSystem = system != null && system.getColorSystem() != null ? system.getColorSystem() : "";
        String statusMet = reqLoreSection != null
                ? reqLoreSection.getString("status-met", "<green>✔")
                : "<green>✔";
        String statusUnmet = reqLoreSection != null
                ? reqLoreSection.getString("status-unmet", "<red>✘")
                : "<red>✘";

        if (rank.getRequirementsMap().isEmpty()) {
            lore.add(plugin.getMessageManager().replacePlaceholders(player, context, emptyText));
            return lore;
        }

        for (Requirement req : rank.getRequirementsMap().values()) {
            boolean met = req.check(player);
            String symbol = met ? statusMet : statusUnmet;
            String progress = met ? "" : req.getProgressDisplay(player);

            String parsed = format
                    .replace("{color_system}", colorSystem)
                    .replace("{status}", symbol)
                    .replace("{display}", req.getDisplay());

            if (progress == null || progress.isEmpty()) {
                parsed = parsed.replace(" &7({progress})", "")
                               .replace(" <gray>({progress})", "")
                               .replace(" ({progress})", "")
                               .replace("({progress})", "");
            } else {
                parsed = parsed.replace("{progress}", progress);
            }

            lore.add(plugin.getMessageManager().replacePlaceholders(player, context, parsed));
        }
        return lore;
    }

    private RankProgressData buildRankProgressData(Player player, Rank rank, String templateKey, int currentRankIndex) {
        double progress = rank.getProgress(player).getOverallProgress();
        int length = plugin.getConfigManager().getProgressBarLength();
        String progressText = NumberFormatter.formatPercentage(progress * 100);
        String progressBar = ProgressBar.createDefault(
                progress,
                length,
                plugin.getConfigManager().getProgressBarFilledChar(),
                plugin.getConfigManager().getProgressBarEmptyChar(),
                plugin.getConfigManager().getProgressBarFilledColor(),
                plugin.getConfigManager().getProgressBarEmptyColor()
        );

        if ("current".equals(templateKey) && currentRankIndex >= rank.getOrder()) {
            progressText = "100";
        }

        return new RankProgressData(progressText, progressBar);
    }

    private record RankProgressData(String progressText, String progressBar) {
    }

    private record StaticMenuItemData(String material, String displayName, List<String> lore, boolean glow, List<String> actions) {
    }
}
