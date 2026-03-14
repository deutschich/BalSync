package com.user404_.balsync;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BalSyncTabCompleter implements TabCompleter {

    private final BalSyncPlugin plugin;
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "save", "load", "status", "backup", "backups", "rollback"
    );

    public BalSyncTabCompleter(BalSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Only players/admins with permission should get suggestions
        if (!sender.hasPermission("balsync.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // Suggest subcommands based on what the user typed
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }

        if (args.length == 2) {
            // Second argument: only for rollback subcommand
            if (args[0].equalsIgnoreCase("rollback")) {
                // Suggest backup file names
                List<String> backups = plugin.getBackupManager().listBackups();
                return StringUtil.copyPartialMatches(args[1], backups, new ArrayList<>());
            }
            // Other subcommands have no second argument
            return Collections.emptyList();
        }

        if (args.length == 3) {
            // Third argument: only for rollback subcommand
            if (args[0].equalsIgnoreCase("rollback")) {
                // Suggest online player names
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return StringUtil.copyPartialMatches(args[2], playerNames, new ArrayList<>());
            }
            return Collections.emptyList();
        }

        // No more arguments to complete
        return Collections.emptyList();
    }
}