package dev.zm.rankup.permission;

import dev.zm.rankup.rank.Rank;
import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.system.RankupSystem;
import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {

    private final zMRankup plugin;
    private final File file;
    private final Map<String, PermissionGroupDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, ResolvedPermissionGroup> resolvedGroups = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public PermissionManager(zMRankup plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "permissions.yml");
    }

    public void load() {
        ensureFileExists();
        definitions.clear();
        resolvedGroups.clear();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection == null) {
            plugin.getLogger().warning(
                    "[Permissions] No 'groups' section found in permissions.yml. All internal permission groups will be skipped.");
            return;
        }

        for (String groupId : groupsSection.getKeys(false)) {
            ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupId);
            if (groupSection == null) {
                warn("Skipping malformed group '" + groupId + "'.");
                continue;
            }

            String parent = clean(groupSection.getString("parent", null));
            List<String> permissions = new ArrayList<>();
            List<String> rawPermissions = groupSection.getStringList("permissions");
            if (rawPermissions != null) {
                for (String permission : rawPermissions) {
                    String cleaned = clean(permission);
                    if (cleaned != null) {
                        permissions.add(cleaned);
                    } else {
                        warn("Skipping empty permission entry in group '" + groupId + "'.");
                    }
                }
            }

            definitions.put(groupId, new PermissionGroupDefinition(groupId, parent, permissions));
        }

        for (String groupId : definitions.keySet()) {
            ResolvedPermissionGroup resolved = resolveGroup(groupId, new HashSet<>(), new ArrayDeque<>());
            if (resolved != null) {
                resolvedGroups.put(groupId, resolved);
            }
        }

        plugin.getLogger().info("Loaded " + resolvedGroups.size() + " internal permission groups.");
    }

    public void reload() {
        load();
    }

    public void ensureAttachment(Player player) {
        if (player == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> ensureAttachment(player));
            return;
        }

        attachments.computeIfAbsent(player.getUniqueId(), uuid -> player.addAttachment(plugin));
    }

    public void refreshPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshPlayer(player));
            return;
        }

        PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(),
                uuid -> player.addAttachment(plugin));
        clearAttachment(attachment);

        PlayerData data = plugin.getPlayerDataCache().get(player.getUniqueId());
        if (data == null) {
            return;
        }

        Map<String, Boolean> effectivePermissions = resolvePlayerPermissions(data);
        for (Map.Entry<String, Boolean> entry : effectivePermissions.entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }
    }

    public void refreshAllOnlinePlayers() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAllOnlinePlayers);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    public void removePlayer(Player player) {
        if (player == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> removePlayer(player));
            return;
        }

        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
                // Attachment was already removed by Bukkit or another cleanup path.
            }
        }
    }

    public void clearAll() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::clearAll);
            return;
        }

        for (Map.Entry<UUID, PermissionAttachment> entry : new HashMap<>(attachments).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            PermissionAttachment attachment = entry.getValue();
            if (player != null && player.isOnline() && attachment != null) {
                try {
                    player.removeAttachment(attachment);
                } catch (IllegalArgumentException ignored) {
                    // Ignore cleanup races during shutdown/reload.
                }
            }
        }
        attachments.clear();
    }

    public Map<String, Boolean> resolvePlayerPermissions(PlayerData data) {
        if (data == null) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> effective = new LinkedHashMap<>();
        List<RankupSystem> systems = new ArrayList<>(plugin.getSystemManager().getAllSystems());
        systems.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));

        for (RankupSystem system : systems) {
            String rankId = data.getCurrentRankId(system.getId());
            if (rankId == null) {
                continue;
            }

            Rank rank = system.getRank(rankId);
            if (rank == null) {
                continue;
            }

            String groupId = rank.getPermissionGroup();
            if (groupId == null) {
                continue;
            }

            ResolvedPermissionGroup group = resolvedGroups.get(groupId);
            if (group == null) {
                warn("Rank '" + rank.getId() + "' in system '" + system.getId()
                        + "' references missing or invalid permission group '" + groupId + "'.");
                continue;
            }

            effective.putAll(group.permissions());
        }

        return effective;
    }

    public ResolvedPermissionGroup getResolvedGroup(String groupId) {
        return resolvedGroups.get(groupId);
    }

    private void ensureFileExists() {
        if (file.exists()) {
            return;
        }

        if (plugin.getResource("permissions.yml") != null) {
            plugin.saveResource("permissions.yml", false);
            return;
        }

        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            warn("Could not create plugin data folder for permissions.yml.");
            return;
        }

        FileConfiguration empty = new YamlConfiguration();
        empty.createSection("groups");
        try {
            empty.save(file);
        } catch (IOException e) {
            warn("Could not create default permissions.yml: " + e.getMessage());
        }
    }

    private ResolvedPermissionGroup resolveGroup(String groupId, Set<String> visiting, Deque<String> chain) {
        if (resolvedGroups.containsKey(groupId)) {
            return resolvedGroups.get(groupId);
        }

        PermissionGroupDefinition definition = definitions.get(groupId);
        if (definition == null) {
            return null;
        }

        if (!visiting.add(groupId)) {
            warn("Cycle detected in permission groups: " + formatChain(chain, groupId));
            return null;
        }

        chain.addLast(groupId);
        try {
            Map<String, Boolean> permissions = new LinkedHashMap<>();

            if (definition.parent() != null) {
                PermissionGroupDefinition parentDefinition = definitions.get(definition.parent());
                if (parentDefinition == null) {
                    warn("Group '" + groupId + "' has missing parent '" + definition.parent() + "'. Skipping group.");
                    return null;
                }

                ResolvedPermissionGroup parent = resolveGroup(definition.parent(), visiting, chain);
                if (parent == null) {
                    warn("Group '" + groupId + "' could not be loaded because its parent chain is invalid.");
                    return null;
                }

                permissions.putAll(parent.permissions());
            }

            for (String rawPermission : definition.permissions()) {
                String permission = clean(rawPermission);
                if (permission == null) {
                    continue;
                }

                boolean positive = true;
                if (permission.startsWith("-") && permission.length() > 1) {
                    positive = false;
                    permission = clean(permission.substring(1));
                }

                if (permission == null || permission.isBlank()) {
                    warn("Ignoring empty permission entry in group '" + groupId + "'.");
                    continue;
                }

                permissions.put(permission, positive);
            }

            ResolvedPermissionGroup resolved = new ResolvedPermissionGroup(groupId,
                    Collections.unmodifiableMap(permissions));
            resolvedGroups.put(groupId, resolved);
            return resolved;
        } finally {
            chain.removeLastOccurrence(groupId);
            visiting.remove(groupId);
        }
    }

    private void clearAttachment(PermissionAttachment attachment) {
        if (attachment == null) {
            return;
        }

        Set<String> currentPermissions = new HashSet<>(attachment.getPermissions().keySet());
        for (String permission : currentPermissions) {
            attachment.unsetPermission(permission);
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void warn(String message) {
        plugin.getLogger().warning("[Permissions] " + message);
    }

    private String formatChain(Deque<String> chain, String repeated) {
        String prefix = chain.stream().collect(Collectors.joining(" -> "));
        if (prefix.isEmpty()) {
            return repeated + " -> " + repeated;
        }
        return prefix + " -> " + repeated;
    }

    public record PermissionGroupDefinition(String id, String parent, List<String> permissions) {
    }

    public record ResolvedPermissionGroup(String id, Map<String, Boolean> permissions) {
    }
}
