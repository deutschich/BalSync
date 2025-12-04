package com.user404_.balsync;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class BalanceManager {
    private final BalSyncPlugin plugin;
    private final Economy economy;
    private final DatabaseManager databaseManager;
    private BukkitTask dbPollingTask;
    private final Map<UUID, Double> lastKnownBalances = new HashMap<>();
    private final Map<UUID, Double> lastKnownDbBalances = new HashMap<>();

    public void saveAllBalances() {
        plugin.getPluginLogger().info("Saving all player balances to database...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int saved = 0;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                try {
                    if (economy.hasAccount(player)) {
                        double balance = economy.getBalance(player);
                        databaseManager.saveBalance(player.getUniqueId(), player.getName(), balance);
                        lastKnownBalances.put(player.getUniqueId(), balance);
                        lastKnownDbBalances.put(player.getUniqueId(), balance);
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

    public BalanceManager(BalSyncPlugin plugin, Economy economy, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.databaseManager = databaseManager;
        startDbPolling();
        startOfflineMonitoring();
    }

    // MODIFIED: Added reset functionality
    public void loadPlayerBalance(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double databaseBalance = databaseManager.getBalance(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Ensure player has account
                    if (!economy.hasAccount(player)) {
                        economy.createPlayerAccount(player);
                    }

                    // RESET TO ZERO if configured
                    double currentBalance = economy.getBalance(player);
                    if (plugin.getConfigManager().isResetOnJoin()) {
                        if (currentBalance > 0) {
                            economy.withdrawPlayer(player, currentBalance);
                        } else if (currentBalance < 0) {
                            economy.depositPlayer(player, Math.abs(currentBalance));
                        }
                        plugin.getLogger().info("Reset balance to 0 for " + player.getName());
                        currentBalance = 0;
                    }

                    // Apply database balance (OVERWRITE)
                    double difference = databaseBalance - currentBalance;
                    if (difference > 0) {
                        economy.depositPlayer(player, difference);
                    } else if (difference < 0) {
                        economy.withdrawPlayer(player, Math.abs(difference));
                    }

                    // Update tracking maps
                    lastKnownBalances.put(player.getUniqueId(), databaseBalance);
                    lastKnownDbBalances.put(player.getUniqueId(), databaseBalance);

                    plugin.getLogger().info("Balance loaded for " + player.getName() +
                            ": " + databaseBalance + " (from DB)");

                    // Send message to player
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

    // NEW: Poll database for external changes
    private void startDbPolling() {
        int interval = plugin.getConfigManager().getDbPollInterval();
        if (interval <= 0) return;

        dbPollingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            pollDatabaseForChanges();
        }, interval * 20L, interval * 20L);

        plugin.getLogger().info("Started database polling every " + interval + " seconds");
    }

    // NEW: Check database for balance changes and apply to online players
    private void pollDatabaseForChanges() {
        List<UUID> onlineUUIDs = getOnlinePlayerUUIDs();

        // Keine online Spieler → nichts abfragen
        if (onlineUUIDs.isEmpty()) {
            return;
        }

        try (Connection conn = databaseManager.getConnection()) {
            // Platzhalter für die IN-Klausel erstellen
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < onlineUUIDs.size(); i++) {
                placeholders.append("?");
                if (i < onlineUUIDs.size() - 1) {
                    placeholders.append(",");
                }
            }

            String sql = String.format(
                    "SELECT player_uuid, balance FROM %s WHERE player_uuid IN (%s)",
                    plugin.getConfigManager().getTableName(),
                    placeholders.toString()
            );

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // UUIDs als Parameter setzen
                for (int i = 0; i < onlineUUIDs.size(); i++) {
                    stmt.setString(i + 1, onlineUUIDs.get(i).toString());
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                    double dbBalance = rs.getDouble("balance");

                    // Prüfen, ob sich die Datenbank-Balance geändert hat
                    Double lastDbBalance = lastKnownDbBalances.get(playerUUID);
                    if (lastDbBalance == null || Math.abs(dbBalance - lastDbBalance) > 0.001) {
                        // Datenbank hat sich geändert → auf Spieler anwenden
                        applyDbChangeToPlayer(playerUUID, dbBalance, lastDbBalance);
                        lastKnownDbBalances.put(playerUUID, dbBalance);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error polling database for changes", e);
        }
    }

    // NEW: Apply database changes to online player
    private void applyDbChangeToPlayer(UUID playerUUID, double newBalance, Double oldBalance) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                double currentBalance = economy.getBalance(player);
                double difference = newBalance - currentBalance;

                if (Math.abs(difference) > 0.001) {
                    if (difference > 0) {
                        economy.depositPlayer(player, difference);
                    } else {
                        economy.withdrawPlayer(player, Math.abs(difference));
                    }

                    lastKnownBalances.put(playerUUID, newBalance);

                    plugin.getLogger().info("Applied external DB change for " +
                            player.getName() + ": " + newBalance);

                    // Notify player if configured
                    if (plugin.getConfigManager().notifyOnExternalChange()) {
                        String message = plugin.getTranslationManager().getMessage(
                                "balance-external-change");
                        if (message != null && !message.isEmpty()) {
                            String formatted = message
                                    .replace("{old}", String.format("%.2f", oldBalance != null ? oldBalance : currentBalance))
                                    .replace("{new}", String.format("%.2f", newBalance))
                                    .replace("&", "§");
                            player.sendMessage(plugin.getTranslationManager().formatMessage("prefix") + formatted);
                        }
                    }
                }
            });
        }
    }

    // NEW: Monitor offline player balance changes when auto-save-interval = 0
    private void startOfflineMonitoring() {
        int autoSaveInterval = plugin.getConfigManager().getAutoSaveInterval();
        boolean monitorOffline = plugin.getConfigManager().monitorOfflineChanges();

        if (autoSaveInterval == 0 && monitorOffline) {
            // Check for balance changes every 30 seconds
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                monitorOfflineBalanceChanges();
            }, 600L, 600L); // 30 seconds (600 ticks)

            plugin.getLogger().info("Started offline balance change monitoring");
        }
    }

    // NEW: Detect and save offline balance changes
    private void monitorOfflineBalanceChanges() {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (economy.hasAccount(offlinePlayer)) {
                double currentBalance = economy.getBalance(offlinePlayer);
                UUID uuid = offlinePlayer.getUniqueId();

                Double lastBalance = lastKnownBalances.get(uuid);
                if (lastBalance != null && Math.abs(currentBalance - lastBalance) > 0.001) {
                    // Balance has changed on this server. Instead of overwriting the DB with
                    // a stale server value, compute the delta and apply that to the DB.
                    double delta = currentBalance - lastBalance;
                    try {
                        databaseManager.addBalanceDelta(uuid, offlinePlayer.getName(), delta);

                        // Refresh DB snapshot for tracking
                        double newDbBalance = databaseManager.getBalance(uuid);

                        lastKnownBalances.put(uuid, currentBalance);
                        lastKnownDbBalances.put(uuid, newDbBalance);

                        plugin.getLogger().info("Detected offline change for " +
                                offlinePlayer.getName() + ": serverDelta=" + delta + ", newDB=" + newDbBalance);

                        // Optional: send configured offline-change message to console/log
                        String msg = plugin.getTranslationManager().getMessage("offline-change-detected");
                        if (msg != null && !msg.isEmpty()) {
                            plugin.getLogger().info(plugin.getTranslationManager().formatMessage("prefix") + msg);
                        }
                    } catch (SQLException e) {
                        plugin.getPluginLogger().log(Level.WARNING,
                                "Failed to save offline change for " + offlinePlayer.getName(), e);
                    }
                } else if (lastBalance == null) {
                    // First time seeing this player on this server, store initial balance
                    lastKnownBalances.put(uuid, currentBalance);
                }
            }
        }
    }

    // NEW: Track balance when player quits
    public void trackPlayerQuit(UUID playerUUID, double balance) {
        lastKnownBalances.put(playerUUID, balance);
    }

    // Helper method
    private List<UUID> getOnlinePlayerUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    // MODIFIED savePlayerBalance to update tracking
    public void savePlayerBalance(OfflinePlayer player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double balance = economy.getBalance(player);
                UUID uuid = player.getUniqueId();

                Double lastBalance = lastKnownBalances.get(uuid);
                if (lastBalance != null) {
                    // There is a last-known server-side snapshot => compute delta and apply it
                    double delta = balance - lastBalance;
                    if (Math.abs(delta) > 0.001) {
                        databaseManager.addBalanceDelta(uuid, player.getName(), delta);

                        double newDb = databaseManager.getBalance(uuid);
                        lastKnownBalances.put(uuid, balance);
                        lastKnownDbBalances.put(uuid, newDb);
                        return;
                    }
                }

                // Fallback: overwrite DB with current balance
                databaseManager.saveBalance(player.getUniqueId(), player.getName(), balance);
                lastKnownBalances.put(player.getUniqueId(), balance);
                lastKnownDbBalances.put(player.getUniqueId(), balance);
            } catch (SQLException e) {
                plugin.getPluginLogger().log(Level.SEVERE,
                        "Failed to save balance for player: " + player.getName(), e);
            }
        });
    }

    // Cleanup on disable
    public void shutdown() {
        if (dbPollingTask != null) {
            dbPollingTask.cancel();
        }
        lastKnownBalances.clear();
        lastKnownDbBalances.clear();
    }
}