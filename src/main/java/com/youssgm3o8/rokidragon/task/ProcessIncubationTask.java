package com.youssgm3o8.rokidragon.task;

import java.util.List;
import java.util.UUID;
import java.util.Map;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.IncubatingEgg;
import com.youssgm3o8.rokidragon.util.DragonUtils;

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
            List<Map<String, String>> incubatingEggs = plugin.getDatabaseManager().getAllIncubatingEggs();
            
            if (incubatingEggs.isEmpty()) {
                return; // No eggs to process
            }
            
            // Get required incubation time from config
            int requiredSeconds = plugin.getConfig().getInt("timing.eggs.incubation_time_seconds", 3600); // Default: 1 hour
            long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
            
            plugin.getLogger().info("Processing " + incubatingEggs.size() + " incubating eggs");
            
            for (Map<String, String> eggData : incubatingEggs) {
                try {
                    processEgg(eggData, currentTime, requiredSeconds);
                } catch (Exception e) {
                    plugin.getLogger().error("Error processing egg " + eggData.get("eggId") + ": " + e.getMessage(), e);
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
    private void processEgg(Map<String, String> eggData, long currentTime, int requiredSeconds) {
        String playerUuid = eggData.get("playerUUID");
        String eggId = eggData.get("eggId");
        String dragonType = eggData.get("dragonType");
        long incubationStartTime = Long.parseLong(eggData.getOrDefault("incubationStartTime", "0"));
        int incubationProgress = Integer.parseInt(eggData.getOrDefault("incubationProgress", "0"));
        
        // Log full egg details for debugging
        plugin.getLogger().info("Processing egg " + eggId + " - Current seconds: " + incubationProgress + 
                               ", Start time: " + incubationStartTime + ", Current time: " + currentTime);
        
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
        long lastCheckTime = incubationStartTime;
        int additionalSeconds = (int) (currentTime - lastCheckTime);
        
        plugin.getLogger().info("Time since last check: " + additionalSeconds + " seconds for egg " + eggId);
        
        if (additionalSeconds <= 0) {
            plugin.getLogger().info("No time has passed since last check for egg " + eggId);
            return; // No time has passed
        }
        
        // Update incubation progress
        int newTotalSeconds = incubationProgress + additionalSeconds;
        plugin.getLogger().info("Updating incubation progress for egg " + eggId + 
                               " from " + incubationProgress + " to " + newTotalSeconds + " seconds");
        
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
            String message = plugin.getLanguageString("messages.success.incubationProgress", progressPercent);
            player.sendMessage(TextFormat.GREEN + message);
        }
        
        // Update the egg's lore
        updateEggLore(player, eggItem, eggId, progressPercent);
        
        // If we've reached 100%, hatch the egg
        if (progressPercent >= 100) {
            plugin.getLogger().info("Egg " + eggId + " is fully incubated! Hatching...");
            handleEggHatching(player, eggData);
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
     * Handle the egg hatching process
     * @param player The player who owns the egg
     * @param eggData The egg data from the database
     */
    private void handleEggHatching(Player player, Map<String, String> eggData) {
        String eggId = eggData.get("eggId");
        
        // Check if this is a first-time hatching by looking for hatched status in database
        boolean isAlreadyHatched = plugin.getDatabaseManager().isEggHatched(eggId);
        
        // Get any existing type from database
        String dragonType = plugin.getDatabaseManager().getDragonType(eggId);
        plugin.getLogger().info("[DB Debug] getDragonType for eggId " + eggId + " found type: " + dragonType);
        
        // For first-time hatching, determine type based on environment
        if (!isAlreadyHatched) {
            // Always determine type based on environment for first-time hatching
            String environmentType = determineDragonType(player);
            
            // If there's no existing type or we're doing first-time hatching, use the environment type
            if (dragonType == null || dragonType.isEmpty() || plugin.getConfig().getBoolean("dragon_settings.environment_determines_type", true)) {
                dragonType = environmentType;
                plugin.getLogger().info("Setting dragon type based on environment: " + dragonType);
                
                // Save the determined type to database
                plugin.getDatabaseManager().setDragonType(eggId, dragonType);
            } else {
                plugin.getLogger().info("Using existing dragon type from database: " + dragonType);
            }
        } else {
            plugin.getLogger().info("Egg was already hatched before, using existing type: " + dragonType);
        }
        
        // Get the dragon name from database or generate one
        String dragonName = plugin.getDatabaseManager().getDragonName(eggId);
        if (dragonName == null || dragonName.isEmpty()) {
            dragonName = "Dragon"; // Default name
            
            // Save it to database
            plugin.getDatabaseManager().setDragonName(eggId, dragonName);
        }
        
        // Mark egg as hatched in database
        plugin.getDatabaseManager().setEggHatched(eggId, true);
        
        // Set egg as not incubating anymore
        plugin.getDatabaseManager().setEggIncubating(eggId, false);
        
        // Update the egg in the player's inventory
        updateEggInInventory(player, eggId, dragonType, dragonName);
        
        // Show hatching effects
        showHatchingEffects(player);
        
        // Send notification
        String hatchMessage = plugin.getLanguageString("messages.success.eggHatched", dragonType);
        player.sendMessage(TextFormat.GREEN + hatchMessage);
        
        // Send instructions
        player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.success.dragonInstructions"));
        
        // Announce to server if configured
        if (plugin.getConfig().getBoolean("announcements.egg_hatched", true)) {
            String announcement = plugin.getLanguageString("messages.announcements.eggHatched", 
                player.getName(), TextFormat.GOLD + dragonType);
            plugin.getLogger().info(announcement);
            // Don't add additional TextFormat.GREEN here since it's already in the language file
            plugin.getServer().broadcastMessage(announcement);
        }
        
        plugin.getLogger().info("Player " + player.getName() + " hatched a " + dragonType + " dragon from egg " + eggId);
    }
    
    /**
     * Show particle and sound effects for egg hatching
     */
    private void showHatchingEffects(Player player) {
        // Implementation depends on Nukkit API for particles/sounds
        // Simple implementation for now
        Position pos = player.getPosition();
        Level level = player.getLevel();
        
        // Play sounds if possible
        try {
            player.getLevel().addSound(pos, cn.nukkit.level.Sound.MOB_ENDERDRAGON_GROWL);
        } catch (Exception e) {
            // Ignore if sound can't play
        }
        
        // Add effects
        for (int i = 0; i < 20; i++) {
            double offsetX = Math.random() - 0.5;
            double offsetY = Math.random() * 1.5;
            double offsetZ = Math.random() - 0.5;
            
            level.addParticle(new cn.nukkit.level.particle.GenericParticle(
                pos.clone().add(offsetX, offsetY, offsetZ), 
                cn.nukkit.level.particle.GenericParticle.TYPE_HUGE_EXPLODE_SEED));
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
        
        // Check weather conditions first
        if (level.isRaining()) {
            if (level.isThundering()) {
                plugin.getLogger().info("Thunderstorm detected, setting dragon type to Lightning Dragon");
                return "Lightning Dragon";
            } else {
                plugin.getLogger().info("Rain detected, setting dragon type to Water Dragon");
                return "Water Dragon";
            }
        }
        
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
        
        // Check if player is underwater (for Water Dragon)
        Block block = level.getBlock(position);
        if (block.getId() == Block.WATER || block.getId() == Block.STILL_WATER) {
            plugin.getLogger().info("Player is underwater, setting dragon type to Water Dragon");
            return "Water Dragon";
        }
        
        // Check for Earth Dragon criteria
        boolean isEarthBiome = false;
        
        // Check for tall grass or if surrounded by dirt/stone
        int checkRadius = 3;
        int dirtCount = 0;
        int stoneCount = 0;
        int grassCount = 0;
        
        for(int xOffset = -checkRadius; xOffset <= checkRadius; xOffset++) {
            for(int zOffset = -checkRadius; zOffset <= checkRadius; zOffset++) {
                // Check for tall grass at player position or nearby
                Block grassBlock = level.getBlock(x + xOffset, y, z + zOffset);
                Block belowBlock = level.getBlock(x + xOffset, y - 1, z + zOffset);
                
                if(grassBlock.getId() == Block.TALL_GRASS) {
                    grassCount++;
                }
                
                // Check for dirt and stone around
                if(belowBlock.getId() == Block.DIRT || belowBlock.getId() == Block.GRASS) {
                    dirtCount++;
                }
                
                if(belowBlock.getId() == Block.STONE) {
                    stoneCount++;
                }
            }
        }
        
        // If player is standing on dirt/grass with tall grass nearby, or is surrounded by stone
        if(grassCount >= 3 || dirtCount >= 5 || stoneCount >= 5) {
            plugin.getLogger().info("Player is in an earth-like area, setting dragon type to Earth Dragon");
            isEarthBiome = true;
        }
        
        if(isEarthBiome) {
            return "Earth Dragon";
        }
        
        // Check biome (for Ice Dragon or Fire Dragon)
        // Instead of directly getting biome, check for blocks characteristic of certain biomes
        boolean isSnowBiome = false;
        boolean isDesertBiome = false;
        
        // Check for snow/ice blocks nearby
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
        plugin.getLogger().info("Updating egg in inventory with ID: " + eggId + ", type: " + dragonType);
        
        // First, find the egg in the inventory
        Item eggItem = null;
        int eggSlot = -1;
        
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            
            if (item.getId() == Item.DRAGON_EGG && item.hasCompoundTag()) {
                CompoundTag tag = item.getNamedTag();
                
                // Check all possible tag names for the egg ID
                if ((tag.contains("eggId") && tag.getString("eggId").equals(eggId)) || 
                    (tag.contains("dragon_egg_id") && tag.getString("dragon_egg_id").equals(eggId)) || 
                    (tag.contains("DragonUUID") && tag.getString("DragonUUID").equals(eggId))) {
                    
                    plugin.getLogger().info("Found matching egg in slot " + slot);
                    eggItem = item;
                    eggSlot = slot;
                    break;
                }
            }
        }
        
        // If we found the egg, update it
        if (eggItem != null && eggSlot >= 0) {
            // Create updated egg with current status
            Item updatedEgg = plugin.getEggManager().createDragonEgg(dragonType, dragonName, UUID.fromString(eggId));
            
            // Replace in inventory
            player.getInventory().setItem(eggSlot, updatedEgg);
            player.getInventory().sendContents(player); // Make sure client updates
            plugin.getLogger().info("Updated egg in player inventory at slot " + eggSlot);
        } else {
            plugin.getLogger().warning("Could not find egg with ID " + eggId + " in player inventory!");
        }
    }
} 