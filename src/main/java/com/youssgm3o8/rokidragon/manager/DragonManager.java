package com.youssgm3o8.rokidragon.manager;

import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;

/**
 * Manages the creation, spawning, and potentially tracking (though currently handled in DragonPlugin)
 * of DragonEntity instances.
 */
public class DragonManager {
    private final DragonPlugin plugin;
    
    /**
     * Constructs a new DragonManager.
     * @param plugin The main plugin instance, used for accessing config, database, etc.
     */
    public DragonManager(DragonPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Spawns a new DragonEntity with the specified attributes at the given position.
     * This method is typically called when hatching an egg or potentially through admin commands.
     *
     * @param dragonType The type of dragon (e.g., "Fire Dragon").
     * @param dragonName The custom name for the dragon.
     * @param dragonUUID The unique ID for this dragon instance (usually from the egg).
     * @param level      The level in which to spawn the dragon.
     * @param position   The position at which to spawn the dragon.
     * @param player     The player who will own this dragon.
     * @return The spawned DragonEntity instance, or null if spawning failed (e.g., chunk not loaded).
     */
    public com.youssgm3o8.rokidragon.dragon.DragonEntity spawnDragon(String dragonType, String dragonName, UUID dragonUUID, Level level, Position position, Player player) {
        // Get dragon data
        String playerUUID = player.getUniqueId().toString();
        String eggId = dragonUUID.toString();
        
        // Debug log start
        plugin.getLogger().info("Attempting to spawn dragon: " + dragonName + " (Type: " + dragonType + ") for player: " + player.getName());
        
        // Check if world is allowed
        if (plugin.areWorldRestrictionsEnabled() && !plugin.isWorldAllowed(level.getName())) {
            plugin.getLogger().info("Failed to spawn dragon: World " + level.getName() + " is not allowed");
            player.sendMessage(TextFormat.RED + "Dragons cannot be summoned in this world.");
            return null;
        }
        
        // Create a spawn location - 2 blocks in front of the player
        double yaw = player.getYaw();
        double rads = Math.toRadians(yaw);
        double x = player.getX() - Math.sin(rads) * 2;
        double y = player.getY();
        double z = player.getZ() + Math.cos(rads) * 2;
        
        Location spawnLocation = new Location(x, y, z, yaw, 0, player.getLevel());
        plugin.getLogger().info("Spawn location: " + spawnLocation.getX() + ", " + spawnLocation.getY() + ", " + spawnLocation.getZ() + " in " + spawnLocation.getLevel().getName());
        
        // Get the chunk to spawn in
        FullChunk chunk = spawnLocation.getLevel().getChunk(
                (int) spawnLocation.getX() >> 4, 
                (int) spawnLocation.getZ() >> 4
        );
        
        if (chunk == null) {
            plugin.getLogger().info("Failed to spawn dragon: Chunk is null");
            player.sendMessage(TextFormat.RED + "Could not spawn your dragon. Try again in a loaded chunk.");
            return null;
        }
        
        // Check if chunk is loaded
        if (!chunk.isGenerated() || !chunk.isPopulated()) {
            plugin.getLogger().info("Failed to spawn dragon: Chunk is not generated or populated");
            player.sendMessage(TextFormat.RED + "Could not spawn your dragon. Try again in a fully loaded area.");
            return null;
        }
        
        // Prepare entity NBT data
        CompoundTag nbt = Entity.getDefaultNBT(spawnLocation)
                .putString("DragonType", dragonType)
                .putString("DragonName", dragonName)
                .putString("EggId", eggId)
                .putString("OwnerUUID", playerUUID)
                .putBoolean("IsAdminDragon", player.hasPermission("rokidragon.admin")); // Add admin tag based on permission (adjust if needed)
        
        plugin.getLogger().info("Attempting to create dragon entity with NBT data");
        
        // Create and spawn the dragon entity
        // Create the dragon entity using Nukkit's method
        Entity entity = null;
        try {
            entity = Entity.createEntity("Dragon", chunk, nbt);
            plugin.getLogger().info("Entity created: " + (entity != null ? entity.getClass().getName() : "null"));
        } catch (Exception e) {
            plugin.getLogger().error("Exception creating dragon entity: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(TextFormat.RED + "Failed to spawn your dragon due to an error.");
            return null;
        }
        
        com.youssgm3o8.rokidragon.dragon.DragonEntity dragon = null;
        
        // Check if the created entity is indeed our DragonEntity
        if (entity instanceof com.youssgm3o8.rokidragon.dragon.DragonEntity) {
            dragon = (com.youssgm3o8.rokidragon.dragon.DragonEntity) entity;
            
            // Initialize the dragon with the plugin instance and set owner
            dragon.initialize(plugin);
            dragon.setOwner(player);
            dragon.setDragonId(eggId); // Set the dragon ID
            
            // Spawn the entity in the world
            dragon.spawnToAll();
            
            player.sendMessage(TextFormat.GREEN + "Your " + TextFormat.YELLOW + dragonName + 
                    TextFormat.GREEN + " has been summoned!");
            
            plugin.getLogger().info("Dragon successfully spawned!");
            return dragon;
        } else {
            plugin.getLogger().info("Failed to spawn dragon: Entity is not a DragonEntity, it is: " + 
                (entity != null ? entity.getClass().getName() : "null"));
            player.sendMessage(TextFormat.RED + "Failed to spawn your dragon.");
            return null;
        }
    }
    
    /**
     * Spawns the active dragon associated with a player, retrieving its data from the database.
     * Calculates a suitable spawn location in front of the player.
     *
     * @param player The player whose dragon should be spawned.
     * @return The spawned DragonEntity instance, or null if the player has no dragon, the egg isn't hatched,
     *         or spawning failed.
     */
    public com.youssgm3o8.rokidragon.dragon.DragonEntity spawnDragon(Player player) {
        // Get dragon data
        String playerUUID = player.getUniqueId().toString();
        String eggId = plugin.getDatabaseManager().getDragonEggId(playerUUID);
        
        if (eggId == null) {
            plugin.getLogger().info("Player " + player.getName() + " has no dragon egg");
            player.sendMessage(TextFormat.RED + "You don't have a dragon egg!");
            return null;
        }
        
        String dragonType = plugin.getDatabaseManager().getDragonType(eggId);
        String dragonName = plugin.getDatabaseManager().getDragonName(eggId);
        
        plugin.getLogger().info("Spawning " + dragonName + " (" + dragonType + ") for " + player.getName());
        
        // Use the existing method to spawn the dragon
        return spawnDragon(dragonType, dragonName, UUID.fromString(eggId), player.getLevel(), player.getPosition(), player);
    }
}