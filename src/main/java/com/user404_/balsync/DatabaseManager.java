package com.user404_.balsync;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final BalSyncPlugin plugin;
    private HikariDataSource dataSource;
    private final String tableName;

    public DatabaseManager(BalSyncPlugin plugin) {
        this.plugin = plugin;
        this.tableName = plugin.getConfigManager().getTableName();
    }

    public boolean connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                    plugin.getConfigManager().getDatabaseHost(),
                    plugin.getConfigManager().getDatabasePort(),
                    plugin.getConfigManager().getDatabaseName()));
            config.setUsername(plugin.getConfigManager().getDatabaseUsername());
            config.setPassword(plugin.getConfigManager().getDatabasePassword());
            config.addDataSourceProperty("useSSL",
                    plugin.getConfigManager().useSSL());

            // Connection pool settings
            config.setMaximumPoolSize(plugin.getConfigManager().getMaxPoolSize());
            config.setMinimumIdle(plugin.getConfigManager().getMinIdle());
            config.setConnectionTimeout(plugin.getConfigManager().getConnectionTimeout());
            config.setIdleTimeout(plugin.getConfigManager().getIdleTimeout());
            config.setLeakDetectionThreshold(30000);

            dataSource = new HikariDataSource(config);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                plugin.getPluginLogger().info("Successfully connected to MySQL database!");
                return true;
            }
        } catch (SQLException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "Failed to connect to database!", e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getPluginLogger().info("Database connection closed.");
        }
    }

    public void setupTables() {
        // Verwende DECIMAL statt DOUBLE für genauere Währungswerte
        double startingBalance = plugin.getConfigManager().getStartingBalance();

        String createTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS `%s` (" +
                        "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                        "`player_uuid` CHAR(36) UNIQUE NOT NULL, " +
                        "`player_name` VARCHAR(16), " +
                        "`balance` DECIMAL(15, 2) NOT NULL DEFAULT %.2f, " +
                        "`last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "INDEX `idx_uuid` (`player_uuid`)" +
                        ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB",
                tableName, startingBalance
        );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            plugin.getPluginLogger().info("Database tables checked/created successfully!");

            // Optional: Protokolliere die erstellte Tabelle
            logTableInfo(conn);
        } catch (SQLException e) {
            plugin.getPluginLogger().log(Level.SEVERE, "Failed to create database tables!", e);

            // Fallback-SQL ohne DEFAULT-Wert
            tryFallbackTableCreation();
        }
    }

    private void logTableInfo(Connection conn) throws SQLException {
        String checkSQL = String.format("DESCRIBE `%s`", tableName);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSQL)) {
            plugin.getPluginLogger().info("Table structure for '" + tableName + "':");
            while (rs.next()) {
                plugin.getPluginLogger().info(String.format(
                        "Column: %s, Type: %s, Default: %s",
                        rs.getString("Field"),
                        rs.getString("Type"),
                        rs.getString("Default")
                ));
            }
        }
    }

    private void tryFallbackTableCreation() {
        plugin.getPluginLogger().warning("Trying fallback table creation...");

        // Fallback: Tabelle ohne DEFAULT-Wert, dann Standardwert über die Anwendung
        String fallbackSQL = String.format(
                "CREATE TABLE IF NOT EXISTS `%s` (" +
                        "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                        "`player_uuid` CHAR(36) UNIQUE NOT NULL, " +
                        "`player_name` VARCHAR(16), " +
                        "`balance` DECIMAL(15, 2) NOT NULL, " +
                        "`last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "INDEX `idx_uuid` (`player_uuid`)" +
                        ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB",
                tableName
        );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(fallbackSQL);
            plugin.getPluginLogger().info("Fallback table created successfully!");
        } catch (SQLException ex) {
            plugin.getPluginLogger().log(Level.SEVERE, "Fallback creation also failed!", ex);
        }
    }

    public double getBalance(UUID playerUUID) throws SQLException {
        String sql = String.format("SELECT balance FROM %s WHERE player_uuid = ?", tableName);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        }
        return plugin.getConfigManager().getStartingBalance();
    }

    public void saveBalance(UUID playerUUID, String playerName, double balance) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (player_uuid, player_name, balance) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), balance = VALUES(balance)",
                tableName
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, playerName);
            stmt.setDouble(3, balance);
            stmt.executeUpdate();
        }
    }

    /**
     * Atomically add a delta to the stored balance. This avoids overwriting the DB with stale
     * values when multiple servers write concurrently. If no row exists, insert one with
     * (startingBalance + delta).
     */
    public void addBalanceDelta(UUID playerUUID, String playerName, double delta) throws SQLException {
        String updateSql = String.format("UPDATE %s SET balance = balance + ? WHERE player_uuid = ?", tableName);

        try (Connection conn = dataSource.getConnection()) {
            // Try atomic update first
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setDouble(1, delta);
                update.setString(2, playerUUID.toString());
                int affected = update.executeUpdate();

                if (affected == 0) {
                    // No row existed — insert with startingBalance + delta
                    double starting = plugin.getConfigManager().getStartingBalance();
                    String insertSql = String.format("INSERT INTO %s (player_uuid, player_name, balance) VALUES (?, ?, ?)", tableName);
                    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                        insert.setString(1, playerUUID.toString());
                        insert.setString(2, playerName);
                        insert.setDouble(3, starting + delta);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}