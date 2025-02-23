package com.youssgm3o8.rokidragon;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ensureConnection();
        try {
            // Create version table
            Statement stmt = connection.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS db_version (version INTEGER PRIMARY KEY)");
            
            // Check current version
            ResultSet rs = stmt.executeQuery("SELECT version FROM db_version");
            int currentVersion = rs.next() ? rs.getInt("version") : 0;
            
            if (currentVersion < 1) {
                // Create initial tables
                stmt.execute("CREATE TABLE IF NOT EXISTS dragon_eggs (" +
                    "eggId TEXT PRIMARY KEY," +
                    "playerUUID TEXT NOT NULL," +
                    "onlineTime INTEGER DEFAULT 0," +
                    "hatched INTEGER DEFAULT 0," +
                    "dragonType TEXT," +
                    "isIncubating INTEGER DEFAULT 0" +
                    ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS dragons (" +
                    "playerUUID TEXT PRIMARY KEY," +
                    "dragonId TEXT NOT NULL" +
                    ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS dragon_egg_storage (" +
                    "playerUUID TEXT NOT NULL," +
                    "slot INTEGER NOT NULL," +
                    "eggData TEXT NOT NULL," +
                    "PRIMARY KEY (playerUUID, slot)" +
                    ")");

                // Set version to 1
                stmt.execute("INSERT INTO db_version (version) VALUES (1)");
            }

            if (currentVersion < 2) {
                // Add isIncubating column if it doesn't exist
                try {
                    stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN isIncubating INTEGER DEFAULT 0");
                } catch (SQLException e) {
                    // Column might already exist, ignore
                }
                
                // Update version
                stmt.execute("UPDATE db_version SET version = 2");
            }

        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error creating tables: ", e);
        }
    }

    private int getCurrentVersion() {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT version FROM db_version LIMIT 1");
            if (rs.next()) {
                return rs.getInt("version");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting database version: ", e);
        }
        return 0;
    }

    private void upgradeDatabase(int fromVersion) {
        try (Statement statement = connection.createStatement()) {
            if (fromVersion < 2) {
                // Add isIncubating column if it doesn't exist
                try {
                    statement.execute("ALTER TABLE dragon_eggs ADD COLUMN isIncubating INTEGER NOT NULL DEFAULT 0");
                } catch (SQLException e) {
                    // Column might already exist, ignore
                }
                statement.execute("UPDATE db_version SET version = 2");
            }
            // Add future upgrades here with higher version numbers
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error upgrading database: ", e);
        }
    }

    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getConnection();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error ensuring database connection: ", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error closing database connection: ", e);
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
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
        String sql = "UPDATE dragon_eggs SET onlineTime = onlineTime + 1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error incrementing egg online time: ", e);
        }
    }

    public void setEggHatched(String eggId) {
        ensureConnection();
        String sql = "UPDATE dragon_eggs SET hatched = 1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error setting egg as hatched: ", e);
        }
    }

    public boolean isEggHatched(String eggId) {
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
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
        ensureConnection();
        String sql = "DELETE FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error removing dragon egg: ", e);
        }
    }

    public void removeDragon(String playerUUID) {
        ensureConnection();
        String sql = "DELETE FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error removing dragon: ", e);
        }
    }

    public void markEggAdminHatched(String eggId) {
        ensureConnection();
        String sql = "UPDATE dragon_eggs SET hatched = 1, onlineTime = -1 WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error marking egg as admin-hatched: ", e);
        }
    }

    public void setDragonType(String eggId, String dragonType) {
        ensureConnection();
        String sql = "UPDATE dragon_eggs SET dragonType = ? WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, dragonType);
            preparedStatement.setString(2, eggId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                plugin.getLogger().info("Successfully set dragon type to " + dragonType + " for egg " + eggId);
            } else {
                plugin.getLogger().warning("No rows updated when setting dragon type " + dragonType + " for egg " + eggId);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error setting dragon type: ", e);
        }
    }

    public String getDragonType(String eggId) {
        ensureConnection();
        String sql = "SELECT dragonType FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String type = resultSet.getString("dragonType");
                if (type == null || type.trim().isEmpty()) {
                    plugin.getLogger().warning("Dragon type not found for egg " + eggId + ", defaulting to Fire Dragon");
                    return "Fire Dragon";
                }
                plugin.getLogger().info("Retrieved dragon type: " + type + " for egg " + eggId);
                return type;
            }
            plugin.getLogger().warning("No egg found with ID " + eggId + ", defaulting to Fire Dragon");
            return "Fire Dragon";
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting dragon type: ", e);
            return "Fire Dragon";
        }
    }

    public Map<Integer, CompoundTag> getStoredEggs(String playerUUID) {
        ensureConnection();
        Map<Integer, CompoundTag> storedEggs = new HashMap<>();
        String sql = "SELECT slot, eggData FROM dragon_egg_storage WHERE playerUUID = ?";
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                int slot = resultSet.getInt("slot");
                String eggDataStr = resultSet.getString("eggData");
                try {
                    // Create a new CompoundTag and put all the stored data
                    CompoundTag eggData = new CompoundTag();
                    String[] dataParts = eggDataStr.split(";");
                    for (String part : dataParts) {
                        String[] keyValue = part.split("=", 2);
                        if (keyValue.length == 2) {
                            eggData.putString(keyValue[0], keyValue[1]);
                        }
                    }
                    storedEggs.put(slot, eggData);
                } catch (Exception e) {
                    plugin.getLogger().error("Failed to parse egg data for slot " + slot, e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to get stored eggs for player " + playerUUID, e);
        }
        
        return storedEggs;
    }

    public void saveStoredEggs(String playerUUID, Map<Integer, CompoundTag> eggs) {
        ensureConnection();
        // First, clear existing storage for this player
        String deleteSql = "DELETE FROM dragon_egg_storage WHERE playerUUID = ?";
        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
            deleteStatement.setString(1, playerUUID);
            deleteStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error clearing stored eggs: ", e);
            return;
        }

        // Then insert new storage data
        String insertSql = "INSERT INTO dragon_egg_storage (playerUUID, slot, eggData) VALUES (?, ?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            for (Map.Entry<Integer, CompoundTag> entry : eggs.entrySet()) {
                CompoundTag tag = entry.getValue();
                // Convert CompoundTag to string format
                StringBuilder dataStr = new StringBuilder();
                for (String key : tag.getTags().keySet()) {
                    if (dataStr.length() > 0) {
                        dataStr.append(";");
                    }
                    dataStr.append(key).append("=").append(tag.getString(key));
                }
                
                insertStatement.setString(1, playerUUID);
                insertStatement.setInt(2, entry.getKey());
                insertStatement.setString(3, dataStr.toString());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error saving stored eggs: ", e);
        }
    }

    public void setEggIncubating(String eggId, boolean incubating) {
        ensureConnection();
        String sql = "UPDATE dragon_eggs SET isIncubating = ? WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, incubating ? 1 : 0);
            preparedStatement.setString(2, eggId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error setting egg incubation status: ", e);
        }
    }

    public boolean isEggIncubating(String eggId) {
        ensureConnection();
        String sql = "SELECT isIncubating FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, eggId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("isIncubating") == 1;
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error checking egg incubation status: ", e);
            return false;
        }
    }

    public String getIncubatingEggId(String playerUUID) {
        ensureConnection();
        String sql = "SELECT eggId FROM dragon_eggs WHERE playerUUID = ? AND isIncubating = 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("eggId");
            }
            return null;
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting incubating egg: ", e);
            return null;
        }
    }

    public List<String> getPlayerEggIds(String playerUUID) {
        ensureConnection();
        List<String> eggIds = new ArrayList<>();
        String sql = "SELECT eggId FROM dragon_eggs WHERE playerUUID = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, playerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                eggIds.add(resultSet.getString("eggId"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(LogLevel.CRITICAL, "Error getting player eggs: ", e);
        }
        return eggIds;
    }
}
