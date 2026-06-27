package dev.zm.rankup.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import dev.zm.rankup.config.PlaceholderContext;
import dev.zm.rankup.zMRankup;

import java.util.List;

public class RewardExecutor {

    public static void execute(Player player, Reward reward) {
        execute(player, reward, PlaceholderContext.empty());
    }

    public static void execute(Player player, Reward reward, PlaceholderContext context) {
        if (reward == null)
            return;
        executeCommands(player, reward.getCommands(), context);
    }

    public static void executeCommands(Player player, List<String> commands) {
        executeCommands(player, commands, PlaceholderContext.empty());
    }

    public static void executeCommands(Player player, List<String> commands, PlaceholderContext context) {
        if (commands == null || commands.isEmpty())
            return;

        Runnable task = () -> {
            for (String command : commands) {
                String parsedCommand = zMRankup.getInstance().getMessageManager().replacePlaceholders(
                        player,
                        context,
                        command,
                        "player", player.getName(),
                        "player_name", player.getName(),
                        "player_uuid", player.getUniqueId().toString());

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(zMRankup.getInstance(), task);
        }
    }

    public static void executeAll(Player player, List<Reward> rewards) {
        executeAll(player, rewards, PlaceholderContext.empty());
    }

    public static void executeAll(Player player, List<Reward> rewards, PlaceholderContext context) {
        if (rewards == null || rewards.isEmpty())
            return;
        for (Reward reward : rewards) {
            execute(player, reward, context);
        }
    }
}
