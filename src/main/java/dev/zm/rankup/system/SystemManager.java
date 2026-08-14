package dev.zm.rankup.system;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.requirement.Requirement;
import dev.zm.rankup.reward.Reward;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemManager {

    private final zMRankup plugin;
    private final Map<String, RankupSystem> systems = new ConcurrentHashMap<>();
    private RankupSystem defaultSystem;

    public SystemManager(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        systems.clear();
        defaultSystem = null;

        File systemsFolder = new File(plugin.getDataFolder(), "systems");
        boolean firstRun = !systemsFolder.exists();
        if (firstRun) {
            systemsFolder.mkdirs();
        }

        if (firstRun) {
            copyBundledSystem("systems/rankups.yml");
            copyBundledSystem("systems/playtime.yml");
        } else {
            // Also handle the legacy ranks.yml migration if rankups.yml doesn't exist
            File target = new File(systemsFolder, "rankups.yml");
            if (!target.exists()) {
                migrateLegacyRanksIfNeeded(target);
            }
        }
        
        File internalDataFile = new File(plugin.getDataFolder(), "internal-data.yml");
        FileConfiguration internalData = YamlConfiguration.loadConfiguration(internalDataFile);
        if (!internalData.getBoolean("prestige-rankups-generated", false)) {
            File prestigeFile = new File(systemsFolder, "prestige-rankups.yml");
            if (!prestigeFile.exists()) {
                copyBundledSystem("systems/prestige-rankups.yml");
            }
            internalData.set("prestige-rankups-generated", true);
            try {
                internalData.save(internalDataFile);
            } catch (java.io.IOException ignored) {}
        }

        File[] files = systemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                RankupSystem system = new RankupSystem(id, config);
                loadRanksForSystem(system);
                systems.put(id, system);

                if (defaultSystem == null || "rankups".equalsIgnoreCase(id) || "rankup".equalsIgnoreCase(id)) {
                    defaultSystem = system; // Prioritize 'rankups' or 'rankup' as default
                }
            }
        }

        plugin.getLogger().info("Loaded " + systems.size() + " rankup systems.");
    }

    private void copyBundledSystem(String resourcePath) {
        if (plugin.getResource(resourcePath) != null) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void migrateLegacyRanksIfNeeded(File target) {
        File oldRanks = new File(plugin.getDataFolder(), "ranks.yml");
        if (oldRanks.exists() && !target.exists() && "rankups.yml".equalsIgnoreCase(target.getName())) {
            oldRanks.renameTo(target);
        }
    }

    private void loadRanksForSystem(RankupSystem system) {
        ConfigurationSection ranksSection = system.getConfig().getConfigurationSection("ranks");
        if (ranksSection == null) {
            ranksSection = system.getConfig().getConfigurationSection("prestiges");
        }
        if (ranksSection == null)
            return;

        int order = 0;
        for (String id : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(id);
            if (rankSection == null)
                continue;

            String displayName = rankSection.getString("display_rank", rankSection.getString("display_name", id));
            String materialStr = rankSection.contains("material")
                    ? rankSection.getString("material", null)
                    : rankSection.getString("display.material", rankSection.getString("material", null));

            int slot = rankSection.getInt("slot", -1);
            int page = rankSection.getInt("page", 1);
            int amount = rankSection.getInt("amount", 1);
            int listPosition = rankSection.contains("list-position")
                    ? rankSection.getInt("list-position", order + 1)
                    : rankSection.getInt("list_position", order + 1);
            if (listPosition < 1) {
                listPosition = order + 1;
            }
            String permissionGroup = rankSection.getString("permission-group",
                    rankSection.getString("permission_group", null));

            Map<String, Requirement> requirementsMap = new LinkedHashMap<>();
            ConfigurationSection reqSection = rankSection.getConfigurationSection("requirements");
            if (reqSection != null) {
                for (String reqId : reqSection.getKeys(false)) {
                    ConfigurationSection reqConfig = reqSection.getConfigurationSection(reqId);
                    if (reqConfig != null) {
                        String type = reqConfig.getString("type");
                        // For rankup_rank requirements: auto-inject the parent system's targetSystemId
                        // if the user did not specify a 'system' key in the requirement block.
                        if ("rankup_rank".equalsIgnoreCase(type) && !reqConfig.contains("system")) {
                            String targetId = system.getTargetSystemId();
                            if (targetId != null && !targetId.isBlank()) {
                                reqConfig.set("system", targetId);
                            }
                        }
                        Requirement req = plugin.getRequirementRegistry().create(type, reqConfig);
                        if (req != null)
                            requirementsMap.put(reqId, req);
                    }
                }
            }

            boolean useDefaultLoreReqs = rankSection.getBoolean("use-default-lore-requirements",
                    rankSection.getBoolean("use-default-lore-requirimients", true));
            List<String> customReqLore = rankSection.getStringList("lore-requirements");
            if (customReqLore.isEmpty() && rankSection.isList("lore-requiriments")) {
                customReqLore = rankSection.getStringList("lore-requiriments");
            }

            List<Reward> rewards = new ArrayList<>();
            ConfigurationSection rewardSection = rankSection.getConfigurationSection("rewards");
            if (rewardSection != null) {
                rewards = plugin.getRewardRegistry().loadRewards(rewardSection);
            }

            List<String> successActions = rankSection.getStringList("actions.success");

            Rank rank = new Rank(id, displayName, order, slot, page, materialStr, permissionGroup, requirementsMap,
                    rewards, successActions, null, useDefaultLoreReqs, customReqLore, listPosition, amount);
            system.addRank(id, rank);
            order++;
        }
    }

    public RankupSystem getSystem(String id) {
        if (id == null)
            return null;
        return systems.get(id);
    }

    public Collection<RankupSystem> getAllSystems() {
        return systems.values();
    }

    public RankupSystem getDefaultSystem() {
        return defaultSystem;
    }

    public void reload() {
        loadAll();
    }
}
