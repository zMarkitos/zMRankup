package dev.zm.rankup.util;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.config.PlaceholderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class VersionChecker {

    private static final String API_URL = "https://api.spigotmc.org/legacy/update.php?resource=%s";
    private static final String RESOURCE_ID = "585858889";
    private static final String SPIGOT_PAGE_URL = "https://www.spigotmc.org/resources/zmrankup.%s/";
    private static final String MODRINTH_PAGE_URL = "https://modrinth.com/plugin/zmrankup";
    private static final String NOTIFICATION_PERMISSION = "zmrankup.admin";
    private static final Pattern NUMERIC_PARTS = Pattern.compile("\\d+");

    private final zMRankup plugin;
    private final HttpClient httpClient;
    private final Set<UUID> notifiedPlayers = ConcurrentHashMap.newKeySet();

    private volatile CompletableFuture<UpdateResult> updateFuture = CompletableFuture
            .completedFuture(UpdateResult.disabled());

    private volatile UpdateResult cachedResult = UpdateResult.disabled();

    public VersionChecker(zMRankup plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void refresh() {
        notifiedPlayers.clear();

        if (!plugin.getConfigManager().getConfig().getBoolean("check-updates", true)) {
            this.cachedResult = UpdateResult.disabled();
            this.updateFuture = CompletableFuture.completedFuture(this.cachedResult);
            return;
        }

        String currentVersion = plugin.getDescription().getVersion();
        CompletableFuture<UpdateResult> future = checkAsync(currentVersion);
        this.updateFuture = future;

        future.thenAccept(result -> {
            if (this.updateFuture == future) {
                this.cachedResult = result;
                if (result.updateAvailable()) {
                    Bukkit.getScheduler().runTask(plugin, () -> notifyOnlinePlayers(result));
                }
            }
        });
    }

    public void notifyPlayer(Player player) {
        if (!plugin.getConfigManager().getConfig().getBoolean("check-updates", true))
            return;
        if (!canReceiveNotifications(player))
            return;

        UpdateResult result = cachedResult;

        if (result.updateAvailable()) {
            sendNotification(player, result);
            return;
        }

        CompletableFuture<UpdateResult> future = updateFuture;
        if (future == null)
            return;

        future.thenAccept(updateResult -> {
            if (this.updateFuture != future)
                return;
            if (!updateResult.updateAvailable())
                return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (this.updateFuture != future)
                    return;
                if (player.isOnline() && canReceiveNotifications(player)) {
                    sendNotification(player, updateResult);
                }
            });
        });
    }

    private void notifyOnlinePlayers(UpdateResult result) {
        if (!result.updateAvailable() || !plugin.getConfigManager().getConfig().getBoolean("check-updates", true))
            return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canReceiveNotifications(player)) {
                sendNotification(player, result);
            }
        }
    }

    private CompletableFuture<UpdateResult> checkAsync(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(API_URL, RESOURCE_ID)))
                        .header("User-Agent", "zMRankup/" + currentVersion)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Spigot update check failed: HTTP " + response.statusCode());
                    return UpdateResult.disabled();
                }

                String latestVersion = response.body().trim();

                if (latestVersion.isEmpty() || !isNewerVersion(latestVersion, currentVersion)) {
                    return UpdateResult.disabled();
                }

                return new UpdateResult(
                        true,
                        currentVersion,
                        latestVersion,
                        String.format(SPIGOT_PAGE_URL, RESOURCE_ID),
                        MODRINTH_PAGE_URL);

            } catch (Exception e) {
                plugin.getLogger().warning("Spigot update check failed: " + e.getMessage());
            }

            return UpdateResult.disabled();
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        return compareVersions(latest, current) > 0;
    }

    private int compareVersions(String left, String right) {
        List<Integer> leftParts = parseVersionParts(left);
        List<Integer> rightParts = parseVersionParts(right);
        int size = Math.max(leftParts.size(), rightParts.size());

        for (int i = 0; i < size; i++) {
            int l = i < leftParts.size() ? leftParts.get(i) : 0;
            int r = i < rightParts.size() ? rightParts.get(i) : 0;

            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private List<Integer> parseVersionParts(String version) {
        List<Integer> parts = new ArrayList<>();

        if (version == null || version.isBlank())
            return parts;

        Matcher matcher = NUMERIC_PARTS.matcher(version);
        while (matcher.find()) {
            String part = matcher.group();
            try {
                parts.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                parts.add(0);
            }
        }
        return parts;
    }

    private void sendNotification(Player player, UpdateResult result) {
        if (!notifiedPlayers.add(player.getUniqueId()))
            return;

        String[] replacements = new String[]{
                "current_version", result.currentVersion(),
                "latest_version", result.latestVersion(),
                "version_url", result.spigotUrl(),
                "spigot_url", result.spigotUrl(),
                "modrinth_url", result.modrinthUrl()
        };

        String title = plugin.getConfigManager().getLangMessage("update-title");
        String subtitle = plugin.getConfigManager().getLangMessage("update-subtitle");

        if (title != null && subtitle != null) {
            String parsedTitle = plugin.getMessageManager().replacePlaceholders(player, title, replacements);
            String parsedSubtitle = plugin.getMessageManager().replacePlaceholders(player, subtitle, replacements);
            plugin.getMessageManager().sendTitle(player, PlaceholderContext.empty(), parsedTitle, parsedSubtitle, 20, 80, 20);
        }

        plugin.getMessageManager().send(player, "update-available", replacements);

        // Build clickable link message programmatically to avoid MiniMessage URL parsing issues
        String linkLabel = plugin.getConfigManager().getLangMessage("update-links");
        String parsedLabel = plugin.getMessageManager().replacePlaceholders(player, linkLabel, replacements);
        Component linkMessage = ColorUtil.parse(parsedLabel)
                .clickEvent(ClickEvent.openUrl(result.spigotUrl()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Click to open", NamedTextColor.GRAY)));
        player.sendMessage(linkMessage);
        
        // Log to console
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("\n<#4FF808><b>[zMRankup] UPDATE AVAILABLE</b>"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<green>A new version of zMRankup is available! <gray>(" + result.currentVersion() + " -> " + result.latestVersion() + ")"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<yellow>Download here: <aqua>" + result.spigotUrl() + "\n"));
    }

    private boolean canReceiveNotifications(Player player) {
        return player.isOp() || player.hasPermission(NOTIFICATION_PERMISSION);
    }

    public record UpdateResult(
            boolean updateAvailable,
            String currentVersion,
            String latestVersion,
            String spigotUrl,
            String modrinthUrl) {
        public static UpdateResult disabled() {
            return new UpdateResult(false, "", "", "", "");
        }
    }
}
