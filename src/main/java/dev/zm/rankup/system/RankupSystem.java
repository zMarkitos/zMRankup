package dev.zm.rankup.system;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.rank.TemplateManager.TemplateData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RankupSystem {

    private final String id;
    private final FileConfiguration config;
    private final LinkedHashMap<String, Rank> ranks = new LinkedHashMap<>();
    private final List<Rank> orderedRanks = new ArrayList<>();
    private final Map<String, TemplateData> templates = new LinkedHashMap<>();
    
    private final String menuTitle;
    private final int menuRows;
    private final String colorSystem;
    private final String targetSystemId;
    private final List<String> openCommands;
    private final boolean registerCommand;
    private final boolean prestigeEnabled;
    private final String prestigeMenuTitle;
    private final String prestigeDisplayName;
    private final String prestigeFormat;
    private final String prestigeIcon;
    private final String prestigeStartRankId;
    private final boolean prestigeResetRank;
    private final String defaultRank;

    public RankupSystem(String id, FileConfiguration config) {
        this.id = id;
        this.config = config;
        
        this.menuTitle = config.getString("menu_title", "&8&n" + id + " Menu ({page}/{max_page})");
        this.menuRows = config.getInt("size", 54) / 9;
        this.colorSystem = config.getString("templates.color-system", "");
        this.defaultRank = config.getString("default_rank", "&c✖");
        
        List<String> cmds = config.getStringList("open_command");
        if (cmds.isEmpty() && config.isString("open_command")) {
            cmds.add(config.getString("open_command"));
        }
        this.openCommands = cmds;
        
        String targetSystemRaw = config.getString("system");
        if (targetSystemRaw != null && targetSystemRaw.endsWith(".yml")) {
            this.targetSystemId = targetSystemRaw.replace(".yml", "");
        } else {
            this.targetSystemId = targetSystemRaw;
        }
        
        this.registerCommand = config.getBoolean("register_command", true);

        ConfigurationSection prestigeSection = config.getConfigurationSection("prestige");
        this.prestigeEnabled = prestigeSection == null || prestigeSection.getBoolean("enabled", true);
        this.prestigeMenuTitle = prestigeSection != null
                ? prestigeSection.getString("menu-title", "&dPrestigios de " + id + " ({page}/{max_page})")
                : "&dPrestigios de " + id + " ({page}/{max_page})";
        this.prestigeDisplayName = prestigeSection != null
                ? prestigeSection.getString("display-name", "&dPrestigio")
                : "&dPrestigio";
        this.prestigeFormat = prestigeSection != null
                ? prestigeSection.getString("format", "P{level}")
                : "P{level}";
        this.prestigeIcon = prestigeSection != null
                ? prestigeSection.getString("material", "NETHER_STAR")
                : "NETHER_STAR";
        this.prestigeStartRankId = prestigeSection != null
                ? normalizePrestigeStartRank(prestigeSection.getString("start-rank", null))
                : null;
        this.prestigeResetRank = prestigeSection == null || prestigeSection.getBoolean("reset-rank", true);
        
        loadTemplates();
    }

    private void loadTemplates() {
        ConfigurationSection section = config.getConfigurationSection("templates");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection tmpl = section.getConfigurationSection(key);
            if (tmpl == null) continue;

            String mat = tmpl.getString("material", "STONE");
            String name = tmpl.getString("display_name", tmpl.getString("name", key));
            List<String> lore = tmpl.getStringList("lore");
            boolean glow = tmpl.getBoolean("glow", false);
            int slot = tmpl.getInt("slot", -1);

            templates.put(key, new TemplateData(mat, name, lore, glow, slot));
        }
    }

    public void addRank(String rankId, Rank rank) {
        ranks.put(rankId, rank);
        orderedRanks.add(rank);
    }

    public String getId() {
        return id;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public int getMenuRows() {
        return menuRows;
    }

    public String getColorSystem() {
        return colorSystem;
    }

    public String getTargetSystemId() {
        return targetSystemId;
    }

    public List<String> getOpenCommands() {
        return openCommands;
    }

    public boolean isRegisterCommand() {
        return registerCommand;
    }

    public boolean isPrestigeEnabled() {
        return prestigeEnabled;
    }

    public String getPrestigeMenuTitle() {
        return prestigeMenuTitle;
    }

    public String getPrestigeDisplayName() {
        return prestigeDisplayName;
    }

    public String getPrestigeFormat() {
        return prestigeFormat;
    }

    public String getPrestigeIcon() {
        return prestigeIcon;
    }

    public String getPrestigeStartRankId() {
        return prestigeStartRankId;
    }

    public boolean isPrestigeResetRank() {
        return prestigeResetRank;
    }

    public String getDefaultRank() {
        return defaultRank;
    }

    public Rank getRank(String rankId) {
        if (rankId == null) return null;
        return ranks.get(rankId);
    }

    public List<Rank> getAllRanks() {
        return Collections.unmodifiableList(orderedRanks);
    }

    public TemplateData getTemplate(String key) {
        return templates.get(key);
    }

    public int getRankIndex(String rankId) {
        Rank r = ranks.get(rankId);
        return r != null ? r.getOrder() : -1;
    }

    public int getMaxPage() {
        int max = 1;
        for (Rank rank : orderedRanks) {
            max = Math.max(max, rank.getPage());
        }
        return max;
    }

    private String normalizePrestigeStartRank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
