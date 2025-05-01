package com.youssgm3o8.rokidragon.task;

import java.util.List;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.IncubatingEgg;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.block.Block;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.TextFormat;

/**
 * Task that processes incubating eggs to update their progress
 */
public class ProcessIncubationTask extends Task {
    private final DragonPlugin plugin;
    
    public ProcessIncubationTask(DragonPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onRun(int currentTick) {
        try {
            // First check for players who have just connected and have incubating eggs
            for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                checkPlayerInventoryForIncubatingEggs(player);
            }
            
            // Get all eggs that are currently incubating
            List<IncubatingEgg> incubatingEggs = plugin.getDatabaseManager().getAllIncubatingEggs();
            
            if (incubatingEggs.isEmpty()) {
                return; // No eggs to process
            }
            
            // Get required incubation time from config
            int requiredSeconds = plugin.getConfig().getInt("timing.eggs.incubation_time_seconds", 3600); // Default: 1 hour
            long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
            
            plugin.getLogger().info("Processing " + incubatingEggs.size() + " incubating eggs");
            
            for (IncubatingEgg egg : incubatingEggs) {
                try {
                    processEgg(egg, currentTime, requiredSeconds);
                } catch (Exception e) {
                    plugin.getLogger().error("Error processing egg " + egg.getEggId() + ": " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error in incubation task: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    /**
     * Check a player's inventory for any eggs that should be incubating
     * This helps resume incubation after server restarts or player reconnects
     */
    private void checkPlayerInventoryForIncubatingEggs(Player player) {
        plugin.getLogger().info("Checking " + player.getName() + "'s inventory for incubating eggs");
        boolean foundIncubatingEggs = false;
        
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
                    plugin.getLogger().info("Found dragon egg with ID: " + eggId + " in " + player.getName() + "'s inventory");
                    
                    // Check if this egg is marked as incubating in the database
                    if (plugin.getDatabaseManager().isEggIncubating(eggId)) {
                        foundIncubatingEggs = true;
                        // Ensure the NBT data is consistent
                        tag.putBoolean("incubating", true);
                        tag.putString("eggId", eggId);
                        tag.putString("dragon_egg_id", eggId);
                        item.setNamedTag(tag);
                        
                        // Egg is marked as incubating, update the lore
                        plugin.getEggManager().updateEggLore(item, eggId);
                        
                        // Only update start time if it's not already set
                        long startTime = plugin.getDatabaseManager().getEggIncubationStartTime(eggId);
                        if (startTime == 0) {
                            long currentTime = System.currentTimeMillis() / 1000;
                            plugin.getDatabaseManager().updateIncubationStartTime(eggId, currentTime);
                            plugin.getLogger().info("Initialized incubation start time for egg " + eggId + " to " + currentTime);
                        } else {
                            plugin.getLogger().info("Egg " + eggId + " already has start time: " + startTime + ", maintaining it");
                        }
                        
                        // Make sure the player inventory sees the updated item
                        int slot = player.getInventory().first(item);
                        if (slot >= 0) {
                            player.getInventory().setItem(slot, item);
                            player.getInventory().sendContents(player);
                        }
                        
                        plugin.getLogger().info("Resumed incubation for egg " + eggId + " for player " + player.getName());
                    }
                }
            }
        }
        
        if (!foundIncubatingEggs) {
            plugin.getLogger().info("No incubating eggs found in " + player.getName() + "'s inventory");
        }
    }
    
    /**
     * Process a single incubating egg
     */
    private void processEgg(IncubatingEgg egg, long currentTime, int requiredSeconds) {
        String playerUuid = egg.getPlayerUUID();
        String eggId = egg.getEggId();
        
        // Log full egg details for debugging
        plugin.getLogger().info("Processing egg " + eggId + " - Current seconds: " + egg.getIncubationSeconds() + 
                               ", Start time: " + egg.getIncubationStartTime() + ", Current time: " + currentTime);
        
        // Check if player is online
        Player player = null;
        try {
            UUID playerUUID = UUID.fromString(playerUuid);
            if (plugin.getServer().getPlayer(playerUUID).isPresent()) {
                player = plugin.getServer().getPlayer(playerUUID).get();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid UUID for egg " + eggId + ": " + playerUuid);
            return;
        }
        
        if (player == null || !player.isOnline()) {
            plugin.getLogger().info("Player is offline, skipping egg " + eggId);
            return; // Skip if player is offline
        }
        
        plugin.getLogger().info("Player " + player.getName() + " is online, continuing with egg " + eggId);
        
        // Find the egg in player's inventory
        Item eggItem = findEggInInventory(player, eggId);
        if (eggItem == null) {
            // Egg not found in inventory, stop incubation
            plugin.getLogger().info("Egg " + eggId + " not found in " + player.getName() + "'s inventory, stopping incubation");
            plugin.getDatabaseManager().setEggIncubating(eggId, false);
            return;
        }
        
        // Calculate time passed since last update
        long lastCheckTime = egg.getIncubationStartTime();
        int additionalSeconds = (int) (currentTime - lastCheckTime);
        
        plugin.getLogger().info("Time since last check: " + additionalSeconds + " seconds for egg " + eggId);
        
        if (additionalSeconds <= 0) {
            plugin.getLogger().info("No time has passed since last check for egg " + eggId);
            return; // No time has passed
        }
        
        // Update incubation progress
        int newTotalSeconds = egg.getIncubationSeconds() + additionalSeconds;
        plugin.getLogger().info("Updating incubation progress for egg " + eggId + 
                               " from " + egg.getIncubationSeconds() + " to " + newTotalSeconds + " seconds");
        
        // Update in database FIRST so it's not lost
        plugin.getDatabaseManager().updateIncubationProgress(eggId, newTotalSeconds);
        
        // Only AFTER recording progress, update the start time to current time
        plugin.getDatabaseManager().updateIncubationStartTime(eggId, currentTime);
        plugin.getLogger().info("Updated start time for next check to " + currentTime + " for egg " + eggId);
        
        // Calculate progress percentage
        int progressPercent = (int) (((double) newTotalSeconds / requiredSeconds) * 100);
        progressPercent = Math.min(100, progressPercent); // Cap at 100%
        
        plugin.getLogger().info("Egg " + eggId + " progress: " + progressPercent + "% (" + newTotalSeconds + "/" + requiredSeconds + " seconds)");
        
        // Notify player every 10% progress (only at exact multiples of 10)
        int oldPercentage = (int)(((double)(newTotalSeconds - additionalSeconds) / requiredSeconds) * 100);
        int newPercentage = progressPercent;
        
        // Check if we've crossed a 10% boundary
        if ((oldPercentage / 10) < (newPercentage / 10)) {
            plugin.getLogger().info("Sending progress notification to player: " + progressPercent + "%");
            player.sendMessage(TextFormat.AQUA + plugin.getLanguageString("messages.success.incubationProgress", progressPercent));
        }
        
        // Check if incubation is complete
        if (newTotalSeconds >= requiredSeconds) {
            plugin.getLogger().info("Egg " + eggId + " has completed incubation, hatching now");
            handleEggHatching(player, egg);
        } else {
            // Update egg lore to show progress
            updateEggLore(player, eggItem, eggId, progressPercent);
        }
    }
    
    /**
     * Find a specific egg in a player's inventory
     */
    private Item findEggInInventory(Player player, String eggId) {
        for (Item item : player.getInventory().getContents().values()) {
            if (item.getId() == Item.DRAGON_EGG && item.hasCompoundTag()) {
                CompoundTag tag = item.getNamedTag();
                
                // Check all possible tag names for egg ID
                if ((tag.contains("eggId") && tag.getString("eggId").equals(eggId)) ||
                    (tag.contains("dragon_egg_id") && tag.getString("dragon_egg_id").equals(eggId)) ||
                    (tag.contains("DragonUUID") && tag.getString("DragonUUID").equals(eggId))) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * Update the egg's lore to show current progress
     */
    private void updateEggLore(Player player, Item eggItem, String eggId, int progressPercent) {
        // First update the NBT tag with the progress percentage
        CompoundTag tag = eggItem.getNamedTag();
        if (tag != null) {
            // Update all possible tag names for consistency
            tag.putInt("IncubationProgress", progressPercent);
            tag.putString("eggId", eggId); // Ensure the eggId is set
            tag.putString("dragon_egg_id", eggId); // Set alternate tag as well
            tag.putBoolean("incubating", true); // Mark as incubating
            
            // Also mark that this is a dragon egg
            tag.putBoolean("IsDragonEgg", true);
            
            eggItem.setNamedTag(tag);
            
            plugin.getLogger().info("Updated egg NBT data with progress: " + progressPercent + "% for egg " + eggId);
            
            // Now use the DragonEggManager to update the lore properly
            plugin.getEggManager().updateEggLore(eggItem, eggId);
            
            // Update the item in the player's inventory
            boolean updated = false;
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                Item item = player.getInventory().getItem(slot);
                
                boolean isTargetEgg = false;
                if (item.getId() == Item.DRAGON_EGG && item.hasCompoundTag()) {
                    CompoundTag itemTag = item.getNamedTag();
                    
                    // Check all possible tag names for egg ID
                    isTargetEgg = (itemTag.contains("eggId") && itemTag.getString("eggId").equals(eggId)) ||
                                 (itemTag.contains("dragon_egg_id") && itemTag.getString("dragon_egg_id").equals(eggId)) ||
                                 (itemTag.contains("DragonUUID") && itemTag.getString("DragonUUID").equals(eggId));
                }
                
                if (isTargetEgg) {
                    player.getInventory().setItem(slot, eggItem);
                    player.getInventory().sendContents(player); // Make sure client updates
                    plugin.getLogger().info("Updated egg in player inventory at slot " + slot);
                    updated = true;
                    break;
                }
            }
            
            if (!updated) {
                plugin.getLogger().warning("Could not find matching egg in player inventory to update");
            }
        } else {
            plugin.getLogger().warning("Could not update egg lore - NBT tag is null");
        }
    }
    
    /**
     * Handle the hatching of an egg
     * 
     * @param player The player who owns the egg
     * @param egg The egg to hatch
     */
    private void handleEggHatching(Player player, IncubatingEgg egg) {
        String eggId = egg.getEggId();
        
        // Determine dragon type based on environment
        String dragonType = determineDragonType(player);
        String dragonName = egg.getDragonName();
        
        // Mark egg as hatched in database
        plugin.getDatabaseManager().setEggHatched(eggId, true);
        
        // Store updated dragon type
        plugin.getDatabaseManager().setDragonType(eggId, dragonType);
        
        // Stop incubation
        plugin.getDatabaseManager().setEggIncubating(eggId, false);
        
        // Send message to player
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggHatched"));
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggHatchedType", dragonType));
        
        // Update egg in player's inventory
        updateEggInInventory(player, eggId, dragonType, dragonName);
        
        // Register the hatched dragon in the system
        String playerUUID = player.getUniqueId().toString();
        boolean registered = plugin.getDatabaseManager().registerDragon(playerUUID, eggId, dragonType, dragonName);
        
        if (registered) {
            plugin.getLogger().info("Successfully registered hatched dragon " + dragonName + " (" + dragonType + ") for player " + player.getName());
            
            // Give initial dragon shards based on dragon type
            plugin.getShardManager().giveInitialShards(dragonType, player);
        } else {
            plugin.getLogger().warning("Failed to register hatched dragon for player " + player.getName());
        }
    }
    
    /**
     * Determines the type of dragon based on the player's environment
     * 
     * @param player The player who owns the egg
     * @return The determined dragon type
     */
    private String determineDragonType(Player player) {
        Level level = player.getLevel();
        
        // Check if it's night time (for Lightning Dragon)
        // Minecraft time is 0-24000, with night being roughly 13000-23000
        if (level.getTime() >= 13000 && level.getTime() <= 23000) {
            plugin.getLogger().info("It's night time, setting dragon type to Lightning Dragon");
            return "Lightning Dragon";
        }
        
        // Check if player is in the Nether (for Fire Dragon)
        if (level.getDimension() == Level.DIMENSION_NETHER) {
            plugin.getLogger().info("Player is in the Nether, setting dragon type to Fire Dragon");
            return "Fire Dragon";
        }
        
        // Get the player's position
        Position position = player.getPosition();
        int x = position.getFloorX();
        int y = position.getFloorY();
        int z = position.getFloorZ();
        
        // Check if player is underwater (for Ice Dragon)
        Block block = level.getBlock(position);
        if (block.getId() == Block.WATER || block.getId() == Block.STILL_WATER) {
            plugin.getLogger().info("Player is underwater, setting dragon type to Ice Dragon");
            return "Ice Dragon";
        }
        
        // Check biome (for Ice Dragon or Fire Dragon)
        // Instead of directly getting biome, check for blocks characteristic of certain biomes
        boolean isSnowBiome = false;
        boolean isDesertBiome = false;
        
        // Check for snow/ice blocks nearby
        int checkRadius = 3;
        for(int xOffset = -checkRadius; xOffset <= checkRadius; xOffset++) {
            for(int zOffset = -checkRadius; zOffset <= checkRadius; zOffset++) {
                Block surfaceBlock = level.getBlock(x + xOffset, y, z + zOffset);
                Block aboveBlock = level.getBlock(x + xOffset, y + 1, z + zOffset);
                
                // Check for snow blocks or layers
                if(surfaceBlock.getId() == Block.SNOW_BLOCK || surfaceBlock.getId() == Block.SNOW_LAYER ||
                   surfaceBlock.getId() == Block.ICE || surfaceBlock.getId() == Block.PACKED_ICE) {
                    isSnowBiome = true;
                    break;
                }
                
                // Check for sand (desert)
                if(surfaceBlock.getId() == Block.SAND && aboveBlock.getId() == Block.AIR) {
                    isDesertBiome = true;
                }
            }
            if(isSnowBiome) break;
        }
        
        if(isSnowBiome) {
            plugin.getLogger().info("Player is in a snow/ice biome, setting dragon type to Ice Dragon");
            return "Ice Dragon";
        }
        
        if(isDesertBiome) {
            plugin.getLogger().info("Player is in a desert biome, setting dragon type to Fire Dragon");
            return "Fire Dragon";
        }
        
        // Default to Fire Dragon for daytime
        plugin.getLogger().info("Default case: daytime, setting dragon type to Fire Dragon");
        return "Fire Dragon";
    }
    
    /**
     * Update the egg item in the player's inventory to show hatched status
     * 
     * @param player The player whose inventory to check
     * @param eggId The ID of the egg to update
     */
    private void updateEggInInventory(Player player, String eggId, String dragonType, String dragonName) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            
            if (item.getId() == Item.DRAGON_EGG && item.hasCompoundTag()) {
                CompoundTag tag = item.getNamedTag();
                
                boolean isTargetEgg = false;
                UUID dragonUUID = null;
                
                if (tag.contains("eggId") && tag.getString("eggId").equals(eggId)) {
                    isTargetEgg = true;
                    try {
                        dragonUUID = UUID.fromString(tag.getString("eggId"));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid UUID format in eggId tag for egg " + eggId);
                    }
                } else if (tag.contains("dragon_egg_id") && tag.getString("dragon_egg_id").equals(eggId)) {
                    isTargetEgg = true;
                    try {
                        dragonUUID = UUID.fromString(tag.getString("dragon_egg_id"));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid UUID format in dragon_egg_id tag for egg " + eggId);
                    }
                } else if (tag.contains("DragonUUID") && tag.getString("DragonUUID").equals(eggId)) {
                    isTargetEgg = true;
                    try {
                        dragonUUID = UUID.fromString(tag.getString("DragonUUID"));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid UUID format in DragonUUID tag for egg " + eggId);
                    }
                }
                
                if (isTargetEgg && dragonUUID != null) {
                    // Create updated egg with current status
                    Item updatedEgg = plugin.getEggManager().createDragonEgg(dragonType, dragonName, dragonUUID);
                    
                    // Replace in inventory
                    player.getInventory().setItem(slot, updatedEgg);
                    player.getInventory().sendContents(player); // Make sure client updates
                    break;
                }
            }
        }
    }
} 