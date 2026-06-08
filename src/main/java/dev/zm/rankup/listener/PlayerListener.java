package dev.zm.rankup.listener;

import dev.zm.rankup.zMRankup;
import dev.zm.rankup.storage.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final zMRankup plugin;

    public PlayerListener(zMRankup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataCache().load(event.getPlayer());
        plugin.getVersionChecker().notifyPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataCache().saveAndRemove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        data.incrementBlocksMined(event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            PlayerData data = plugin.getPlayerDataCache().getOrCreate(killer.getUniqueId());
            data.incrementMobKills(event.getEntity().getType().name(), 1);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData victimData = plugin.getPlayerDataCache().getOrCreate(player.getUniqueId());
        victimData.incrementDeaths(1);

        Player killer = player.getKiller();
        if (killer != null) {
            PlayerData killerData = plugin.getPlayerDataCache().getOrCreate(killer.getUniqueId());
            killerData.incrementPlayerKills(1);
        }
    }
}
