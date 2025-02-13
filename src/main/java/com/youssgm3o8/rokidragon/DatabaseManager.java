package com.youssgm3o8.rokidragon;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class DatabaseManager {

    private final PluginBase plugin;
    private final String dbPath;
    private Connection connection;
    private final String logFilePath;

    public DatabaseManager(PluginBase plugin) {
        this.plugin = plugin;
        this.dbPath = new File(plugin.getDataFolder(), "dragons.db").getAbsolutePath();
        this.logFilePath = new File(plugin.getDataFolder(), "duplicate_eggs.log").getAbsolutePath();
        this.connection = getConnection();
        createTables();
    }

    private Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting database connection: ", e);
            return null;
        }
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            String eggsTableSQL = "CREATE TABLE IF NOT EXISTS dragon_eggs (" +
                    "eggId TEXT PRIMARY KEY," +
                    "playerUUID TEXT NOT NULL," +
                    "onlineTime INTEGER NOT NULL DEFAULT 0," +
                    "hatched INTEGER NOT NULL DEFAULT 0)";

            String dragonsTableSQL = "CREATE TABLE IF NOT EXISTS dragons (" +
                    "playerUUID TEXT PRIMARY KEY," +
                    "dragonId TEXT," +
                    "FOREIGN KEY (dragonId) REFERENCES dragon_eggs(eggId))";

            statement.execute(eggsTableSQL);
            statement.execute(dragonsTableSQL);

        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error creating tables: ", e);
        }
    }

    public void logDuplicateEgg(String playerUUID, String eggId) {
        try {
            String logMessage = String.format("Duplicate egg detected for player %s with egg ID %s%n", playerUUID, eggId);
            Files.write(Paths.get(logFilePath), logMessage.getBytes(), java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error logging duplicate egg: ", e);
        }
    }

    public boolean hasDragonEgg(String playerUUID) {
        String sql = "SELECT eggId FROM dragon_eggs WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error checking for dragon egg: ", e);
            return false;
        }
    }

    public void insertDragonEgg(String eggId, String playerUUID) {
        String sql = "INSERT INTO dragon_eggs (eggId, playerUUID, onlineTime, hatched) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.setString(2, playerUUID);
            preparedStatement.setInt(3, 0); // Initial online time is 0
            preparedStatement.setInt(4, 0);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error inserting dragon egg: ", e);
        }
    }

    public String getDragonEggId(String playerUUID) {
        String sql = "SELECT eggId FROM dragon_eggs WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("eggId");
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting dragon egg ID: ", e);
            return null;
        }
    }

    public int getEggOnlineTime(String eggId) {
        String sql = "SELECT onlineTime FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("onlineTime");
            }
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting egg online time: ", e);
            return 0;
        }
    }

    public void incrementEggOnlineTime(String eggId) {
        String sql = "UPDATE dragon_eggs SET onlineTime = onlineTime + 1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error incrementing egg online time: ", e);
        }
    }

    public void setEggHatched(String eggId) {
        String sql = "UPDATE dragon_eggs SET hatched = 1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error setting egg as hatched: ", e);
        }
    }

    public boolean isEggHatched(String eggId) {
        String sql = "SELECT hatched FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("hatched") == 1;
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error checking if egg is hatched: ", e);
            return false;
        }
    }

    public void insertDragon(String playerUUID, String dragonId) {
        String sql = "INSERT INTO dragons (playerUUID, dragonId) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            preparedStatement.setString(2, dragonId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error inserting dragon: ", e);
        }
    }

    public boolean playerHasDragon(String playerUUID) {
        String sql = "SELECT dragonId FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error checking if player has dragon: ", e);
            return false;
        }
    }

    public String getDragonId(String playerUUID) {
        String sql = "SELECT dragonId FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("dragonId");
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting dragon ID: ", e);
            return null;
        }
    }

    public void removeDragonEgg(String eggId) {
        String sql = "DELETE FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error removing dragon egg: ", e);
        }
    }

    public void removeDragon(String playerUUID) {
        String sql = "DELETE FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error removing dragon: ", e);
        }
    }

    public void markEggAdminHatched(String eggId) {
        String sql = "UPDATE dragon_eggs SET hatched = 1, onlineTime = -1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error marking egg as admin-hatched: ", e);
        }
    }
}
