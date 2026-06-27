package dev.zm.rankup.config;

import dev.zm.rankup.zMRankup;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class YmlMigrationManager {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final Set<String> SKIP_PRESERVE = Set.of(
            "config-version",
            "messages-version");

    private static final Map<String, String> FORCE_VALUES = Map.of(
            "settings.storage", "SQLITE");

    private final zMRankup plugin;
    private final Logger log;

    public YmlMigrationManager(zMRankup plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public boolean migrateConfigIfNeeded() {
        return migrateIfNeeded("config.yml", "config-version");
    }

    public boolean migrateLangIfNeeded(String language) {
        return migrateIfNeeded("langs/Lang_" + language + ".yml", "messages-version");
    }

    private boolean migrateIfNeeded(String resourcePath, String versionKey) {
        File diskFile = new File(plugin.getDataFolder(),
                resourcePath.replace("/", File.separator));

        if (!diskFile.exists()) {
            return false;
        }

        int bundledVersion = readVersionFromJar(resourcePath, versionKey);
        int diskVersion = readVersionFromDisk(diskFile, versionKey);

        log.info("[Migration] " + resourcePath
                + " | disk=" + diskVersion + " bundled=" + bundledVersion);

        if (diskVersion >= bundledVersion) {
            return false;
        }

        log.info("[Migration] Migrating " + resourcePath
                + " from v" + diskVersion + " to v" + bundledVersion + "...");

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(diskFile);

        if (!backup(diskFile, resourcePath, diskVersion)) {
            log.warning("[Migration] Backup failed for " + resourcePath + " — aborting.");
            return false;
        }

        plugin.saveResource(resourcePath, true);

        FileConfiguration freshConfig = YamlConfiguration.loadConfiguration(diskFile);
        int preserved = mergeUserValues(oldConfig, freshConfig);
        int forced = applyForcedValues(freshConfig, resourcePath);

        try {
            freshConfig.save(diskFile);
        } catch (IOException e) {
            log.warning("[Migration] Save failed for " + resourcePath + ": " + e.getMessage());
            return false;
        }

        log.info("[Migration] Done: " + resourcePath + " v" + bundledVersion
                + " (" + preserved + " preserved, " + forced + " forced)");
        return true;
    }

    private int readVersionFromJar(String resourcePath, String versionKey) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                log.warning("[Migration] Resource not found in jar: " + resourcePath);
                return 1;
            }
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return parseVersionFromText(content, versionKey);
        } catch (IOException e) {
            log.warning("[Migration] Failed to read jar resource " + resourcePath + ": " + e.getMessage());
            return 1;
        }
    }

    private int readVersionFromDisk(File file, String versionKey) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return parseVersionFromText(content, versionKey);
        } catch (IOException e) {
            log.warning("[Migration] Failed to read disk file " + file.getName() + ": " + e.getMessage());
            return 1;
        }
    }

    private int parseVersionFromText(String content, String versionKey) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(versionKey + ":")) {
                String value = trimmed.substring((versionKey + ":").length()).trim();
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private boolean backup(File diskFile, String resourcePath, int oldVersion) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        File backupDir = new File(plugin.getDataFolder(),
                "backups" + File.separator + "v" + oldVersion + "_" + timestamp);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            log.warning("[Migration] Could not create backup dir: " + backupDir.getAbsolutePath());
            return false;
        }

        File dest = new File(backupDir, resourcePath.replace("/", File.separator));
        dest.getParentFile().mkdirs();

        try {
            Files.copy(diskFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[Migration] Backup → " + dest.getAbsolutePath());
            return true;
        } catch (IOException e) {
            log.warning("[Migration] Backup copy failed: " + e.getMessage());
            return false;
        }
    }

    private int mergeUserValues(FileConfiguration oldConfig, FileConfiguration freshConfig) {
        int[] count = { 0 };
        mergeSection(oldConfig, freshConfig, null, count);
        return count[0];
    }

    private void mergeSection(ConfigurationSection oldSection,
            ConfigurationSection newSection,
            String parentPath,
            int[] count) {
        for (String key : oldSection.getKeys(false)) {
            String fullKey = parentPath == null ? key : parentPath + "." + key;

            if (SKIP_PRESERVE.contains(key) || SKIP_PRESERVE.contains(fullKey)) {
                continue;
            }

            Object oldValue = oldSection.get(key);

            if (oldValue instanceof ConfigurationSection oldChild) {
                ConfigurationSection newChild = newSection.getConfigurationSection(key);
                if (newChild != null) {
                    mergeSection(oldChild, newChild, fullKey, count);
                }
                continue;
            }

            if (newSection.contains(key)) {
                newSection.set(key, oldValue);
                count[0]++;
            }
        }
    }

    private int applyForcedValues(FileConfiguration config, String resourcePath) {
        if (!"config.yml".equals(resourcePath))
            return 0;
        int count = 0;
        for (var entry : FORCE_VALUES.entrySet()) {
            String key = entry.getKey();
            String forced = entry.getValue();
            if (config.contains(key) && !forced.equals(String.valueOf(config.get(key)))) {
                log.info("[Migration] Force " + key + " -> " + forced);
                config.set(key, forced);
                count++;
            }
        }
        return count;
    }
}