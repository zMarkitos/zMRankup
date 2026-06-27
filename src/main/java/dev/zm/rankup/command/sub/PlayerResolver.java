package dev.zm.rankup.command.sub;

import dev.zm.rankup.storage.PlayerData;
import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;

public class PlayerResolver {

    private final zMRankup plugin;

    public PlayerResolver(zMRankup plugin) {
        this.plugin = plugin;
    }

    public void resolve(CommandSender sender, String targetName,
            BiConsumer<PlayerData, Player> action) {

        // Fast path: player is already online
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            PlayerData data = plugin.getPlayerDataCache().getOrCreate(online.getUniqueId());
            action.accept(data, online);
            plugin.getPlayerDataCache().save(online.getUniqueId());
            return;
        }

        // Slow path: player is offline
        plugin.getMessageManager().sendRaw(sender,
                "<gray>Player is offline – searching in database...");

        // Move the blocking Mojang lookup off the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

            // hasPlayedBefore() is safe to call async and avoids creating
            // phantom records for names that have never touched this server.
            if (!offlinePlayer.hasPlayedBefore()) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMessageManager().sendRaw(sender,
                        "<red>Player <white>" + targetName + "</white> has never played on this server."));
                return;
            }

            UUID uuid = offlinePlayer.getUniqueId();

            plugin.getStorageManager().loadPlayerData(uuid)
                    .thenAccept(dbData -> Bukkit.getScheduler().runTask(plugin, () -> {

                        // Race-condition guard
                        // Did the player log in while we were loading from DB?
                        Player nowOnline = Bukkit.getPlayer(uuid);
                        if (nowOnline != null) {
                            // Redirect to live cache; discard the stale DB snapshot
                            PlayerData liveData = plugin.getPlayerDataCache().getOrCreate(uuid);
                            action.accept(liveData, nowOnline);
                            plugin.getPlayerDataCache().save(uuid);
                            return;
                        }

                        // Normal offline path: mutate DB snapshot and persist
                        action.accept(dbData, null);
                        plugin.getStorageManager().savePlayerData(dbData);
                    }));
        });
    }
}