package com.user404_.balsync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BalSyncCommand implements CommandExecutor {
    private final BalSyncPlugin plugin;
    private final BalanceManager balanceManager;

    public BalSyncCommand(BalSyncPlugin plugin, BalanceManager balanceManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("balsync.admin")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().reload();
                plugin.getTranslationManager().loadMessages();
                sendMessage(sender, "config-reloaded");
                break;

            case "save":
                balanceManager.saveAllBalances();
                sendMessage(sender, "balance-saved");
                break;

            case "load":
                if (sender instanceof Player) {
                    balanceManager.loadPlayerBalance((Player) sender);
                } else {
                    sendMessage(sender, "player-not-found");
                }
                break;

            case "status":
                sendStatusInfo(sender);
                break;

            case "backup":
                plugin.getBackupManager().createBackup();
                sender.sendMessage("§aBackup started (check console for completion).");
                break;

            // NEW: backups list
            case "backups":
                listBackups(sender);
                break;

            // NEW: rollback
            case "rollback":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /balsync rollback <filename> [player]");
                    return true;
                }
                String fileName = args[1];
                String targetPlayer = (args.length >= 3) ? args[2] : null;
                boolean started = plugin.getBackupManager().rollback(fileName, targetPlayer);
                if (started) {
                    sender.sendMessage("§aRollback started from file: " + fileName +
                            (targetPlayer != null ? " for player " + targetPlayer : " for all players"));
                } else {
                    sender.sendMessage("§cBackup file not found: " + fileName);
                }
                break;

            default:
                sendMessage(sender, "usage");
                break;
        }

        return true;
    }

    private void sendMessage(CommandSender sender, String messageKey) {
        String message = plugin.getTranslationManager().formatMessage(messageKey);
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if ("actionbar".equalsIgnoreCase(plugin.getConfigManager().getMessageDisplay())) {
                player.sendActionBar(message);
            } else {
                player.sendMessage(message);
            }
        } else {
            sender.sendMessage(message);
        }
    }

    private void sendStatusInfo(CommandSender sender) {
        sender.sendMessage("§6=== BalSync Status ===");
        sender.sendMessage("§7Auto-save interval: §e" + plugin.getConfigManager().getAutoSaveInterval() + "s");
        sender.sendMessage("§7Database polling: §e" + plugin.getConfigManager().getDbPollInterval() + "s");
        sender.sendMessage("§7Reset on join: §e" + plugin.getConfigManager().isResetOnJoin());
        sender.sendMessage("§7Offline monitoring: §e" + plugin.getConfigManager().monitorOfflineChanges());
    }

    // NEW: Display list of backups
    private void listBackups(CommandSender sender) {
        List<String> backups = plugin.getBackupManager().listBackups();
        if (backups.isEmpty()) {
            sender.sendMessage("§cNo backups found.");
            return;
        }
        sender.sendMessage("§6=== Available Backups ===");
        for (String name : backups) {
            sender.sendMessage("§7- §e" + name);
        }
        sender.sendMessage("§7Use §e/balsync rollback <filename> [player]§7 to restore.");
    }
}