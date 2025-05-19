package com.youssgm3o8.rokidragon;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.event.Listener;
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
import com.youssgm3o8.rokidragon.entities.EntityWaterBall;
import com.youssgm3o8.rokidragon.entities.EntityEarthBall;
import com.youssgm3o8.rokidragon.listeners.EventListenerEdit;
import com.youssgm3o8.rokidragon.listeners.DragonEggListener;
import com.youssgm3o8.rokidragon.listeners.FormResponseListener;
import com.youssgm3o8.rokidragon.util.ResourcePackManager;
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
        // Register all dragon entity type definitions
        EntityManager.get().registerDefinition(DragonEntity.getDefinitionForType("Fire Dragon"));
        EntityManager.get().registerDefinition(DragonEntity.getDefinitionForType("Ice Dragon"));
        EntityManager.get().registerDefinition(DragonEntity.getDefinitionForType("Lightning Dragon"));
        EntityManager.get().registerDefinition(DragonEntity.getDefinitionForType("Water Dragon"));
        EntityManager.get().registerDefinition(DragonEntity.getDefinitionForType("Earth Dragon"));
        
        getLogger().info("Registered all dragon entity definitions");
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
        
        // Export resource pack to server's resource_packs folder
        getLogger().info("Exporting dragon resource pack to server...");
        ResourcePackManager resourcePackManager = new ResourcePackManager(this);
        boolean resourcePackExported = resourcePackManager.exportResourcePack();
        
        if (resourcePackExported) {
            getLogger().info("Resource pack successfully exported and registered with the server!");
        } else {
            getLogger().warning("Failed to export resource pack. Dragons may not display correctly!");
        }
        
        // Initialize language manager
        this.languageManager = new LanguageManager(this);
        
        // Setup database
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.validateDatabase()) {
            getLogger().critical("Failed to initialize database! Disabling RokiDragon.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Set default stats for DragonEntity from config
        int maxHealth = config.getInt("dragon_stats.max_health", 100);
        double baseDamage = config.getDouble("dragon_stats.base_damage", 15.0);
        double damageReduction = config.getDouble("dragon_stats.damage_reduction", 0.25);
        DragonEntity.setDefaultStats(maxHealth, baseDamage, damageReduction);
        getLogger().info("Loaded dragon stats from config: Health=" + maxHealth + 
                         ", Damage=" + baseDamage + ", Reduction=" + damageReduction);
        
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
            // Register the specific dragon types with proper identifiers to match resource pack
            // The format must be "roki:type_dragon" to match the resource pack identifiers
            Entity.registerEntity("roki:fire_dragon", DragonEntity.class);
            Entity.registerEntity("roki:ice_dragon", DragonEntity.class);
            Entity.registerEntity("roki:lightning_dragon", DragonEntity.class);
            Entity.registerEntity("roki:water_dragon", DragonEntity.class);
            Entity.registerEntity("roki:earth_dragon", DragonEntity.class);
            getLogger().info("Successfully registered RokiDragon entity types");
            
            // Register projectile entities
            Entity.registerEntity("EntityBedFireBall", EntityBedFireBall.class);
            Entity.registerEntity("EntityIceBall", EntityIceBall.class);
            Entity.registerEntity("EntityLightningBall", EntityLightningBall.class);
            Entity.registerEntity("EntityWaterBall", EntityWaterBall.class);
            Entity.registerEntity("EntityEarthBall", EntityEarthBall.class);
            
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
        
        // Schedule a task to check database connection every minute
        this.getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            // Check if database connection is valid and reconnect if needed
            if (this.databaseManager != null) {
                this.databaseManager.checkConnection();
                getLogger().debug("Database connection check completed");
            }
        }, 20 * 60); // 60 seconds (20 ticks per second)
        
        // Save default config
        this.saveDefaultConfig();
        
        // Ensure the config has all required default values
        initializeDefaultConfig();
        
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
                
                // Save the dragon's health to the database
                String eggId = dragon.getDragonId();
                if (eggId != null) {
                    float currentHealth = dragon.getPluginHealth();
                    getLogger().info("Saving dragon health for " + player.getName() + ": " + currentHealth);
                    databaseManager.saveDragonHealth(eggId, currentHealth);
                }
                
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
     * Gets the map of currently active dragons
     * @return Map of player UUID to DragonEntity
     */
    public Map<UUID, DragonEntity> getActiveDragons() {
        return this.activeDragons;
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
        
        // Check if player is valid and has an inventory
        if (player == null || !player.isOnline()) {
            getLogger().warning("Attempted to check incubation for null or offline player");
            return;
        }
        
        // Check if inventory is available
        if (player.getInventory() == null) {
            getLogger().warning("Player " + player.getName() + " has null inventory");
            return;
        }
        
        // Get inventory contents safely
        Map<Integer, Item> contents = player.getInventory().getContents();
        if (contents == null) {
            getLogger().warning("Player " + player.getName() + " has null inventory contents");
            return;
        }
        
        // Find all eggs in the player's inventory
        for (Item item : contents.values()) {
            if (item == null) continue;
            
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
                        player.sendMessage(TextFormat.AQUA + getLanguageString("messages.success.incubationResumed"));
                    }
                }
            }
        }
    }

    /**
     * Initialize default config values for all settings used in the plugin
     */
    private void initializeDefaultConfig() {
        Config config = this.getConfig();
        
        // Dragon stats
        setDefaultIfMissing(config, "dragon_stats.max_health", 100);
        setDefaultIfMissing(config, "dragon_stats.base_damage", 15.0);
        setDefaultIfMissing(config, "dragon_stats.damage_reduction", 0.25);
        setDefaultIfMissing(config, "dragon_stats.move_speed", 1.8);
        setDefaultIfMissing(config, "dragon_stats.max_speed", 0.92);
        setDefaultIfMissing(config, "dragon_stats.ice_dragon_fire_vulnerability", 1.5);
        
        // Flight physics constants
        setDefaultIfMissing(config, "flight_physics.acceleration_rate", 0.08);
        setDefaultIfMissing(config, "flight_physics.drag_coefficient", 0.025);
        setDefaultIfMissing(config, "flight_physics.gravity_accel", 0.04);
        setDefaultIfMissing(config, "flight_physics.dive_accel_multiplier", 1.2);
        setDefaultIfMissing(config, "flight_physics.lift_coefficient", 0.05);
        setDefaultIfMissing(config, "flight_physics.max_vertical_speed_up", 1.0);
        setDefaultIfMissing(config, "flight_physics.max_vertical_speed_down", -2.0);
        setDefaultIfMissing(config, "flight_physics.min_lift_speed", 0.2);
        setDefaultIfMissing(config, "flight_physics.flat_flight_speed_multiplier", 1.5);
        setDefaultIfMissing(config, "flight_physics.pitch_threshold_down", 20);
        setDefaultIfMissing(config, "flight_physics.pitch_threshold_flat", 15);
        setDefaultIfMissing(config, "flight_physics.pitch_threshold_up", -20);
        
        // Timing settings
        setDefaultIfMissing(config, "timing.cooldowns.ability_milliseconds", 500);
        setDefaultIfMissing(config, "timing.cooldowns.summon_seconds", 30);
        setDefaultIfMissing(config, "timing.despawn_delay_seconds", 300);
        
        // Dragon settings
        setDefaultIfMissing(config, "dragon_settings.dismount_resistance_duration_ticks", 100); // 5 seconds
        setDefaultIfMissing(config, "dragon_settings.dismount_resistance_amplifier", 254); // Maximum resistance
        setDefaultIfMissing(config, "dragon_settings.min_name_length", 3);
        setDefaultIfMissing(config, "dragon_settings.max_name_length", 16);
        
        // Visual settings
        setDefaultIfMissing(config, "visual.particle_effect_interval_ticks", 5);
        setDefaultIfMissing(config, "visual.death_particle_count", 20);
        
        // Save any changes
        this.saveConfig();
    }
    
    /**
     * Set a default value in the config if the key doesn't exist
     */
    private void setDefaultIfMissing(Config config, String path, Object defaultValue) {
        if (!config.exists(path)) {
            config.set(path, defaultValue);
        }
    }

} // End of DragonPlugin class
