package dev.zm.rankup.placeholder;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlaceholderListManager {

    private static final String RESOURCE_PATH = "placeholder-lists.yml";

    private final zMRankup plugin;
    private volatile Map<String, PlaceholderList> listsByKey = Collections.emptyMap();
    private volatile String globalDefault = "";

    public PlaceholderListManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getDataFolder().mkdirs();

        File file = new File(plugin.getDataFolder(), RESOURCE_PATH);
        if (!file.exists() && plugin.getResource(RESOURCE_PATH) != null) {
            plugin.saveResource(RESOURCE_PATH, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defaults = loadBundledDefaults();
        boolean changed = false;
        if (defaults != null) {
            changed = mergeMissingKeys(defaults, config);
            if (changed) {
                try {
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to update placeholder list file: " + e.getMessage());
                }
            }
        }

        ConfigurationSection listsSection = config.getConfigurationSection("lists");
        int listCount = listsSection != null ? listsSection.getKeys(false).size() : 0;

        this.globalDefault = config.getString("default", "");
        this.listsByKey = Collections.unmodifiableMap(loadLists(config));

        plugin.getLogger().info("Loaded " + listCount + " placeholder lists.");
    }

    public void reload() {
        load();
    }

    public String resolve(String listName, int position) {
        PlaceholderList list = getList(listName);
        if (list == null) {
            return globalDefault;
        }
        return list.resolve(position, globalDefault);
    }

    public String resolvePlaceholder(String params) {
        if (params == null || params.isBlank()) {
            return globalDefault;
        }

        int separator = params.lastIndexOf('_');
        if (separator <= 0 || separator >= params.length() - 1) {
            return globalDefault;
        }

        String listName = params.substring(0, separator);
        String positionText = params.substring(separator + 1);
        try {
            int position = Integer.parseInt(positionText);
            return resolve(listName, position);
        } catch (NumberFormatException ignored) {
            return globalDefault;
        }
    }

    public PlaceholderList getList(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return listsByKey.get(normalize(name));
    }

    private Map<String, PlaceholderList> loadLists(FileConfiguration config) {
        Map<String, PlaceholderList> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("lists");
        if (section == null) {
            return loaded;
        }

        for (String sourceKey : section.getKeys(false)) {
            ConfigurationSection listSection = section.getConfigurationSection(sourceKey);
            if (listSection == null) {
                continue;
            }

            String id = listSection.getString("id", sourceKey);
            String defaultValue = listSection.getString("default", globalDefault);
            List<String> values = listSection.getStringList("values");
            PlaceholderList list = new PlaceholderList(sourceKey, id, defaultValue, values);

            registerAlias(loaded, sourceKey, list);
            registerAlias(loaded, id, list);
        }

        return loaded;
    }

    private void registerAlias(Map<String, PlaceholderList> loaded, String alias, PlaceholderList list) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        loaded.put(normalize(alias), list);
    }

    private FileConfiguration loadBundledDefaults() {
        try (InputStream stream = plugin.getResource(RESOURCE_PATH)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled placeholder lists: " + e.getMessage());
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

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    public record PlaceholderList(String sourceKey, String id, String defaultValue, List<String> values) {
        public PlaceholderList {
            values = values == null ? List.of() : List.copyOf(values);
        }

        public String resolve(int position, String globalDefault) {
            if (position < 1 || position > values.size()) {
                return defaultOr(globalDefault);
            }

            String value = values.get(position - 1);
            if (value == null || value.isBlank()) {
                return defaultOr(globalDefault);
            }
            return value;
        }

        private String defaultOr(String globalDefault) {
            if (defaultValue != null && !defaultValue.isBlank()) {
                return defaultValue;
            }
            return globalDefault == null ? "" : globalDefault;
        }
    }
}
