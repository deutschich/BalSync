package com.user404_.balsync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
}