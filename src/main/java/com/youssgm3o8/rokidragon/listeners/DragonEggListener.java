package com.youssgm3o8.rokidragon.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.dragon.DragonEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all dragon egg interactions, including:
 * - Preventing placement as blocks
 * - Managing incubation (shift-right-click)
 * - Limiting to one incubating egg per player
 * - Summoning dragons from hatched eggs
 */
public class DragonEggListener implements Listener {
    private final DragonPlugin plugin;
    private final HashMap<UUID, Long> lastInteract = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 200; // 200ms cooldown

    public DragonEggListener(DragonPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent dragon eggs from being placed as blocks
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Item item = event.getItem();
        // Check if the item is a dragon egg (by ID and NBT tag)
        if (item.getId() == BlockID.DRAGON_EGG && item.hasCompoundTag() && 
            (item.getNamedTag().contains("eggId") || item.getNamedTag().contains("DragonUUID") || 
             item.getNamedTag().getBoolean("IsDragonEgg"))) {
            
            event.setCancelled(true);
            Player player = event.getPlayer();
            // Revert: Send original message directly
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.cannotPlaceEgg"));
        }
    }

    /**
     * Handle egg shift-right-click for incubation and normal right-click for dragon summoning
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_AIR && 
            event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Item item = event.getItem();
        // Check if item is a dragon egg
        if (item == null || item.getId() != BlockID.DRAGON_EGG || !item.hasCompoundTag()) {
            return;
        }
        
        // Check for dragon egg NBT
        CompoundTag tag = item.getNamedTag();
        if (!(tag.contains("eggId") || tag.contains("DragonUUID") || tag.getBoolean("IsDragonEgg"))) {
            return;
        }
        
        // Cancel the event to prevent placement
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Apply cooldown to prevent spam-clicking
        long current = System.currentTimeMillis();
        if (lastInteract.containsKey(uuid) && current - lastInteract.get(uuid) < INTERACTION_COOLDOWN) {
            return;
        }
        lastInteract.put(uuid, current);
        
        // Get egg ID
        String eggId = tag.contains("eggId") ? tag.getString("eggId") : 
                      (tag.contains("DragonUUID") ? tag.getString("DragonUUID") : null);
        
        if (eggId == null) {
            player.sendMessage(plugin.getLanguageString("messages.errors.invalidEgg")); // Use lang key
            return;
        }
        
        String playerUUID = player.getUniqueId().toString();
        
        // Verify ownership by checking the owner UUID stored in the DragonEgg table
        String ownerUUID = plugin.getDatabaseManager().getPlayerUUIDFromEggId(eggId);
        boolean isPlayerEgg = ownerUUID != null && ownerUUID.equals(playerUUID);

        if (!isPlayerEgg) {
            // Add a check for admin override if desired (e.g., using a permission)
            // if (!player.hasPermission("rokidragon.admin.interactall")) { ... }
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggNotBelongToYou"));
            return;
        }
        
        // Get egg status
        boolean isHatched = plugin.getDatabaseManager().isEggHatched(eggId);
        
        // Handle shift-right-click for incubation
        if (player.isSneaking()) {
            handleEggIncubation(player, item, eggId, isHatched);
        } else {
            // Regular right-click
            handleEggInteraction(player, eggId, isHatched);
        }
    }
    
    /**
     * Handle egg incubation toggle (shift-right-click)
     */
    private void handleEggIncubation(Player player, Item item, String eggId, boolean isHatched) {
        if (isHatched) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggAlreadyHatched"));
            return;
        }
        
        String playerUUID = player.getUniqueId().toString();
        
        // Check if another egg is already incubating
        String incubatingEggId = plugin.getDatabaseManager().getIncubatingEggId(playerUUID);
        if (incubatingEggId != null && !incubatingEggId.equals(eggId)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.oneEggAtATime"));
            return;
        }
        
        // Toggle incubation
        boolean isIncubating = plugin.getDatabaseManager().isEggIncubating(eggId);
        
        try {
            // If we're stopping incubation, make sure we preserve the progress in the database
            if (isIncubating) {
                // Get current progress before stopping incubation
                Map<String, String> eggData = plugin.getDatabaseManager().getEggData(
                    plugin.getDatabaseManager().getPlayerUUIDFromEggId(eggId), eggId);
                if (eggData != null && eggData.containsKey("incubationProgress")) {
                    int currentProgress = Integer.parseInt(eggData.get("incubationProgress"));
                    // Store the progress value - this ensures it's not lost when incubation stops
                    plugin.getDatabaseManager().updateIncubationProgress(eggId, currentProgress);
                    plugin.getLogger().info("Preserved incubation progress " + currentProgress + " seconds for egg " + eggId);
                }
            } else {
                // Starting incubation - only set the start time if it's not already set
                long startTime = plugin.getDatabaseManager().getEggIncubationStartTime(eggId);
                if (startTime == 0) {
                    long currentTime = System.currentTimeMillis() / 1000;
                    plugin.getDatabaseManager().updateIncubationStartTime(eggId, currentTime);
                    plugin.getLogger().info("Started incubation for egg " + eggId + " at time " + currentTime);
                } else {
                    plugin.getLogger().info("Resumed incubation for egg " + eggId + " with existing start time: " + startTime);
                }
                
                // Set NBT data to indicate incubation started
                CompoundTag tag = item.getNamedTag();
                tag.putBoolean("incubating", true);
                tag.putString("eggId", eggId);
                tag.putString("dragon_egg_id", eggId);
                tag.putInt("IncubationProgress", 0);
                item.setNamedTag(tag);
            }
            
            // Set incubation state in the database
            plugin.getDatabaseManager().setEggIncubating(eggId, !isIncubating);
            plugin.getLogger().info("Set incubation state to " + (!isIncubating) + " for egg " + eggId);
            
            // Update egg lore
            plugin.getEggManager().updateEggLore(item, eggId);
            
            // Update the item in the player's inventory
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                Item inventoryItem = player.getInventory().getItem(slot);
                if ((inventoryItem.getId() == item.getId()) && 
                    inventoryItem.hasCompoundTag() && 
                    item.hasCompoundTag() && 
                    inventoryItem.getNamedTag().getString("eggId").equals(eggId)) {
                    
                    // Replace the item with our updated one
                    player.getInventory().setItem(slot, item);
                    
                    // Make sure inventory is updated for the player
                    player.getInventory().sendContents(player);
                    plugin.getLogger().info("Updated egg in inventory slot " + slot + " for player " + player.getName());
                    break;
                }
            }
            
            // Send message
            if (!isIncubating) {
                player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.incubationStarted"));
                plugin.getLogger().info("Player " + player.getName() + " started incubating egg " + eggId);
                // NOTE: Incubation progress is handled by the database and ProcessIncubationTask.
                // No need to set NBT progress here. The lore update will reflect DB state.
            } else {
                player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.success.incubationStopped"));
                plugin.getLogger().info("Player " + player.getName() + " stopped incubating egg " + eggId);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error toggling incubation: " + e.getMessage(), e);
            player.sendMessage(plugin.getLanguageString("messages.errors.genericError")); // Use generic error lang key
        }
    }
    
    /**
     * Handle regular egg interaction (right-click without shift)
     */
    private void handleEggInteraction(Player player, String eggId, boolean isHatched) {
        // First check if the dragon is dead
        boolean isDead = plugin.getDatabaseManager().isDragonDead(eggId);
        
        if (isDead) {
            // Get the dragon's name
            String dragonName = plugin.getDatabaseManager().getDragonName(eggId);
            if (dragonName == null || dragonName.isEmpty()) {
                dragonName = "Your dragon";
            }
            
            // Get death information
            Map<String, Object> deathInfo = plugin.getDatabaseManager().getDragonDeathInfo(eggId);
            String killedBy = "Unknown";
            if (deathInfo != null && deathInfo.containsKey("killedBy")) {
                killedBy = (String) deathInfo.get("killedBy");
            }
            
            // Send death message
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.egg.dragonDeathMessage", 
                TextFormat.BOLD + dragonName + TextFormat.RESET + TextFormat.RED,
                killedBy));
            
            // Add message about not counting toward limit
            player.sendMessage(TextFormat.GREEN + "This deceased dragon does not count toward your dragon limit.");
            
            return; // Don't proceed with summoning
        }
        
        if (isHatched) {
            // Summon or despawn dragon
            if (plugin.hasActiveDragon(player)) {
                plugin.despawnDragon(player);
                player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.dragonDismissed"));
            } else {
                summonDragon(player);
            }
        } else {
            // Revert: Original logic - Inform player to use shift-right-click
            player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.errors.needShiftClick"));
        }
    }
    
    /**
     * Summon the player's dragon
     */
    private void summonDragon(Player player) {
        // Check for cooldown
        if (plugin.isOnCooldown(player.getName())) {
            long remainingSeconds = plugin.getCooldownTime(player.getName());
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.cooldown", remainingSeconds));
            return;
        }
        
        // Check if already has an active dragon (double-check for safety)
        if (plugin.hasActiveDragon(player)) {
            player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.errors.alreadySummoned"));
            return;
        }
        
        // Check if player is in an allowed world
        if (plugin.areWorldRestrictionsEnabled() && !plugin.isWorldAllowed(player.getLevel().getName())) {
            player.sendMessage(TextFormat.RED + "Dragons cannot be summoned in this world.");
            return;
        }
        
        // Summon dragon
        DragonEntity dragon = plugin.getDragonManager().spawnDragon(player);
        if (dragon != null) {
            // Register the dragon using the new method
            plugin.registerActiveDragon(player, dragon);
            
            // Set cooldown
            int summonCooldown = plugin.getConfig().getInt("timing.cooldowns.summon_seconds", 30);
        } else {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.summonFailed"));
            // Suggest a solution to the player
            player.sendMessage(TextFormat.YELLOW + "Try moving to a different location or relogging.");
        }
    }
    
    /**
     * When player quits, handle dragon despawning
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // If player was riding a dragon, restore their XP
        if (player.riding instanceof DragonEntity) {
            // DragonEntity dragon = (DragonEntity) player.riding; // No longer needed
            // Removed call to non-existent restorePlayerXP
        }
        // Despawn their dragon if spawned
        plugin.despawnDragon(player);
    }
} 