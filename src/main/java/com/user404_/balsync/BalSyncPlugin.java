package com.user404_.balsync;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public class BalSyncPlugin extends JavaPlugin {
    private static BalSyncPlugin instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private BalanceManager balanceManager;
    private TranslationManager translationManager;
    private ConfigManager configManager;
    private Logger logger;
    private BackupManager backupManager;
    private UpdateChecker updateChecker;
    private BukkitTask updateCheckTask;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // PREVIEW BUILD CHANGES: Show warning if this is a preview build
        if ("preview".equals(BuildConfig.BUILD_TYPE)) {
            logger.warning("=========================================");
            logger.warning("This is a PREVIEW build (commit: " + BuildConfig.COMMIT_ID + ").");
            logger.warning("It may contain bugs and should not be used in production.");
            logger.warning("Update checking is disabled.");
            logger.warning("=========================================");
        }

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
        getCommand("balsync").setTabCompleter(new BalSyncTabCompleter(this));

        // Start auto-save task if enabled
        int interval = configManager.getAutoSaveInterval();
        if (interval > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                    () -> balanceManager.saveAllBalances(),
                    interval * 20L, interval * 20L);
        }

        backupManager = new BackupManager(this);

        // Schedule backup task if enabled
        if (configManager.isBackupEnabled()) {
            int intervalMinutes = configManager.getBackupIntervalMinutes();
            if (intervalMinutes > 0) {
                long ticks = intervalMinutes * 60L * 20L;
                getServer().getScheduler().runTaskTimerAsynchronously(this,
                        () -> backupManager.createBackup(),
                        ticks, ticks);
                logger.info("Backups scheduled every " + intervalMinutes + " minute(s).");
            } else {
                logger.warning("Backup enabled but interval is 0 or negative; backups disabled.");
            }
        }

        // PREVIEW BUILD CHANGES: Only start update checker for release builds
        if (configManager.checkForUpdates() && BuildConfig.UPDATE_CHECKER_ENABLED) {
            updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates(); // immediate check

            long updateCheckInterval = 6L * 60L * 60L * 20L; // 6 hours in ticks
            updateCheckTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                    () -> updateChecker.checkForUpdates(), updateCheckInterval, updateCheckInterval);
        }

        logger.info("BalSync v" + getDescription().getVersion() + " enabled successfully!");
        logger.info("The Official Version of BalSync is by User404_ (or deutschich on GitHub)");
        logger.info("Other Copies may not be safe!");
    }

    @Override
    public void onDisable() {
        // Save all balances on shutdown
        if (balanceManager != null) {
            balanceManager.saveAllBalances();
            balanceManager.shutdown();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        if (updateCheckTask != null) {
            updateCheckTask.cancel();
        }

        logger.info("BalSync disabled.");
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
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

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public BalanceManager getBalanceManager() {
        return balanceManager;
    }
}