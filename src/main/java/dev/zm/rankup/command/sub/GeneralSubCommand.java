package dev.zm.rankup.command.sub;

import dev.zm.rankup.zMRankup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GeneralSubCommand implements SubCommand {

    private final zMRankup plugin;

    public GeneralSubCommand(zMRankup plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String sub = args[1].toLowerCase();

        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                plugin.getMessageManager().send(sender, "config-reloaded");
            }
            case "version" -> {
                plugin.getMessageManager().send(sender, "admin-version-checking");
                plugin.getVersionChecker().checkNow().thenAccept(result -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String currentVersion = plugin.getDescription().getVersion();
                        if (result.updateAvailable()) {
                            plugin.getMessageManager().send(sender, "admin-version-update",
                                    "current_version", currentVersion,
                                    "latest_version", result.latestVersion(),
                                    "spigot_url", "https://www.spigotmc.org/resources/zmrankup.135973/");
                        } else {
                            plugin.getMessageManager().send(sender, "admin-version-latest",
                                    "current_version", currentVersion,
                                    "spigot_url", "https://www.spigotmc.org/resources/zmrankup.135973/");
                        }
                    });
                });
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}