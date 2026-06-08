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
    private final List<String> openCommands;
    private final boolean registerCommand;

    public RankupSystem(String id, FileConfiguration config) {
        this.id = id;
        this.config = config;
        
        this.menuTitle = config.getString("menu_title", "&8&n" + id + " Menu ({page}/{max_page})");
        this.menuRows = config.getInt("size", 54) / 9;
        this.colorSystem = config.getString("templates.color-system", "");
        
        List<String> cmds = config.getStringList("open_command");
        if (cmds.isEmpty() && config.isString("open_command")) {
            cmds.add(config.getString("open_command"));
        }
        this.openCommands = cmds;
        this.registerCommand = config.getBoolean("register_command", true);
        
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

            templates.put(key, new TemplateData(mat, name, lore, glow));
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

    public List<String> getOpenCommands() {
        return openCommands;
    }

    public boolean isRegisterCommand() {
        return registerCommand;
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
}
