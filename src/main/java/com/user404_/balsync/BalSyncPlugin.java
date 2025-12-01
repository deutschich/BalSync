package com.user404_.balsync;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import java.util.logging.Logger;

public class BalSyncPlugin extends JavaPlugin {
    private static BalSyncPlugin instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private BalanceManager balanceManager;
    private TranslationManager translationManager;
    private ConfigManager configManager;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // Load configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Load translations
        translationManager = new TranslationManager(this);
        translationManager.loadMessages();

        // Setup Vault economy
        if (!setupEconomy()) {
            logger.severe("Vault economy not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            logger.severe("Failed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup database tables
        databaseManager.setupTables();

        // Initialize balance manager
        balanceManager = new BalanceManager(this, economy, databaseManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this, balanceManager), this);

        // Register commands
        getCommand("balsync").setExecutor(new BalSyncCommand(this, balanceManager));

        // Start auto-save task if enabled
        int interval = configManager.getAutoSaveInterval();
        if (interval > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                    () -> balanceManager.saveAllBalances(),
                    interval * 20L, interval * 20L);
        }

        logger.info("BalSync v" + getDescription().getVersion() + " enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all balances on shutdown
        if (balanceManager != null) {
            balanceManager.saveAllBalances();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        logger.info("BalSync disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public static BalSyncPlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Logger getPluginLogger() {
        return logger;
    }
}