package dev.zm.rankup.command.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    /**
     * Executes the sub-command.
     *
     * @param sender the command sender
     * @param args   the full args array (args[0] = "admin", args[1] = sub-command
     *               name, args[2+] = params)
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Returns tab-completion suggestions for this sub-command.
     *
     * @param sender the command sender
     * @param args   the full args array
     * @return a list of suggestions, or an empty list
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}