package com.youssgm3o8.rokidragon.data;

import java.io.File; // Needed for path manipulation
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp; // For date handling
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ORMLite imports removed
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.models.Dragon;
import com.youssgm3o8.rokidragon.data.models.DragonEgg;
import com.youssgm3o8.rokidragon.data.models.DragonLog;
import com.youssgm3o8.rokidragon.data.models.StoredEgg;

import cn.nukkit.nbt.tag.CompoundTag;
// DbLib import was already removed

public class DatabaseManager {
    private final DragonPlugin plugin;
    private Connection connection; // Use java.sql.Connection
    private final String dbUrl;
    // ORMLite DAOs and ConnectionSource removed

    public DatabaseManager(DragonPlugin plugin) {
        this.plugin = plugin;
        // Construct DB URL using plugin's data folder
        File dbFile = new File(plugin.getDataFolder(), "rokidragon.db");
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        plugin.getLogger().info("Database Path: " + dbFile.getAbsolutePath());
    }

    /**
     * Initialize the database connection and create tables
     */
    public boolean initialize() {
        plugin.getLogger().info("Initializing standalone SQLite database connection...");
        try {
            // Load the SQLite JDBC driver (optional but good practice)
            Class.forName("org.sqlite.JDBC"); // Load standard driver name

            connection = DriverManager.getConnection(dbUrl);
            plugin.getLogger().info("Database connection established.");

            // Create tables using plain JDBC
            try (Statement stmt = connection.createStatement()) {
                plugin.getLogger().info("Creating tables if they do not exist...");

                // Dragon Eggs Table
                stmt.execute("CREATE TABLE IF NOT EXISTS dragon_eggs (" +
                             "eggId TEXT PRIMARY KEY, " +
                             "playerUUID TEXT NOT NULL, " +
                             "dragonType TEXT, " +
                             "dragonName TEXT, " +
                             "hatched INTEGER DEFAULT 0, " +
                             "incubating INTEGER DEFAULT 0, " +
                             "incubationProgress INTEGER DEFAULT 0, " +
                             "incubationStartTime INTEGER DEFAULT 0, " +
                             "purchaseDate INTEGER, " +
                             "isDead INTEGER DEFAULT 0, " +
                             "killedBy TEXT, " +
                             "deathTime INTEGER, " +
                             "deathLocation TEXT)");
                plugin.getLogger().info("Table 'dragon_eggs' created/verified.");

                // Check if death-related columns exist, add them if not
                try {
                    ResultSet rs = stmt.executeQuery("PRAGMA table_info(dragon_eggs)");
                    boolean hasIsDead = false;
                    boolean hasKilledBy = false;
                    boolean hasDeathTime = false;
                    boolean hasDeathLocation = false;
                    
                    while (rs.next()) {
                        String columnName = rs.getString("name");
                        if ("isDead".equalsIgnoreCase(columnName)) {
                            hasIsDead = true;
                        } else if ("killedBy".equalsIgnoreCase(columnName)) {
                            hasKilledBy = true;
                        } else if ("deathTime".equalsIgnoreCase(columnName)) {
                            hasDeathTime = true;
                        } else if ("deathLocation".equalsIgnoreCase(columnName)) {
                            hasDeathLocation = true;
                        }
                    }
                    
                    if (!hasIsDead) {
                        plugin.getLogger().info("Adding isDead column to dragon_eggs table...");
                        stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN isDead INTEGER DEFAULT 0");
                    }
                    
                    if (!hasKilledBy) {
                        plugin.getLogger().info("Adding killedBy column to dragon_eggs table...");
                        stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN killedBy TEXT");
                    }
                    
                    if (!hasDeathTime) {
                        plugin.getLogger().info("Adding deathTime column to dragon_eggs table...");
                        stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN deathTime INTEGER");
                    }
                    
                    if (!hasDeathLocation) {
                        plugin.getLogger().info("Adding deathLocation column to dragon_eggs table...");
                        stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN deathLocation TEXT");
                    }
                    
                } catch (SQLException e) {
                    plugin.getLogger().error("Error checking/adding death columns: " + e.getMessage(), e);
                }

                // Check if incubationStartTime column exists, add it if not
                try {
                    ResultSet rs = stmt.executeQuery("PRAGMA table_info(dragon_eggs)");
                    boolean hasIncubationStartTime = false;
                    while (rs.next()) {
                        if ("incubationStartTime".equalsIgnoreCase(rs.getString("name"))) {
                            hasIncubationStartTime = true;
                            break;
                        }
                    }
                    
                    if (!hasIncubationStartTime) {
                        plugin.getLogger().info("Adding incubationStartTime column to dragon_eggs table...");
                        stmt.execute("ALTER TABLE dragon_eggs ADD COLUMN incubationStartTime INTEGER DEFAULT 0");
                    }
                    
                    // Initialize any existing incubating eggs that might not have a start time
                    String sql = "UPDATE dragon_eggs SET incubationStartTime = ? WHERE incubating = 1 AND (incubationStartTime IS NULL OR incubationStartTime = 0)";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        long currentTime = System.currentTimeMillis() / 1000;
                        pstmt.setLong(1, currentTime);
                        int updatedRows = pstmt.executeUpdate();
                        if (updatedRows > 0) {
                            plugin.getLogger().info("Updated start time for " + updatedRows + " existing incubating eggs");
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().error("Error initializing incubation start times: " + e.getMessage(), e);
                    }
                    
                } catch (SQLException e) {
                    plugin.getLogger().error("Error checking/adding incubationStartTime column: " + e.getMessage(), e);
                }

                // Dragons Table
                stmt.execute("CREATE TABLE IF NOT EXISTS dragons (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "playerUUID TEXT NOT NULL UNIQUE, " +
                             "dragonEgg_id TEXT, " +
                             "FOREIGN KEY(dragonEgg_id) REFERENCES dragon_eggs(eggId))");
                plugin.getLogger().info("Table 'dragons' created/verified.");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_dragons_playerUUID ON dragons(playerUUID)");


                // Dragon Logs Table
                stmt.execute("CREATE TABLE IF NOT EXISTS dragon_logs (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "eggId TEXT NOT NULL, " +
                             "playerUUID TEXT NOT NULL, " +
                             "lostLocation TEXT, " +
                             "logDate INTEGER)");
                plugin.getLogger().info("Table 'dragon_logs' created/verified.");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_playerUUID ON dragon_logs(playerUUID)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_eggId ON dragon_logs(eggId)");


                // Stored Eggs Table
                stmt.execute("CREATE TABLE IF NOT EXISTS stored_eggs (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "playerUUID TEXT NOT NULL, " +
                             "slotNumber INTEGER, " +
                             "eggId TEXT NOT NULL, " +
                             "nbtData TEXT)");
                plugin.getLogger().info("Table 'stored_eggs' created/verified.");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_stored_playerUUID ON stored_eggs(playerUUID)");

                plugin.getLogger().info("Database tables initialized successfully.");
                return true;
            } catch (SQLException e) {
                plugin.getLogger().error("Error creating database tables: " + e.getMessage(), e);
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().error("Error establishing database connection: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates the database and creates necessary tables
     */
    public boolean validateDatabase() {
        return initialize();
    }

    /**
     * Check if player has a dragon
     * 
     * @param playerUUID The UUID of the player
     * @return true if player has a dragon
     */
    public boolean playerHasDragon(String playerUUID) {
        String sql = "SELECT COUNT(*) FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error checking if player has dragon: " + e.getMessage(), e);
        }
        return false; // Return false if error or no record found
    }

    /**
     * Get the dragon egg ID for a player
     * 
     * @param playerUUID The UUID of the player
     * @return The dragon egg ID
     */
    public String getDragonEggId(String playerUUID) {
        String sql = "SELECT dragonEgg_id FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("dragonEgg_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting dragon egg ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get the dragon type
     * 
     * @param eggId The egg ID
     * @return The dragon type
     */
    public String getDragonType(String eggId) {
        String sql = "SELECT dragonType FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString("dragonType");
                    plugin.getLogger().info("[DB Debug] getDragonType for eggId " + eggId + " found type: " + type);
                    // Return default if null or empty
                    return (type == null || type.trim().isEmpty()) ? "Fire Dragon" : type;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting dragon type: " + e.getMessage(), e);
        }
        plugin.getLogger().warning("[DB Debug] getDragonType for eggId " + eggId + " did not find entry or had error, returning default.");
        return "Fire Dragon"; // Default if not found or error
    }

    /**
     * Get the dragon name
     * 
     * @param eggId The egg ID
     * @return The dragon name
     */
    public String getDragonName(String eggId) {
        String sql = "SELECT dragonName FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("dragonName");
                    // Return default if null or empty
                    return (name == null || name.trim().isEmpty()) ? "Dragon" : name;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting dragon name: " + e.getMessage(), e);
        }
        return "Dragon"; // Default if not found or error
    }

    /**
     * Set the dragon name
     * 
     * @param eggId The egg ID
     * @param name The dragon name
     */
    public void setDragonName(String eggId, String name) {
        String sql = "UPDATE dragon_eggs SET dragonName = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, eggId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Error setting dragon name: " + e.getMessage(), e);
        }
    }

    /**
     * Set the dragon type
     * 
     * @param eggId The egg ID
     * @param type The dragon type
     */
    public void setDragonType(String eggId, String type) {
        String sql = "UPDATE dragon_eggs SET dragonType = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, eggId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Error setting dragon type: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an egg is hatched
     * 
     * @param eggId The egg ID
     * @return true if the egg is hatched
     */
    public boolean isEggHatched(String eggId) {
        String sql = "SELECT hatched FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("hatched") == 1; // Assuming 1 for true, 0 for false
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error checking if egg is hatched: " + e.getMessage(), e);
        }
        return false; // Default to false if not found or error
    }

    /**
     * Get the player UUID from an egg ID
     * 
     * @param eggId The egg ID
     * @return The player UUID
     */
    public String getPlayerUUIDFromEggId(String eggId) {
        String sql = "SELECT playerUUID FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("playerUUID");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting player UUID from egg ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Check if an egg is incubating
     * 
     * @param eggId The egg ID
     * @return true if the egg is incubating
     */
    public boolean isEggIncubating(String eggId) {
        String sql = "SELECT incubating FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("incubating") == 1; // Assuming 1 for true, 0 for false
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error checking if egg is incubating: " + e.getMessage(), e);
        }
        return false; // Default to false if not found or error
    }

    /**
     * Set if an egg is incubating
     * 
     * @param eggId The egg ID
     * @param incubating Whether the egg is incubating
     */
    public void setEggIncubating(String eggId, boolean incubating) {
        String sql = "UPDATE dragon_eggs SET incubating = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, incubating ? 1 : 0); // Use 1 for true, 0 for false
            pstmt.setString(2, eggId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Error setting egg incubating: " + e.getMessage(), e);
        }
    }

    /**
     * Get the incubating egg ID for a player
     * 
     * @param playerUUID The UUID of the player
     * @return The incubating egg ID
     */
    public String getIncubatingEggId(String playerUUID) {
        String sql = "SELECT eggId FROM dragon_eggs WHERE playerUUID = ? AND incubating = 1 LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("eggId");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting incubating egg ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get stored eggs for a player
     * 
     * @param playerUUID The UUID of the player
     * @return Map of slot numbers to egg NBT data
     */
    public Map<Integer, CompoundTag> getStoredEggs(String playerUUID) {
        Map<Integer, CompoundTag> eggs = new HashMap<>();
        String sqlStored = "SELECT slotNumber, eggId FROM stored_eggs WHERE playerUUID = ?";
        String sqlEggDetails = "SELECT dragonType, dragonName, hatched, incubating, incubationProgress FROM dragon_eggs WHERE eggId = ?";

        try (PreparedStatement pstmtStored = connection.prepareStatement(sqlStored)) {
            pstmtStored.setString(1, playerUUID);
            try (ResultSet rsStored = pstmtStored.executeQuery()) {
                while (rsStored.next()) {
                    int slotNumber = rsStored.getInt("slotNumber");
                    String eggId = rsStored.getString("eggId");

                    CompoundTag tag = new CompoundTag();
                    tag.putString("eggId", eggId);

                    // Get additional details from dragon_eggs table
                    try (PreparedStatement pstmtEgg = connection.prepareStatement(sqlEggDetails)) {
                        pstmtEgg.setString(1, eggId);
                        try (ResultSet rsEgg = pstmtEgg.executeQuery()) {
                            if (rsEgg.next()) {
                                String type = rsEgg.getString("dragonType");
                                String name = rsEgg.getString("dragonName");
                                tag.putString("dragonType", (type == null || type.trim().isEmpty()) ? "Fire Dragon" : type);
                                tag.putString("dragonName", (name == null || name.trim().isEmpty()) ? "Dragon" : name);
                                tag.putBoolean("hatched", rsEgg.getInt("hatched") == 1);
                                tag.putBoolean("incubating", rsEgg.getInt("incubating") == 1);
                                tag.putInt("incubationProgress", rsEgg.getInt("incubationProgress"));
                            } else {
                                plugin.getLogger().warning("Could not find DragonEgg data for stored eggId: " + eggId);
                                tag.putString("dragonType", "Unknown");
                                tag.putString("dragonName", "Unknown");
                                tag.putBoolean("hatched", false);
                            }
                        }
                    } catch (SQLException eggQueryException) {
                        plugin.getLogger().error("Error querying DragonEgg data for stored eggId " + eggId + ": " + eggQueryException.getMessage(), eggQueryException);
                    }
                    eggs.put(slotNumber, tag);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting stored eggs: " + e.getMessage(), e);
        }
        return eggs;
    }

    /**
     * Store an egg for a player
     * 
     * @param playerUUID The UUID of the player
     * @param dragonUUID The UUID of the dragon
     * @param dragonType The type of the dragon
     * @param dragonName The name of the dragon
     * @return true if successful
     */
    public boolean storeEgg(UUID playerUUID, UUID dragonUUID, String dragonType, String dragonName) {
        String eggId = dragonUUID.toString();
        String playerUUIDStr = playerUUID.toString();
        String sqlEggUpsert = "INSERT INTO dragon_eggs (eggId, playerUUID, dragonType, dragonName, hatched, incubating, incubationProgress, purchaseDate) " +
                              "VALUES (?, ?, ?, ?, 0, 0, 0, ?) " +
                              "ON CONFLICT(eggId) DO NOTHING"; // Simple upsert: insert if not exists
        String sqlDragonInsert = "INSERT OR IGNORE INTO dragons (playerUUID, dragonEgg_id) VALUES (?, ?)"; // Ignore if player already has a dragon record

        try (PreparedStatement pstmtEgg = connection.prepareStatement(sqlEggUpsert);
             PreparedStatement pstmtDragon = connection.prepareStatement(sqlDragonInsert)) {

            // Insert/Ignore DragonEgg
            pstmtEgg.setString(1, eggId);
            pstmtEgg.setString(2, playerUUIDStr);
            pstmtEgg.setString(3, dragonType);
            pstmtEgg.setString(4, dragonName);
            pstmtEgg.setLong(5, System.currentTimeMillis() / 1000); // Store purchase date as timestamp
            pstmtEgg.executeUpdate();

            // Insert/Ignore Dragon link
            pstmtDragon.setString(1, playerUUIDStr);
            pstmtDragon.setString(2, eggId);
            pstmtDragon.executeUpdate();

            return true;
        } catch (SQLException e) {
            plugin.getLogger().error("Error storing egg: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get egg data for a player
     * 
     * @param playerUUID The UUID of the player
     * @param eggId The egg ID
     * @return Map of egg data
     */
    public Map<String, String> getEggData(String playerUUID, String eggId) {
        Map<String, String> data = new HashMap<>();
        String sql = "SELECT * FROM dragon_eggs WHERE eggId = ? AND playerUUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            pstmt.setString(2, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data.put("eggId", rs.getString("eggId"));
                    data.put("dragonType", rs.getString("dragonType"));
                    data.put("dragonName", rs.getString("dragonName"));
                    data.put("hatched", String.valueOf(rs.getInt("hatched") == 1));
                    data.put("incubating", String.valueOf(rs.getInt("incubating") == 1));
                    data.put("incubationProgress", String.valueOf(rs.getInt("incubationProgress")));
                    long purchaseTimestamp = rs.getLong("purchaseDate");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    data.put("purchaseDate", sdf.format(new Date(purchaseTimestamp * 1000))); // Convert timestamp
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting egg data: " + e.getMessage(), e);
        }
        return data;
    }

    /**
     * Remove an egg for a player
     * 
     * @param playerUUID The UUID of the player
     * @param eggId The egg ID
     * @return true if successful
     */
    public boolean removeEgg(String playerUUID, String eggId) {
        String sqlSelectDragon = "SELECT dragonEgg_id FROM dragons WHERE playerUUID = ?";
        String sqlDeleteDragon = "DELETE FROM dragons WHERE playerUUID = ?";
        String sqlDeleteEgg = "DELETE FROM dragon_eggs WHERE eggId = ? AND playerUUID = ?";
        boolean originalAutoCommit = true; // Default to true

        try {
            // Start transaction
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            boolean eggRemoved = false;
            String currentDragonEggId = null;

            // Check if the player's current dragon uses this egg
            try (PreparedStatement pstmtSelect = connection.prepareStatement(sqlSelectDragon)) {
                pstmtSelect.setString(1, playerUUID);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        currentDragonEggId = rs.getString("dragonEgg_id");
                    }
                }
            }

            // If the current dragon uses this egg, remove the dragon link
            if (eggId.equals(currentDragonEggId)) {
                try (PreparedStatement pstmtDeleteDragon = connection.prepareStatement(sqlDeleteDragon)) {
                    pstmtDeleteDragon.setString(1, playerUUID);
                    pstmtDeleteDragon.executeUpdate();
                }
            }

            // Delete the egg itself
            try (PreparedStatement pstmtDeleteEgg = connection.prepareStatement(sqlDeleteEgg)) {
                pstmtDeleteEgg.setString(1, eggId);
                pstmtDeleteEgg.setString(2, playerUUID);
                int rowsAffected = pstmtDeleteEgg.executeUpdate();
                eggRemoved = (rowsAffected > 0);
            }

            // Commit transaction
            connection.commit();
            return eggRemoved;

        } catch (SQLException e) {
            plugin.getLogger().error("Error removing egg (transaction failed): " + e.getMessage(), e);
            // Rollback transaction on error
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                plugin.getLogger().error("Error rolling back transaction: " + rollbackEx.getMessage(), rollbackEx);
            }
            return false;
        } finally {
            // Restore original auto-commit state
            try {
                if (connection != null) {
                    connection.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException finalEx) {
                plugin.getLogger().error("Error restoring auto-commit state: " + finalEx.getMessage(), finalEx);
            }
        }
    }

    /**
     * Save stored eggs for a player
     * 
     * @param playerUUID The UUID of the player
     * @param eggs Map of slot numbers to egg NBT data
     */
    public void saveStoredEggs(String playerUUID, Map<Integer, CompoundTag> eggs) {
        String sqlDelete = "DELETE FROM stored_eggs WHERE playerUUID = ?";
        String sqlInsert = "INSERT INTO stored_eggs (playerUUID, slotNumber, eggId, nbtData) VALUES (?, ?, ?, ?)";
        boolean originalAutoCommit = true;

        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false); // Start transaction

            // Clear existing
            try (PreparedStatement pstmtDelete = connection.prepareStatement(sqlDelete)) {
                pstmtDelete.setString(1, playerUUID);
                pstmtDelete.executeUpdate();
            }

            // Insert new ones
            try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsert)) {
                for (Map.Entry<Integer, CompoundTag> entry : eggs.entrySet()) {
                    int slotNumber = entry.getKey();
                    CompoundTag tag = entry.getValue();
                    String eggId = tag.getString("eggId");

                    if (eggId == null || eggId.isEmpty()) {
                        plugin.getLogger().warning("Stored egg in slot " + slotNumber + " for player " + playerUUID + " is missing eggId NBT. Skipping.");
                        continue;
                    }

                    // We decided not to store NBT data directly in DB anymore
                    String nbtData = null; // Or handle NBT serialization if needed later

                    pstmtInsert.setString(1, playerUUID);
                    pstmtInsert.setInt(2, slotNumber);
                    pstmtInsert.setString(3, eggId);
                    pstmtInsert.setString(4, nbtData); // Store null or serialized NBT
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
            }

            connection.commit(); // Commit transaction

        } catch (SQLException e) {
            plugin.getLogger().error("Error saving stored eggs: " + e.getMessage(), e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException rollbackEx) {
                 plugin.getLogger().error("Error rolling back transaction: " + rollbackEx.getMessage(), rollbackEx);
            }
        } finally {
             try {
                 if (connection != null) connection.setAutoCommit(originalAutoCommit);
             } catch (SQLException finalEx) {
                 plugin.getLogger().error("Error restoring auto-commit state: " + finalEx.getMessage(), finalEx);
             }
        }
    }

    public int getPlayerStoredEggCount(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM stored_eggs s JOIN dragon_eggs d ON s.eggId = d.eggId WHERE s.playerUUID = ? AND (d.isDead IS NULL OR d.isDead = 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting stored egg count: " + e.getMessage(), e);
        }
        return 0;
    }

    public List<Map<String, Object>> getPlayerStoredEggs(UUID playerUUID) {
        List<Map<String, Object>> eggs = new ArrayList<>();
        // Join stored_eggs with dragon_eggs to get all data in one query
        String sql = "SELECT s.slotNumber, s.eggId FROM stored_eggs s WHERE s.playerUUID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> eggData = new HashMap<>();
                    eggData.put("slotNumber", rs.getInt("slotNumber"));
                    eggData.put("eggId", rs.getString("eggId"));
                    eggs.add(eggData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting player stored eggs: " + e.getMessage(), e);
        }
        return eggs;
    }

    /**
     * Remove a dragon for a player
     * 
     * @param playerUUID The UUID of the player
     * @return true if successful
     */
    public boolean removeDragon(String playerUUID) {
        String sql = "DELETE FROM dragons WHERE playerUUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().error("Error removing dragon: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean setEggHatched(String eggId, boolean hatched) {
        String sql = "UPDATE dragon_eggs SET hatched = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, hatched ? 1 : 0); // Use 1 for true, 0 for false
            pstmt.setString(2, eggId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().error("Error setting egg hatched: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean registerDragon(String playerUUID, String eggId, String dragonType, String dragonName) {
        String sqlSelectEgg = "SELECT COUNT(*) FROM dragon_eggs WHERE eggId = ?";
        String sqlInsertEgg = "INSERT INTO dragon_eggs (eggId, playerUUID, dragonType, dragonName, hatched, incubating, incubationProgress, purchaseDate) VALUES (?, ?, ?, ?, 0, 0, 0, ?)";
        String sqlUpdateEgg = "UPDATE dragon_eggs SET playerUUID = ?, dragonType = ?, dragonName = ? WHERE eggId = ?";
        String sqlSelectDragon = "SELECT id FROM dragons WHERE playerUUID = ?";
        String sqlUpdateDragon = "UPDATE dragons SET dragonEgg_id = ? WHERE playerUUID = ?";
        String sqlInsertDragon = "INSERT INTO dragons (playerUUID, dragonEgg_id) VALUES (?, ?)";
        boolean originalAutoCommit = true;

        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false); // Start transaction

            boolean eggExists = false;
            try (PreparedStatement pstmtSelect = connection.prepareStatement(sqlSelectEgg)) {
                pstmtSelect.setString(1, eggId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        eggExists = true;
                    }
                }
            }

            if (eggExists) {
                // Update existing egg
                try (PreparedStatement pstmtUpdate = connection.prepareStatement(sqlUpdateEgg)) {
                    pstmtUpdate.setString(1, playerUUID);
                    pstmtUpdate.setString(2, dragonType);
                    pstmtUpdate.setString(3, dragonName);
                    pstmtUpdate.setString(4, eggId);
                    pstmtUpdate.executeUpdate();
                }
            } else {
                // Create new egg
                try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsertEgg)) {
                    pstmtInsert.setString(1, eggId);
                    pstmtInsert.setString(2, playerUUID);
                    pstmtInsert.setString(3, dragonType);
                    pstmtInsert.setString(4, dragonName);
                    pstmtInsert.setLong(5, System.currentTimeMillis() / 1000); // purchaseDate
                    pstmtInsert.executeUpdate();
                }
            }

            boolean dragonRecordExists = false;
             try (PreparedStatement pstmtSelect = connection.prepareStatement(sqlSelectDragon)) {
                pstmtSelect.setString(1, playerUUID);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        dragonRecordExists = true;
                    }
                }
            }

            if (dragonRecordExists) {
                 // Update existing dragon record
                 try (PreparedStatement pstmtUpdate = connection.prepareStatement(sqlUpdateDragon)) {
                     pstmtUpdate.setString(1, eggId);
                     pstmtUpdate.setString(2, playerUUID);
                     pstmtUpdate.executeUpdate();
                 }
            } else {
                 // Create new dragon record
                 try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsertDragon)) {
                     pstmtInsert.setString(1, playerUUID);
                     pstmtInsert.setString(2, eggId);
                     pstmtInsert.executeUpdate();
                 }
            }

            connection.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            plugin.getLogger().error("Error registering dragon (transaction failed): " + e.getMessage(), e);
             try {
                 if (connection != null) connection.rollback();
             } catch (SQLException rollbackEx) {
                  plugin.getLogger().error("Error rolling back transaction: " + rollbackEx.getMessage(), rollbackEx);
             }
            return false;
        } finally {
             try {
                 if (connection != null) connection.setAutoCommit(originalAutoCommit);
             } catch (SQLException finalEx) {
                 plugin.getLogger().error("Error restoring auto-commit state: " + finalEx.getMessage(), finalEx);
             }
        }
    }

    /**
     * Get all eggs for a player (active, stored, and purchased eggs)
     * 
     * @param playerUUIDString The UUID string of the player
     * @return A list of maps containing egg information
     */
    public List<Map<String, Object>> getAllEggsForPlayer(String playerUUIDString) {
        if (playerUUIDString == null || playerUUIDString.trim().isEmpty()) {
            plugin.getLogger().warning("Attempted to get all eggs with null or empty UUID string.");
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Get all eggs owned by this player from the dragon_eggs table
        String sql = "SELECT eggId, dragonType, dragonName, hatched, incubating " +
                    "FROM dragon_eggs " +
                    "WHERE playerUUID = ?";
                    
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUIDString);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> eggData = new HashMap<>();
                    String eggId = rs.getString("eggId");
                    eggData.put("egg_id", eggId); // Use consistent key "egg_id"
                    eggData.put("type", rs.getString("dragonType"));
                    eggData.put("name", rs.getString("dragonName"));
                    eggData.put("is_hatched", rs.getInt("hatched") == 1);
                    // Note: 'is_active' status will be determined in the GUI logic now
                    result.add(eggData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting all eggs for player UUID " + playerUUIDString + ": " + e.getMessage(), e);
        }
        
        // Removed separate query for stored_eggs as dragon_eggs should be the primary source of ownership.
        // Storage status can be checked elsewhere if needed.
        
        return result;
    }

    /**
     * Get the purchase date of an egg
     * 
     * @param eggId The egg ID
     * @return The purchase date as a string
     */
    public String getEggPurchaseDate(String eggId) {
        String sql = "SELECT purchaseDate FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long purchaseTimestamp = rs.getLong("purchaseDate");
                    if (!rs.wasNull()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        return sdf.format(new Date(purchaseTimestamp * 1000));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting egg purchase date: " + e.getMessage(), e);
        }
        return "Unknown";
    }

    public boolean logLostEgg(String eggId, String playerUUID, String lostLocation) {
        String sql = "INSERT INTO dragon_logs (eggId, playerUUID, lostLocation, logDate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            pstmt.setString(2, playerUUID);
            pstmt.setString(3, lostLocation);
            pstmt.setLong(4, System.currentTimeMillis() / 1000); // Store log date as timestamp
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().error("Error logging lost egg: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete an egg from the database
     * 
     * @param eggId The ID of the egg to delete
     * @return true if successful
     */
    public boolean deleteEgg(String eggId) {
        String updateSql = "UPDATE dragon_eggs SET incubating = 0 WHERE eggId = ?";
        String deleteSql = "DELETE FROM dragon_eggs WHERE eggId = ?";
        
        try {
            // First ensure any incubation status is cleared for this egg
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                pstmt.setString(1, eggId);
                pstmt.executeUpdate();
            }
            
            // Then delete the egg record
            try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
                pstmt.setString(1, eggId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error deleting egg: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get recent lost egg logs
     * 
     * @param limit The maximum number of logs to retrieve
     * @return List of log data
     */
    public List<Map<String, Object>> getRecentLogs(int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT id, eggId, playerUUID, lostLocation, logDate FROM dragon_logs ORDER BY logDate DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    Map<String, Object> logData = new HashMap<>();
                    logData.put("id", rs.getInt("id"));
                    logData.put("eggId", rs.getString("eggId"));
                    logData.put("playerUUID", rs.getString("playerUUID"));
                    logData.put("lostLocation", rs.getString("lostLocation"));
                    long logTimestamp = rs.getLong("logDate");
                     if (!rs.wasNull()) {
                         logData.put("logDate", sdf.format(new Date(logTimestamp * 1000)));
                     } else {
                         logData.put("logDate", "Unknown");
                     }
                    logs.add(logData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting recent logs: " + e.getMessage(), e);
        }
        return logs;
    }

    public List<Map<String, Object>> getPlayerLogs(String playerUUID) {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT id, eggId, playerUUID, lostLocation, logDate FROM dragon_logs WHERE playerUUID = ? ORDER BY logDate DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (rs.next()) {
                    Map<String, Object> logData = new HashMap<>();
                    logData.put("id", rs.getInt("id"));
                    logData.put("eggId", rs.getString("eggId"));
                    logData.put("playerUUID", rs.getString("playerUUID"));
                    logData.put("lostLocation", rs.getString("lostLocation"));
                    long logTimestamp = rs.getLong("logDate");
                     if (!rs.wasNull()) {
                         logData.put("logDate", sdf.format(new Date(logTimestamp * 1000)));
                     } else {
                         logData.put("logDate", "Unknown");
                     }
                    logs.add(logData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting player logs: " + e.getMessage(), e);
        }
        return logs;
    }

    /**
     * Get all eggs that are currently incubating
     * 
     * @return List of incubating eggs
     */
    public List<IncubatingEgg> getAllIncubatingEggs() {
        List<IncubatingEgg> incubatingEggs = new ArrayList<>();
        String sql = "SELECT eggId, playerUUID, dragonType, dragonName, incubationProgress, incubationStartTime FROM dragon_eggs WHERE incubating = 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                String eggId = rs.getString("eggId");
                String playerUUID = rs.getString("playerUUID");
                String dragonType = rs.getString("dragonType");
                String dragonName = rs.getString("dragonName");
                int incubationProgress = rs.getInt("incubationProgress");
                
                // Get the incubation start time from database
                long incubationStartTime = rs.getLong("incubationStartTime");
                
                // If start time is 0 or not set, use current time but log a warning
                if (incubationStartTime == 0) {
                    plugin.getLogger().warning("Found incubating egg " + eggId + " with no start time! Setting to current time.");
                    incubationStartTime = System.currentTimeMillis() / 1000;
                    // Update it in the database
                    updateIncubationStartTime(eggId, incubationStartTime);
                }
                
                IncubatingEgg incubatingEgg = new IncubatingEgg(
                    eggId,
                    playerUUID,
                    dragonType,
                    (dragonName == null || dragonName.trim().isEmpty()) ? "Dragon" : dragonName, // Default name
                    incubationStartTime,
                    incubationProgress
                );
                incubatingEggs.add(incubatingEgg);
                
                plugin.getLogger().info("Found incubating egg: " + eggId + " for player " + playerUUID + 
                                       " with progress " + incubationProgress + " seconds, start time: " + incubationStartTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting all incubating eggs: " + e.getMessage(), e);
        }
        return incubatingEggs;
    }

    /**
     * Update incubation progress for an egg
     * 
     * @param eggId The egg ID
     * @param incubationSeconds The incubation progress in seconds
     */
    public void updateIncubationProgress(String eggId, int incubationSeconds) {
        if (eggId == null || eggId.isEmpty()) {
            plugin.getLogger().error("Cannot update incubation progress: eggId is null or empty");
            return;
        }
        
        if (incubationSeconds < 0) {
            plugin.getLogger().error("Cannot update incubation progress: seconds is negative: " + incubationSeconds);
            return;
        }
        
        String sql = "UPDATE dragon_eggs SET incubationProgress = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, incubationSeconds);
            pstmt.setString(2, eggId);
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                plugin.getLogger().info("Updated incubation progress for egg " + eggId + " to " + incubationSeconds + " seconds");
            } else {
                plugin.getLogger().warning("Failed to update incubation progress: egg ID not found in database: " + eggId);
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error updating incubation progress: " + e.getMessage(), e);
        }
    }

    /**
     * Update incubation start time for an egg
     * 
     * @param eggId The egg ID
     * @param startTime The incubation start time in seconds
     */
    public void updateIncubationStartTime(String eggId, long startTime) {
        if (eggId == null || eggId.isEmpty()) {
            plugin.getLogger().error("Cannot update incubation start time: eggId is null or empty");
            return;
        }
        
        if (startTime <= 0) {
            plugin.getLogger().error("Cannot update incubation start time: time is invalid: " + startTime);
            return;
        }
        
        String sql = "UPDATE dragon_eggs SET incubationStartTime = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, startTime);
            pstmt.setString(2, eggId);
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                plugin.getLogger().info("Updated incubation start time for egg " + eggId + " to " + startTime);
            } else {
                plugin.getLogger().warning("Failed to update incubation start time: egg ID not found in database: " + eggId);
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error updating incubation start time: " + e.getMessage(), e);
        }
    }

    public void updateDragonType(String eggId, String dragonType) {
        String sql = "UPDATE dragon_eggs SET dragonType = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, dragonType);
            pstmt.setString(2, eggId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("Error updating dragon type: " + e.getMessage(), e);
        }
    }

    /**
     * Clear stored eggs for a player
     * 
     * @param playerUUID The UUID of the player
     * @return true if successful
     */
    public boolean clearStoredEggs(UUID playerUUID) {
        String sql = "DELETE FROM stored_eggs WHERE playerUUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            int deleted = pstmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            plugin.getLogger().error("Error clearing stored eggs: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Close the database connection
     */
    public void closeConnection() { // Renamed for clarity
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().error("Error closing database connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get active dragon information for a player
     * 
     * @param playerName The name of the player
     * @return A map containing the dragon information, or null if player has no dragon
     */
    public Map<String, Object> getActiveDragonForPlayer(String playerName) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        Map<String, Object> result = new HashMap<>();
        
        if (!playerHasDragon(playerUUID)) {
            return null;
        }
        
        String eggId = getDragonEggId(playerUUID);
        if (eggId == null) {
            return null;
        }
        
        result.put("name", getDragonName(eggId));
        result.put("type", getDragonType(eggId));
        result.put("eggId", eggId);
        
        return result;
    }
    
    /**
     * Get all stored eggs for a player
     * 
     * @param playerName The name of the player
     * @return A list of maps containing stored egg information
     */
    public List<Map<String, Object>> getStoredEggsForPlayer(String playerName) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        String sql = "SELECT se.eggId, de.dragonType, de.dragonName " +
                    "FROM stored_eggs se " +
                    "JOIN dragon_eggs de ON se.eggId = de.eggId " +
                    "WHERE se.playerUUID = ?";
                    
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> eggData = new HashMap<>();
                    eggData.put("id", rs.getString("eggId"));
                    eggData.put("type", rs.getString("dragonType"));
                    eggData.put("name", rs.getString("dragonName"));
                    result.add(eggData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting stored eggs: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Get egg data by ID
     * 
     * @param eggId The egg ID
     * @return Map containing egg data or null if not found
     */
    public Map<String, Object> getEggById(String eggId) {
        Map<String, Object> data = new HashMap<>();
        String sql = "SELECT * FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    data.put("id", rs.getString("eggId"));
                    data.put("name", rs.getString("dragonName"));
                    data.put("type", rs.getString("dragonType"));
                    data.put("is_active", rs.getInt("hatched") == 1);
                    data.put("incubating", rs.getInt("incubating") == 1);
                    data.put("incubationProgress", rs.getInt("incubationProgress"));
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting egg by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Count stored eggs for a player
     * 
     * @param playerName The name of the player
     * @return The number of stored eggs
     */
    public int countStoredEggsForPlayer(String playerName) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        String sql = "SELECT COUNT(*) FROM stored_eggs WHERE playerUUID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error counting stored eggs: " + e.getMessage(), e);
        }
        
        return 0;
    }
    
    /**
     * Store an egg in the database
     * 
     * @param playerName The name of the player
     * @param eggId The ID of the egg to store
     * @return True if successful
     */
    public boolean storeEgg(String playerName, String eggId) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        // Find the next available slot
        int slotNumber = getNextAvailableSlot(playerUUID);
        if (slotNumber == -1) {
            return false; // No available slots
        }
        
        String sql = "INSERT INTO stored_eggs (playerUUID, slotNumber, eggId) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, slotNumber);
            pstmt.setString(3, eggId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().error("Error storing egg: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update dragon information for a player
     * 
     * @param playerName The name of the player
     * @param dragonName The new name for the dragon
     * @param dragonType The new type for the dragon
     * @return True if successful
     */
    public boolean updateDragonForPlayer(String playerName, String dragonName, String dragonType) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        if (!playerHasDragon(playerUUID)) {
            return false;
        }
        
        String eggId = getDragonEggId(playerUUID);
        if (eggId == null) {
            return false;
        }
        
        // Update dragon name
        setDragonName(eggId, dragonName);
        
        // Update dragon type
        setDragonType(eggId, dragonType);
        
        return true;
    }
    
    /**
     * Log a lost egg report
     * 
     * @param playerName The name of the player
     * @param eggId The ID of the lost egg
     * @param eggName The name of the dragon
     * @param eggType The type of the dragon
     * @param location The location where the egg was lost
     * @param timestamp The timestamp of the report
     * @return True if successful
     */
    public boolean logLostEgg(String playerName, String eggId, String eggName, String eggType, String location, String timestamp) {
        // Convert player name to UUID (implement this if you store players by name)
        // For now, we'll assume playerName is UUID string
        String playerUUID = playerName; // Adjust as needed for your implementation
        
        String sql = "INSERT INTO dragon_logs (eggId, playerUUID, lostLocation, logDate) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            pstmt.setString(2, playerUUID);
            pstmt.setString(3, location);
            
            // Parse and convert timestamp to long
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timestamp);
            long timeMillis = date.getTime();
            
            pstmt.setLong(4, timeMillis);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            plugin.getLogger().error("Error logging lost egg: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Find the next available slot for egg storage
     * 
     * @param playerUUID The UUID of the player
     * @return The next available slot number, or -1 if no slots available
     */
    private int getNextAvailableSlot(String playerUUID) {
        // Get max storage size from config
        int maxSlots = plugin.getConfig().getInt("gui.storage_size", 5);
        
        // Get used slots
        String sql = "SELECT slotNumber FROM stored_eggs WHERE playerUUID = ?";
        List<Integer> usedSlots = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    usedSlots.add(rs.getInt("slotNumber"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting used slots: " + e.getMessage(), e);
            return -1;
        }
        
        // Find first available slot
        for (int i = 0; i < maxSlots; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        
        return -1; // No available slots
    }

    /**
     * Get the incubation start time for an egg
     * 
     * @param eggId The egg ID
     * @return The incubation start time in seconds, or 0 if not set
     */
    public long getEggIncubationStartTime(String eggId) {
        String sql = "SELECT incubationStartTime FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("incubationStartTime");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting egg incubation start time: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Check if player has a specific metadata key
     * 
     * @param playerUUID The UUID of the player
     * @param key The metadata key to check
     * @return true if the metadata key exists for the player
     */
    public boolean hasPlayerMetadata(String playerUUID, String key) {
        // First, ensure the player_metadata table exists
        createPlayerMetadataTableIfNotExists();
        
        String sql = "SELECT COUNT(*) FROM player_metadata WHERE playerUUID = ? AND meta_key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error checking player metadata: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Set a metadata value for a player
     * 
     * @param playerUUID The UUID of the player
     * @param key The metadata key
     * @param value The metadata value
     * @return true if the metadata was set successfully
     */
    public boolean setPlayerMetadata(String playerUUID, String key, String value) {
        // First, ensure the player_metadata table exists
        createPlayerMetadataTableIfNotExists();
        
        // Check if the key already exists for this player
        if (hasPlayerMetadata(playerUUID, key)) {
            // Update existing record
            String sql = "UPDATE player_metadata SET meta_value = ? WHERE playerUUID = ? AND meta_key = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, value);
                pstmt.setString(2, playerUUID);
                pstmt.setString(3, key);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                plugin.getLogger().error("Error updating player metadata: " + e.getMessage(), e);
                return false;
            }
        } else {
            // Insert new record
            String sql = "INSERT INTO player_metadata (playerUUID, meta_key, meta_value) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID);
                pstmt.setString(2, key);
                pstmt.setString(3, value);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                plugin.getLogger().error("Error inserting player metadata: " + e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * Get a metadata value for a player
     * 
     * @param playerUUID The UUID of the player
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public String getPlayerMetadata(String playerUUID, String key) {
        // First, ensure the player_metadata table exists
        createPlayerMetadataTableIfNotExists();
        
        String sql = "SELECT meta_value FROM player_metadata WHERE playerUUID = ? AND meta_key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("meta_value");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting player metadata: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Create the player_metadata table if it doesn't exist
     */
    private void createPlayerMetadataTableIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_metadata (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "playerUUID TEXT NOT NULL, " +
                         "meta_key TEXT NOT NULL, " +
                         "meta_value TEXT, " +
                         "UNIQUE(playerUUID, meta_key))");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_metadata_playerUUID ON player_metadata(playerUUID)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_metadata_key ON player_metadata(meta_key)");
        } catch (SQLException e) {
            plugin.getLogger().error("Error creating player_metadata table: " + e.getMessage(), e);
        }
    }

    /**
     * Marks a dragon as dead in the database
     * 
     * @param eggId The ID of the egg/dragon
     * @param killedBy Who or what killed the dragon (entity name or "unknown")
     * @param deathLocation Location where the dragon died
     * @return true if successfully marked as dead
     */
    public boolean markDragonAsDead(String eggId, String killedBy, String deathLocation) {
        String sql = "UPDATE dragon_eggs SET isDead = 1, killedBy = ?, deathTime = ?, deathLocation = ? WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
            pstmt.setString(1, killedBy);
            pstmt.setLong(2, currentTime);
            pstmt.setString(3, deathLocation);
            pstmt.setString(4, eggId);
            
            int updated = pstmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            plugin.getLogger().error("Error marking dragon as dead: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a dragon is dead
     * 
     * @param eggId The ID of the egg/dragon
     * @return true if the dragon is marked as dead
     */
    public boolean isDragonDead(String eggId) {
        String sql = "SELECT isDead FROM dragon_eggs WHERE eggId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("isDead") == 1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error checking if dragon is dead: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Gets death information for a dragon
     * 
     * @param eggId The ID of the egg/dragon
     * @return Map with death details or null if not found/dead
     */
    public Map<String, Object> getDragonDeathInfo(String eggId) {
        String sql = "SELECT killedBy, deathTime, deathLocation FROM dragon_eggs WHERE eggId = ? AND isDead = 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, eggId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> deathInfo = new HashMap<>();
                    deathInfo.put("killedBy", rs.getString("killedBy"));
                    
                    // Format the death time as a readable date
                    long deathTime = rs.getLong("deathTime");
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(deathTime * 1000));
                    deathInfo.put("deathTime", formattedDate);
                    
                    deathInfo.put("deathLocation", rs.getString("deathLocation"));
                    return deathInfo;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting dragon death info: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Gets the count of dead dragons a player has in storage
     * 
     * @param uuid The UUID of the player
     * @return The count of dead dragons in storage
     */
    public int getDeadDragonCount(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM stored_eggs s JOIN dragon_eggs d ON s.eggId = d.eggId WHERE s.playerUUID = ? AND d.isDead = 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().error("Error getting dead dragon count: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Checks if a dragon egg exists in the database
     * @param eggId The egg ID to check
     * @return true if the egg exists, false if not
     */
    public boolean doesEggExist(String eggId) {
        final String sql = "SELECT COUNT(*) FROM dragon_eggs WHERE eggId = ?";
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, eggId);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking if egg exists: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Checks if a dragon egg is owned by a specific player
     * @param eggId The egg ID to check
     * @param playerUuid The player UUID to verify as owner
     * @return true if the player owns the egg, false if not
     */
    public boolean isEggOwnedByPlayer(String eggId, String playerUuid) {
        final String sql = "SELECT COUNT(*) FROM dragon_eggs WHERE eggId = ? AND playerUUID = ?";
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, eggId);
            statement.setString(2, playerUuid);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking egg ownership: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Gets a connection to the database.
     * @return a connection to the database
     * @throws SQLException if a database access error occurs
     */
    private Connection getConnection() throws SQLException {
        // Use the existing connection or create a new one if needed
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
        }
        return connection;
    }
} 