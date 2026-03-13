package com.user404_.balsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class BackupManager {
    private final BalSyncPlugin plugin;
    private final Economy economy;
    private final File backupFolder;
    private final Gson gson;

    public BackupManager(BalSyncPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Creates a backup of all player balances in a JSON file.
     * File name format: backup-YYYY-MM-DD-HH-mm-ss.json
     */
    public void createBackup() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
                File backupFile = new File(backupFolder, "backup-" + timestamp + ".json");

                JsonArray playersArray = new JsonArray();
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (economy.hasAccount(player)) {
                        JsonObject playerObj = new JsonObject();
                        playerObj.addProperty("uuid", player.getUniqueId().toString());
                        playerObj.addProperty("name", player.getName());
                        playerObj.addProperty("balance", economy.getBalance(player));
                        playersArray.add(playerObj);
                    }
                }

                JsonObject root = new JsonObject();
                root.addProperty("timestamp", timestamp);
                root.add("players", playersArray);

                try (FileWriter writer = new FileWriter(backupFile)) {
                    gson.toJson(root, writer);
                }

                plugin.getLogger().info("Backup created: " + backupFile.getName());

                // Clean old backups after successful creation
                cleanOldBackups();

            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
            }
        });
    }

    /**
     * Deletes oldest backup files if the number exceeds max-files.
     */
    private void cleanOldBackups() {
        int maxFiles = plugin.getConfigManager().getBackupMaxFiles();
        if (maxFiles <= 0) return; // 0 = unlimited

        File[] files = backupFolder.listFiles((dir, name) -> name.startsWith("backup-") && name.endsWith(".json"));
        if (files == null || files.length <= maxFiles) return;

        // Sort files by creation time (oldest first)
        List<File> fileList = Arrays.asList(files);
        fileList.sort(Comparator.comparingLong(f -> {
            try {
                return Files.readAttributes(f.toPath(), BasicFileAttributes.class).creationTime().toMillis();
            } catch (IOException e) {
                return f.lastModified(); // fallback
            }
        }));

        // Delete oldest files until we are within limit
        int toDelete = fileList.size() - maxFiles;
        for (int i = 0; i < toDelete; i++) {
            File f = fileList.get(i);
            if (f.delete()) {
                plugin.getLogger().info("Deleted old backup: " + f.getName());
            } else {
                plugin.getLogger().warning("Could not delete old backup: " + f.getName());
            }
        }
    }

    // ========== NEW: Rollback methods ==========

    /**
     * Returns a list of available backup file names (without path).
     */
    public List<String> listBackups() {
        File[] files = backupFolder.listFiles((dir, name) -> name.startsWith("backup-") && name.endsWith(".json"));
        if (files == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (File f : files) {
            names.add(f.getName());
        }
        // Sort by newest first (reverse chronological)
        names.sort(Collections.reverseOrder());
        return names;
    }

    /**
     * Rolls back player balances from a specified backup file.
     * @param fileName The backup file name (e.g., "backup-2025-03-13-12-30-00.json")
     * @param targetPlayer Optional player name (if null, rollback all players in backup)
     * @return true if rollback was successfully started (async), false if file not found.
     */
    public boolean rollback(String fileName, String targetPlayer) {
        File backupFile = new File(backupFolder, fileName);
        if (!backupFile.exists()) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> performRollback(backupFile, targetPlayer));
        return true;
    }

    private void performRollback(File backupFile, String targetPlayer) {
        plugin.getLogger().info("Starting rollback from file: " + backupFile.getName() +
                (targetPlayer != null ? " for player: " + targetPlayer : " for all players"));

        try (FileReader reader = new FileReader(backupFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray playersArray = root.getAsJsonArray("players");

            int updated = 0;
            UUID targetUuid = null;
            if (targetPlayer != null) {
                // Try to resolve UUID from name (may be offline)
                OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetPlayer);
                if (targetOffline.hasPlayedBefore() || targetOffline.isOnline()) {
                    targetUuid = targetOffline.getUniqueId();
                } else {
                    // Could not resolve – maybe the name is from backup, we'll match by name string
                    // Keep targetUuid null, we'll compare by name later
                }
            }

            for (var element : playersArray) {
                JsonObject playerObj = element.getAsJsonObject();
                String uuidStr = playerObj.get("uuid").getAsString();
                String name = playerObj.get("name").getAsString();
                double balance = playerObj.get("balance").getAsDouble();

                // If targeting a specific player, check if this entry matches
                if (targetPlayer != null) {
                    boolean matches = false;
                    if (targetUuid != null) {
                        matches = uuidStr.equals(targetUuid.toString());
                    } else {
                        matches = name.equalsIgnoreCase(targetPlayer);
                    }
                    if (!matches) continue;
                }

                // Apply rollback to this player
                applyRollbackToPlayer(UUID.fromString(uuidStr), name, balance);
                updated++;
            }

            String message = "Rollback completed. Updated " + updated + " player(s) from backup.";
            plugin.getLogger().info(message);

            // --- FIX: use a final copy of 'updated' for the lambda ---
            final int updatedFinal = updated;
            Bukkit.getScheduler().runTask(plugin, () -> {
                String broadcast = plugin.getTranslationManager().formatMessage("rollback-completed",
                        String.valueOf(updatedFinal), backupFile.getName());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("balsync.admin")) {
                        p.sendMessage(broadcast);
                    }
                }
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Rollback failed", e);
        }
    }

    private void applyRollbackToPlayer(UUID uuid, String name, double balance) {
        try {
            // 1. Update database (overwrite)
            plugin.getDatabaseManager().saveBalance(uuid, name, balance);

            // 2. If player is online, update their Vault balance and tracking maps
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                // Use Vault to set the balance exactly (by resetting and then adding)
                double current = economy.getBalance(onlinePlayer);
                if (Math.abs(current - balance) > 0.001) {
                    if (current > 0) {
                        economy.withdrawPlayer(onlinePlayer, current);
                    } else if (current < 0) {
                        economy.depositPlayer(onlinePlayer, Math.abs(current));
                    }
                    if (balance > 0) {
                        economy.depositPlayer(onlinePlayer, balance);
                    } else if (balance < 0) {
                        economy.withdrawPlayer(onlinePlayer, Math.abs(balance));
                    }
                }

                // Update BalanceManager's tracking maps
                BalanceManager bm = plugin.getBalanceManager();
                bm.updateLastKnown(uuid, balance);
                bm.updateLastKnownDb(uuid, balance);
            }

            plugin.getLogger().fine("Rollback applied to " + name + " (" + uuid + "): new balance " + balance);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to rollback player " + name, e);
        }
    }
}