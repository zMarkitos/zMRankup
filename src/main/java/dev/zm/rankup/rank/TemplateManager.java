package dev.zm.rankup.rank;

import dev.zm.rankup.zMRankup;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateManager {

    private final zMRankup plugin;
    private final Map<String, TemplateData> templates = new HashMap<>();

    public TemplateManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void loadTemplates() {
        templates.clear();
        ConfigurationSection section = plugin.getConfigManager().getTemplatesSection();
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

    public TemplateData getTemplate(String key) {
        return templates.get(key);
    }

    public String getMaterialString(String key) {
        TemplateData data = templates.get(key);
        return data != null ? data.material : "STONE";
    }

    public String getName(String key) {
        TemplateData data = templates.get(key);
        return data != null ? data.name : key;
    }

    public List<String> getLore(String key) {
        TemplateData data = templates.get(key);
        return data != null ? data.lore : new ArrayList<>();
    }

    public boolean isGlow(String key) {
        TemplateData data = templates.get(key);
        return data != null && data.glow;
    }

    public static class TemplateData {
        private final String material;
        private final String name;
        private final List<String> lore;
        private final boolean glow;
        private final int slot;

        public TemplateData(String material, String name, List<String> lore, boolean glow) {
            this(material, name, lore, glow, -1);
        }

        public TemplateData(String material, String name, List<String> lore, boolean glow, int slot) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.glow = glow;
            this.slot = slot;
        }

        public String getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public boolean isGlow() { return glow; }
        public int getSlot() { return slot; }
    }
}
