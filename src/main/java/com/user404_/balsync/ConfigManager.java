package com.user404_.balsync;

import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

public class ConfigManager {
    private final BalSyncPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(BalSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Save default config from resources
        plugin.saveDefaultConfig();

        // Reload configuration
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Set default values if not present
        setDefaults();
    }

    private void setDefaults() {
        config.addDefault("database.host", "localhost");
        config.addDefault("database.port", 3306);
        config.addDefault("database.database", "minecraft");
        config.addDefault("database.username", "root");
        config.addDefault("database.password", "password");
        config.addDefault("database.use-ssl", false);
        config.addDefault("database.connection-pool.maximum-pool-size", 10);
        config.addDefault("database.connection-pool.minimum-idle", 5);
        config.addDefault("database.connection-pool.connection-timeout", 30000);
        config.addDefault("database.connection-pool.idle-timeout", 600000);

        config.addDefault("settings.auto-save-interval", 60);
        config.addDefault("settings.save-on-quit", true);
        config.addDefault("settings.starting-balance", 100.0);
        config.addDefault("settings.locale", "en");

        config.addDefault("tables.player_balances.table-name", "player_balances");
        config.addDefault("tables.player_balances.uuid-column", "player_uuid");
        config.addDefault("tables.player_balances.balance-column", "balance");
        config.addDefault("tables.player_balances.last-updated-column", "last_updated");

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // Database getters
    public String getDatabaseHost() {
        return config.getString("database.host", "localhost");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return config.getString("database.database", "minecraft");
    }

    public String getDatabaseUsername() {
        return config.getString("database.username", "root");
    }

    public String getDatabasePassword() {
        return config.getString("database.password", "password");
    }

    public boolean useSSL() {
        return config.getBoolean("database.use-ssl", false);
    }

    public int getMaxPoolSize() {
        return config.getInt("database.connection-pool.maximum-pool-size", 10);
    }

    public int getMinIdle() {
        return config.getInt("database.connection-pool.minimum-idle", 5);
    }

    public int getConnectionTimeout() {
        return config.getInt("database.connection-pool.connection-timeout", 30000);
    }

    public int getIdleTimeout() {
        return config.getInt("database.connection-pool.idle-timeout", 600000);
    }

    // Settings getters
    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 60);
    }

    public boolean saveOnQuit() {
        return config.getBoolean("settings.save-on-quit", true);
    }

    public double getStartingBalance() {
        return config.getDouble("settings.starting-balance", 100.0);
    }

    public String getLocale() {
        return config.getString("settings.locale", "en");
    }

    // Table getters
    public String getTableName() {
        return config.getString("tables.player_balances.table-name", "player_balances");
    }
    public boolean isResetOnJoin() {
        return config.getBoolean("settings.reset-on-join", false);
    }

    public boolean monitorOfflineChanges() {
        return config.getBoolean("settings.monitor-offline-changes", true);
    }

    public int getDbPollInterval() {
        return config.getInt("settings.db-poll-interval", 10);
    }

    public boolean notifyOnExternalChange() {
        return config.getBoolean("settings.notify-on-external-change", true);
    }
}