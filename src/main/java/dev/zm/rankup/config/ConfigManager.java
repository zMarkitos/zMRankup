package dev.zm.rankup.config;

import dev.zm.rankup.zMRankup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final zMRankup plugin;
    private final YmlMigrationManager migrationManager; // NEW
    private FileConfiguration config;
    private FileConfiguration langConfig;
    private File langFile;

    public ConfigManager(zMRankup plugin) {
        this.plugin = plugin;
        this.migrationManager = new YmlMigrationManager(plugin); // NEW
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        if (migrationManager.migrateConfigIfNeeded()) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }

        mergeConfigDefaults();

        loadLanguageFile();
        copyBundledResourceIfMissing("prestiges.yml");
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        if (migrationManager.migrateConfigIfNeeded()) {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }

        mergeConfigDefaults();
        loadLanguageFile();
        copyBundledResourceIfMissing("prestiges.yml");
    }

    private void mergeConfigDefaults() {
        try (java.io.InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null)
                return;
            org.bukkit.configuration.file.FileConfiguration defaultConfig = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(
                            new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
            if (mergeMissingKeys(defaultConfig, this.config)) {
                plugin.saveConfig();
                plugin.reloadConfig();
                this.config = plugin.getConfig();
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to merge config defaults: " + e.getMessage());
        }
    }

    private void loadLanguageFile() {
        migrationManager.migrateLangIfNeeded("EN");
        migrationManager.migrateLangIfNeeded("ES");

        ensureBundledLanguageFiles();

        String language = getLanguage();
        String resourcePath = "langs/Lang_" + language + ".yml";

        FileConfiguration defaultLang = loadDefaultLanguage(resourcePath);

        plugin.getDataFolder().mkdirs();
        this.langFile = new File(plugin.getDataFolder(), resourcePath.replace("/", File.separator));
        if (!langFile.exists()) {
            if (defaultLang != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                String fallback = "EN".equals(language) ? "ES" : "EN";
                String fallbackResource = "langs/Lang_" + fallback + ".yml";
                if (plugin.getResource(fallbackResource) != null) {
                    plugin.saveResource(fallbackResource, false);
                    this.langFile = new File(plugin.getDataFolder(), fallbackResource.replace("/", File.separator));
                }
            }
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
        if (defaultLang != null && mergeMissingKeys(defaultLang, this.langConfig) && langFile.exists()) {
            try {
                this.langConfig.save(langFile);
            } catch (IOException e) {
                plugin.getLogger()
                        .warning("Failed to update language file " + langFile.getName() + ": " + e.getMessage());
            }
        }
    }

    private void ensureBundledLanguageFiles() {
        copyBundledResourceIfMissing("langs/Lang_EN.yml");
        copyBundledResourceIfMissing("langs/Lang_ES.yml");
    }

    private void copyBundledResourceIfMissing(String resourcePath) {
        File resourceFile = new File(plugin.getDataFolder(), resourcePath.replace("/", File.separator));
        if (resourceFile.exists() || plugin.getResource(resourcePath) == null) {
            return;
        }
        plugin.saveResource(resourcePath, false);
    }

    private FileConfiguration loadDefaultLanguage(String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled language file " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private boolean mergeMissingKeys(ConfigurationSection source, ConfigurationSection target) {
        boolean changed = false;
        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);
            if (sourceValue instanceof ConfigurationSection sourceSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    targetSection = target.createSection(key);
                    changed = true;
                }
                changed |= mergeMissingKeys(sourceSection, targetSection);
                continue;
            }

            if (!target.contains(key)) {
                target.set(key, sourceValue);
                changed = true;
            }
        }
        return changed;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public boolean isAutoRankupEnabled() {
        return config.getBoolean("auto-rankup.enabled", false);
    }

    public int getAutoRankupInterval() {
        return config.getInt("auto-rankup.interval", 2);
    }

    public boolean isTitlesEnabled() {
        return config.getBoolean("titles.enabled", true);
    }

    public boolean isSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }

    public String getSound(String key) {
        return config.getString("sounds." + key);
    }

    public String getPrefix() {
        return config.getString("settings.prefix", "<gray>[<light_purple>zMRankup<gray>] ");
    }

    public String getLanguage() {
        String language = config.getString("settings.language", "ES");
        return "EN".equalsIgnoreCase(language) ? "EN" : "ES";
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    public ConfigurationSection getMenuSection() {
        return config.getConfigurationSection("menu");
    }

    public ConfigurationSection getTemplatesSection() {
        return config.getConfigurationSection("templates");
    }

    public ConfigurationSection getRequirementLoreSection() {
        return langConfig != null ? langConfig.getConfigurationSection("requirements.default-lore") : null;
    }

    public List<Integer> getRankSlots() {
        List<Integer> list = config.getIntegerList("menu.rank-slots");
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list;
    }

    public int getMenuRows() {
        return config.getInt("menu.rows", 6);
    }

    public String getMenuTitle() {
        return config.getString("menu.title", "<dark_gray>✦ Menu de RankUps ✦");
    }

    public int getProgressBarLength() {
        return config.getInt("progress-bar.length", 20);
    }

    public String getProgressBarFilledChar() {
        return config.getString("progress-bar.filled-char", "■");
    }

    public String getProgressBarEmptyChar() {
        return config.getString("progress-bar.empty-char", "□");
    }

    public String getProgressBarFilledColor() {
        return config.getString("progress-bar.filled-color", "<green>");
    }

    public String getProgressBarEmptyColor() {
        return config.getString("progress-bar.empty-color", "<gray>");
    }

    public String getLangMessage(String key) {
        if (langConfig == null) {
            return "<red>Missing language file: " + key;
        }

        String message = langConfig.getString(key);
        if (message == null) {
            return "<red>Missing message: " + key;
        }
        return message;
    }
}