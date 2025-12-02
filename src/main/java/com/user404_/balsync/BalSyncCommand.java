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
            sender.sendMessage(plugin.getTranslationManager().formatMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getTranslationManager().formatMessage("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().reload();
                plugin.getTranslationManager().loadMessages();
                sender.sendMessage(plugin.getTranslationManager().formatMessage("config-reloaded"));
                break;

            case "save":
                // Save all balances - Methode ist jetzt in BalanceManager
                balanceManager.saveAllBalances();
                sender.sendMessage(plugin.getTranslationManager().formatMessage("balance-saved"));
                break;

            case "load":
                if (sender instanceof Player) {
                    balanceManager.loadPlayerBalance((Player) sender);
                } else {
                    sender.sendMessage(plugin.getTranslationManager().formatMessage("player-not-found"));
                }
                break;

            case "status":
                sendStatusInfo(sender);
                break;

            default:
                sender.sendMessage(plugin.getTranslationManager().formatMessage("usage"));
                break;
        }

        return true;
    }

    private void sendStatusInfo(CommandSender sender) {
        sender.sendMessage("§6=== BalSync Status ===");
        sender.sendMessage("§7Auto-save interval: §e" + plugin.getConfigManager().getAutoSaveInterval() + "s");
        sender.sendMessage("§7Database polling: §e" + plugin.getConfigManager().getDbPollInterval() + "s");
        sender.sendMessage("§7Reset on join: §e" + plugin.getConfigManager().isResetOnJoin());
        sender.sendMessage("§7Offline monitoring: §e" + plugin.getConfigManager().monitorOfflineChanges());
    }
}