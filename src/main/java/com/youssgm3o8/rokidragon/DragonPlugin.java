package com.youssgm3o8.rokidragon;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.data.DatabaseManager;
import com.youssgm3o8.rokidragon.dragon.DragonEntity;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;
import com.youssgm3o8.rokidragon.manager.DragonEggManager;
import com.youssgm3o8.rokidragon.manager.DragonShardManager;
import com.youssgm3o8.rokidragon.manager.DragonManager;
import com.youssgm3o8.rokidragon.manager.DragonLoafManager;
import com.youssgm3o8.rokidragon.task.ProcessIncubationTask;
import com.youssgm3o8.rokidragon.language.LanguageManager;
import com.youssgm3o8.rokidragon.commands.DragonCommandExecutor;
import com.youssgm3o8.rokidragon.commands.DragonLogsCommandExecutor;
import com.youssgm3o8.rokidragon.entities.EntityBedFireBall;
import com.youssgm3o8.rokidragon.entities.EntityIceBall;
import com.youssgm3o8.rokidragon.entities.EntityLightningBall;
import com.youssgm3o8.rokidragon.listeners.EventListenerEdit;
import com.youssgm3o8.rokidragon.listeners.DragonEggListener;
import com.youssgm3o8.rokidragon.listeners.FormResponseListener;
import cn.nukkit.inventory.CraftingManager;
import java.util.List;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.nukkit.plugin.PluginManager;
import java.util.concurrent.TimeUnit;

/**
 * Main plugin class for RokiDragon. Handles initialization, command registration,
 * event listening, and provides access to managers and configuration.
 */
public class DragonPlugin extends PluginBase implements Listener {
    private static DragonPlugin instance;
    // SQLiteDatabase
    private DatabaseManager databaseManager;
    // The cooldown map will contain the name of a player along with the expiry time in milliseconds
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();
    // Active dragon entities
    private final Map<UUID, DragonEntity> activeDragons = new ConcurrentHashMap<>();
    // The dragon manager for spawning/despawning dragons
    private DragonManager dragonManager;
    // The dragon egg manager for handling eggs
    private DragonEggManager eggManager;
    // The dragon shard manager for handling shards
    private DragonShardManager shardManager;
    // The dragon loaf manager
    private DragonLoafManager loafManager; 
    // GUI manager for dragon management (now using forms instead of fake inventories)
    private FormBasedDragonGUI dragonGUI;
    // Configuration
    private Config config;
    private LanguageManager languageManager;

    /**
     * Called when the plugin is loaded. Registers custom entities.
     */
    @Override
    public void onLoad() {
        // Register the dragon entity type
        EntityManager.get().registerDefinition(DragonEntity.DEFINITION);
    }

    /**
     * Called when the plugin is enabled. Initializes managers, loads configuration,
     * registers commands and listeners, and schedules tasks.
     */
    @Override
    public void onEnable() {
        instance = this;
        
        // Load config
        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        loadConfig();
        
        // Initialize language manager
        this.languageManager = new LanguageManager(this);
        
        // Setup database
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.validateDatabase()) {
            getLogger().critical("Failed to initialize database! Disabling RokiDragon.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register managers
        this.dragonManager = new DragonManager(this);
        this.eggManager = new DragonEggManager(this);
        this.shardManager = new DragonShardManager(this);
        this.loafManager = new DragonLoafManager(this);
        
        // Register GUI with forms instead of fake inventories
        this.dragonGUI = new FormBasedDragonGUI(this, databaseManager, eggManager, shardManager);
        
        // Set the GUI reference in the shardManager to avoid circular dependency
        this.shardManager.setDragonGUI(dragonGUI);
        
        // Register listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new EventListenerEdit(this), this);
        
        // Register the dedicated Dragon Egg Listener for improved handling
        pm.registerEvents(new DragonEggListener(this), this);
        
        // Register form response listener
        pm.registerEvents(new FormResponseListener(this), this);
        
        // Register commands programmatically
        registerCommands();

        // Register entity types - this is critical for proper entity spawning
        try {
            // Register the Dragon entity with its proper name
            Entity.registerEntity("Dragon", DragonEntity.class);
            getLogger().info("Successfully registered Dragon entity");
            
            // Register projectile entities
            Entity.registerEntity("EntityBedFireBall", EntityBedFireBall.class);
            Entity.registerEntity("EntityIceBall", EntityIceBall.class);
            Entity.registerEntity("EntityLightningBall", EntityLightningBall.class);
            
            getLogger().info("All entities registered successfully");
        } catch (Exception e) {
            getLogger().error("Failed to register entities: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Schedule egg incubation processor to run every 20 seconds (was 30)
        int incubationTaskInterval = 20 * 20; // 20 seconds
        getServer().getScheduler().scheduleRepeatingTask(this, new ProcessIncubationTask(this), incubationTaskInterval);
        getLogger().info("Scheduled incubation task to run every " + (incubationTaskInterval/20) + " seconds");

        // Schedule cooldown map cleanup task (runs every 5 minutes)
        getServer().getScheduler().scheduleRepeatingTask(this, this::cleanupCooldownMap, 5 * 60 * 20);
        
        getLogger().info("RokiDragon plugin has been enabled!");
    }

    /**
     * Registers all plugin commands programmatically with their respective executors.
     */
    private void registerCommands() {
        // Dragon Command
        PluginCommand<DragonPlugin> dragonCmd = new PluginCommand<>(
            "dragon", this // Command name and owner plugin
        );
        dragonCmd.setDescription("Main command for dragon management");
        dragonCmd.setUsage("/dragon [help|info|summon|despawn|name|recipes|lost]");
        dragonCmd.setAliases(new String[]{"d"});
        dragonCmd.setPermission("rokidragon.use");
        dragonCmd.setExecutor(new DragonCommandExecutor(this, databaseManager, eggManager, shardManager, dragonGUI));
        this.getServer().getCommandMap().register("rokidragon", dragonCmd); // Register with fallback prefix

        // Dragon Logs Command (Admin only)
        PluginCommand<DragonPlugin> logsCmd = new PluginCommand<>(
            "dragonlogs", this
        );
        logsCmd.setDescription("View lost dragon egg reports");
        logsCmd.setUsage("/dragonlogs <list|player> [playerName]");
        logsCmd.setAliases(new String[]{"dlogs"});
        logsCmd.setPermission("rokidragon.admin");
        logsCmd.setExecutor(new DragonLogsCommandExecutor(this, databaseManager));
        this.getServer().getCommandMap().register("rokidragon", logsCmd);
        
        getLogger().info("Registered RokiDragon commands.");
    }

    /**
     * Called when the plugin is disabled. Despawns all active dragons.
     */
    @Override
    public void onDisable() {
        // Despawn all active dragons when the plugin is disabled
        for (Map.Entry<UUID, DragonEntity> entry : activeDragons.entrySet()) {
            Player owner = getServer().getPlayer(entry.getKey()).orElse(null);
            if (owner != null) {
                despawnDragon(owner);
            }
        }
        activeDragons.clear();
        
        getLogger().info("RokiDragon plugin has been disabled!");
    }

    /**
     * Checks if world restrictions are enabled in the configuration
     * 
     * @return True if world restrictions are enabled
     */
    public boolean areWorldRestrictionsEnabled() {
        return config.getBoolean("world_restrictions.enabled", true);
    }
    
    /**
     * Gets a list of allowed world names from the configuration
     * 
     * @return List of allowed world names
     */
    public java.util.List<String> getAllowedWorlds() {
        return config.getStringList("world_restrictions.allowed_worlds");
    }
    
    /**
     * Checks if a world is allowed for dragon commands
     * 
     * @param worldName The name of the world to check
     * @return True if the world is allowed or restrictions are disabled
     */
    public boolean isWorldAllowed(String worldName) {
        // If restrictions are disabled, all worlds are allowed
        if (!areWorldRestrictionsEnabled()) {
            return true;
        }
        
        // Check if the world name is in the allowed list
        return getAllowedWorlds().contains(worldName);
    }
    
    /**
     * Checks if a player is in an allowed world and sends a message if not
     * 
     * @param player The player to check
     * @return True if the player is in an allowed world or restrictions are disabled
     */
    public boolean isPlayerInAllowedWorld(Player player) {
        // If restrictions are disabled, all worlds are allowed
        if (!areWorldRestrictionsEnabled()) {
            return true;
        }
        
        // Check if the player's world is allowed
        String worldName = player.getLevel().getName();
        boolean allowed = isWorldAllowed(worldName);
        
        // If not allowed, send a message to the player
        if (!allowed) {
            java.util.List<String> allowedWorlds = getAllowedWorlds();
            if (allowedWorlds.isEmpty()) {
                player.sendMessage(TextFormat.RED + getLanguageString("messages.errors.worldRestricted"));
            } else {
                String worldsList = String.join("§e, §e", allowedWorlds);
                player.sendMessage(TextFormat.RED + getLanguageString("messages.errors.worldRestrictedWithList", worldsList));
            }
        }
        
        return allowed;
    }

    /**
     * Checks if a player is on cooldown.
     *
     * @param player The player to check
     * @return true if the player is on cooldown, false otherwise
     */
    public boolean isOnCooldown(String player) {
        if (!cooldownMap.containsKey(player)) {
            return false;
        }

        long expiry = cooldownMap.get(player);
        boolean expired = System.currentTimeMillis() > expiry;

        if (expired) {
            cooldownMap.remove(player);
        }

        return !expired;
    }

    /**
     * Gets the remaining cooldown time for a player in seconds.
     *
     * @param player The name of the player.
     * @return The remaining cooldown time in seconds, or 0 if not on cooldown.
     */
    public long getCooldownTime(String player) {
        if (!cooldownMap.containsKey(player)) {
            return 0;
        }

        long expiry = cooldownMap.get(player);
        long remaining = expiry - System.currentTimeMillis();

        return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) + 1 : 0;
    }

    /**
     * Sets a cooldown for a specific player.
     * If seconds is less than or equal to 0, any existing cooldown for the player is removed.
     *
     * @param player  The name of the player to set the cooldown for.
     * @param seconds The duration of the cooldown in seconds.
     */
    public void setCooldown(String player, int seconds) {
        if (seconds <= 0) {
            cooldownMap.remove(player);
            return;
        }

        long expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        cooldownMap.put(player, expiry);
    }

    /**
     * Despawns the dragon currently associated with the given player, if any.
     * Explicitly dismounts passengers before removing the entity.
     *
     * @param player The player whose dragon should be despawned.
     * @return true if a dragon was found and despawned, false otherwise.
     */
    public boolean despawnDragon(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (activeDragons.containsKey(playerUUID)) {
            DragonEntity dragon = activeDragons.get(playerUUID);
            
            // Dismount all passengers *before* closing the entity
            if (dragon != null) { // Check if dragon reference is valid
                getLogger().info("Dismounting passengers before despawning dragon for " + player.getName());
                dragon.dismountAllPassengers(); // Use the existing method in DragonEntity
                
                // Now close the dragon entity
                dragon.close();
                getLogger().info("Closed dragon entity for " + player.getName());
            } else {
                getLogger().warning("Found player UUID in activeDragons map, but DragonEntity was null for " + player.getName());
            }
            
            // Remove from active dragons list regardless of null check outcome (to clean up the map)
            activeDragons.remove(playerUUID);
            getLogger().info("Removed dragon entry from active map for player " + player.getName());
            
            return true; // Return true as we found an entry in the map
        }
        
        getLogger().info("No active dragon found to despawn for player " + player.getName());
        return false;
    }

    /**
     * Checks if the player already has a dragon active.
     * 
     * @param player The player to check
     * @return true if the player has an active dragon, false otherwise
     */
    public boolean hasActiveDragon(Player player) {
        return activeDragons.containsKey(player.getUniqueId());
    }

    /**
     * Registers a spawned dragon in the active dragons map
     * 
     * @param player The owner of the dragon
     * @param dragon The dragon entity
     */
    public void registerActiveDragon(Player player, DragonEntity dragon) {
        UUID playerUUID = player.getUniqueId();
        
        // If player already has a dragon, despawn it first
        if (activeDragons.containsKey(playerUUID)) {
            DragonEntity existingDragon = activeDragons.get(playerUUID);
            existingDragon.close();
            getLogger().info("Despawned existing dragon for player " + player.getName() + " before spawning a new one");
        }
        
        // Register the new dragon
        activeDragons.put(playerUUID, dragon);
        getLogger().info("Registered active dragon for player " + player.getName());
    }

    /**
     * Called when a player's dragon dies. Removes the dragon from the active map,
     * and notifies the owner that the dragon is lost forever.
     *
     * @param owner The player whose dragon died.
     */
    public void onDragonDeath(Player owner) {
        if (owner == null) return;
        
        // Remove the dragon from active dragons
        activeDragons.remove(owner.getUniqueId());
        
        // Send message to owner using language keys
        owner.sendMessage(TextFormat.RED + getLanguageString("messages.death.defeated"));
        owner.sendMessage(TextFormat.YELLOW + getLanguageString("messages.death.lostForever"));
    }

    // Getters
    /**
     * Gets the singleton instance of the DragonPlugin.
     * @return The plugin instance.
     */
    public static DragonPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the database manager instance.
     * @return The DatabaseManager.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the loaded language configuration.
     * @return The language Config object.
     * @deprecated Use {@link #getLanguageManager()} and {@link LanguageManager#get(String, Object...)} instead.
     */
    @Deprecated
    public Config getLanguageConfig() {
        return config;
    }

    /**
     * Gets the map of currently active (spawned) dragons, keyed by owner UUID.
     * @return A map of active DragonEntity instances.
     */
    public Map<UUID, DragonEntity> getActiveDragons() {
        return activeDragons;
    }

    /**
     * Gets the dragon manager instance.
     * @return The DragonManager.
     */
    public DragonManager getDragonManager() {
        return dragonManager;
    }

    /**
     * Gets the dragon egg manager instance.
     * @return The DragonEggManager.
     */
    public DragonEggManager getEggManager() {
        return eggManager;
    }

    /**
     * Gets the dragon shard manager instance.
     * @return The DragonShardManager.
     */
    public DragonShardManager getShardManager() {
        return shardManager;
    }

    /**
     * Gets the dragon loaf manager instance.
     * @return The DragonLoafManager.
     */
    public DragonLoafManager getLoafManager() {
        return loafManager;
    }

    /**
     * Gets the Dragon Management GUI instance.
     * @return The FormBasedDragonGUI.
     */
    public FormBasedDragonGUI getDragonGUI() {
        return dragonGUI;
    }

    public String getLanguageString(String key, Object... params) {
        String message = languageManager.get(key, params);
        return message;
    }

    /**
     * Gets the language manager instance.
     * @return The LanguageManager.
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Loads the main config.yml and saves default language files.
     */
    private void loadConfig() {
        // Load main configuration
        this.saveDefaultConfig();
        this.config = this.getConfig();
        
        // Load language files
        saveResource("lang/en_US.yml", false);
        saveResource("lang/es_ES.yml", false);
        
        getLogger().info("Configuration loaded");
    }
    /**
     * Periodically removes expired entries from the cooldown map to prevent memory leaks.
     * Scheduled to run automatically.
     */
    private void cleanupCooldownMap() {
        long currentTime = System.currentTimeMillis();
        cooldownMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        getLogger().debug("Cleaned up expired cooldown entries.");
    }

    /**
     * Check and resume incubation for a player's eggs
     * @param player The player to check
     */
    public void checkAndResumeIncubation(Player player) {
        getLogger().debug("Checking for incubating eggs for player " + player.getName());
        // Find all eggs in the player's inventory
        for (Item item : player.getInventory().getContents().values()) {
            if (item.getId() == Item.DRAGON_EGG && item.hasCompoundTag()) {
                CompoundTag tag = item.getNamedTag();
                String eggId = null;
                
                // Check for different tag names that might have the egg ID
                if (tag.contains("eggId")) {
                    eggId = tag.getString("eggId");
                } else if (tag.contains("dragon_egg_id")) {
                    eggId = tag.getString("dragon_egg_id");
                } else if (tag.contains("DragonUUID")) {
                    eggId = tag.getString("DragonUUID");
                }
                
                if (eggId != null && !eggId.isEmpty()) {
                    // Check if this egg is marked as incubating in the database
                    if (databaseManager.isEggIncubating(eggId)) {
                        // Egg is marked as incubating, update the lore
                        eggManager.updateEggLore(item, eggId);
                        
                        // Update incubation start time to current time
                        databaseManager.updateIncubationStartTime(eggId, System.currentTimeMillis() / 1000);
                        
                        getLogger().info("Resumed incubation for egg " + eggId + " for player " + player.getName());
                        player.sendMessage(TextFormat.AQUA + getLanguageString("messages.incubationResumed"));
                    }
                }
            }
        }
    }

} // End of DragonPlugin class
