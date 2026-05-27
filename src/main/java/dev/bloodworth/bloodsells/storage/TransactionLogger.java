package dev.bloodworth.bloodsells.storage;

import dev.bloodworth.bloodsells.config.BloodConfig;
import dev.bloodworth.bloodsells.economy.EconomyKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public final class TransactionLogger implements AutoCloseable {
    private final JavaPlugin plugin;
    private final BloodConfig config;
    private Connection connection;

    public TransactionLogger(JavaPlugin plugin, BloodConfig config) {
        this.plugin = plugin;
        this.config = config;
        if (config.transactionLog()) {
            open();
        }
    }

    public void log(UUID player, String item, int amount, double payout, EconomyKey economy) {
        if (!config.transactionLog() || connection == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO bloodsells_transactions(time, player, item, amount, payout, economy) VALUES (?, ?, ?, ?, ?, ?)")) {
                    statement.setString(1, Instant.now().toString());
                    statement.setString(2, player.toString());
                    statement.setString(3, item);
                    statement.setInt(4, amount);
                    statement.setDouble(5, payout);
                    statement.setString(6, economy.raw());
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to log BloodSells transaction", ex);
                }
            }
        });
    }

    private void open() {
        try {
            String type = plugin.getConfig().getString("settings.storage.type", "SQLITE");
            if ("MYSQL".equalsIgnoreCase(type)) {
                connection = DriverManager.getConnection(
                        plugin.getConfig().getString("settings.storage.mysql.jdbc-url", ""),
                        plugin.getConfig().getString("settings.storage.mysql.username", ""),
                        plugin.getConfig().getString("settings.storage.mysql.password", ""));
            } else {
                File db = new File(plugin.getDataFolder(), "transactions.db");
                plugin.getDataFolder().mkdirs();
                connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS bloodsells_transactions (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          time TEXT NOT NULL,
                          player TEXT NOT NULL,
                          item TEXT NOT NULL,
                          amount INTEGER NOT NULL,
                          payout REAL NOT NULL,
                          economy TEXT NOT NULL
                        )
                        """);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Transaction storage disabled.", ex);
            connection = null;
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
