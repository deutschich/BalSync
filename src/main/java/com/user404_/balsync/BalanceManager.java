package com.user404_.balsync;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class BalanceManager {
    private final BalSyncPlugin plugin;
    private final Economy economy;
    private final DatabaseManager databaseManager;

    public BalanceManager(BalSyncPlugin plugin, Economy economy, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.databaseManager = databaseManager;
    }

    public void loadPlayerBalance(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double databaseBalance = databaseManager.getBalance(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Sicherstellen, dass der Spieler ein Konto hat
                    if (!economy.hasAccount(player)) {
                        economy.createPlayerAccount(player);
                    }

                    // Aktuelle Balance abrufen
                    double currentBalance = economy.getBalance(player);

                    // KOMPLETTE ÜBERSCHREIBUNG der Balance
                    // 1. Zuerst auf 0 setzen (alles abheben oder auf 0 bringen)
                    if (currentBalance > 0) {
                        // Positive Balance abheben
                        economy.withdrawPlayer(player, currentBalance);
                    } else if (currentBalance < 0) {
                        // Negative Balance ausgleichen (einzahlen um auf 0 zu kommen)
                        economy.depositPlayer(player, Math.abs(currentBalance));
                    }

                    // 2. Datenbankbalance einzahlen (ÜBERSCHREIBT die alte Balance)
                    economy.depositPlayer(player, databaseBalance);

                    // Logging
                    plugin.getLogger().info("BALANCE OVERWRITTEN for " + player.getName() +
                            ": Old=" + currentBalance + ", New=" + databaseBalance + " (from DB)");

                    // Nachricht an Spieler
                    String message = plugin.getTranslationManager().getMessage("balance-loaded");
                    if (message != null && !message.isEmpty()) {
                        player.sendMessage(plugin.getTranslationManager().formatMessage(message));
                    }
                });

            } catch (SQLException e) {
                plugin.getPluginLogger().log(Level.SEVERE,
                        "Failed to load balance for player: " + player.getName(), e);
            }
        });
    }

    public void savePlayerBalance(OfflinePlayer player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double balance = economy.getBalance(player);
                databaseManager.saveBalance(player.getUniqueId(), player.getName(), balance);
            } catch (SQLException e) {
                plugin.getPluginLogger().log(Level.SEVERE,
                        "Failed to save balance for player: " + player.getName(), e);
            }
        });
    }

    public void saveAllBalances() {
        plugin.getPluginLogger().info("Saving all player balances to database...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int saved = 0;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                try {
                    if (economy.hasAccount(player)) {
                        double balance = economy.getBalance(player);
                        databaseManager.saveBalance(player.getUniqueId(), player.getName(), balance);
                        saved++;
                    }
                } catch (SQLException e) {
                    plugin.getPluginLogger().log(Level.WARNING,
                            "Failed to save balance for: " + player.getName(), e);
                }
            }
            plugin.getPluginLogger().info("Saved " + saved + " player balances to database.");
        });
    }

    public double getCachedBalance(UUID playerUUID) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player != null && economy.hasAccount(player)) {
            return economy.getBalance(player);
        }
        return plugin.getConfigManager().getStartingBalance();
    }
}