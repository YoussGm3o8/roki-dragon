package com.youssgm3o8.rokidragon.listeners; // Updated package

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.dragon.DragonEntity;
import com.youssgm3o8.rokidragon.items.ItemDragonLoaf; // Added import
import com.youssgm3o8.rokidragon.manager.DragonEggManager; // Assuming EggManager is moved

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEntityEvent; // Added import
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerItemConsumeEvent; // Added import
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.PlayerAuthInputPacket;
import cn.nukkit.potion.Effect; // Added import
import cn.nukkit.utils.TextFormat;

/**
 * Event listener for player-dragon interactions and core mechanics
 */
public class EventListenerEdit implements Listener {
    // Store last interact timestamps per player (cooldown in milliseconds)
    private final HashMap<UUID, Long> lastInteract = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 2000; // 2 seconds cooldown
    private final DragonPlugin plugin;
    private final DragonEggManager eggManager; // Added for convenience

    public EventListenerEdit(DragonPlugin plugin) {
        this.plugin = plugin;
        this.eggManager = plugin.getEggManager(); // Get manager instance
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDataPacketReceive(DataPacketReceiveEvent ev) { // Renamed method for clarity
        if (ev.getPacket() instanceof PlayerAuthInputPacket) {
            Player player = ev.getPlayer();
            if (!player.locallyInitialized || player.riding == null) { // Simplified check
                return;
            }
            
            if (player.riding instanceof DragonEntity) {
                PlayerAuthInputPacket pk = (PlayerAuthInputPacket) ev.getPacket();
                double inputX = pk.getMotion().getX();
                double inputY = pk.getMotion().getY();
                
                // Extract pitch and yaw directly from the packet
                float packetPitch = pk.getPitch();
                float packetYaw = pk.getYaw();
                
                // Check bounds for input values if necessary, though Nukkit might handle this
                // if (inputX >= -1.0 && inputX <= 1.0 && inputY >= -1.0 && inputY <= 1.0) { 
                    DragonEntity dragon = (DragonEntity) player.riding;
                    // Pass the extracted pitch and yaw to the input handler
                    dragon.onPlayerInput(player, inputX, inputY, packetPitch, packetYaw); 
                // }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_AIR && 
            event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        Item item = event.getItem(); // Get item once
        
        // Handle Shard Usage while Riding
        if (player.riding instanceof DragonEntity) {
            // Use ShardManager to check if it's a valid shard
            if (plugin.getShardManager().isValidShardForDragon(item, ((DragonEntity) player.riding).getDragonType())) {
                event.setCancelled(true);
                DragonEntity dragon = (DragonEntity) player.riding;
                dragon.shoot(); // Assumes shoot() handles shard consumption
                return; // Interaction handled, exit
            }
        }

        // Handle Dragon Egg Interactions
        if (eggManager.isDragonEgg(item)) {
            event.setCancelled(true); // Always cancel placement/default use

            // Apply interaction cooldown
            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            if (lastInteract.containsKey(playerUUID) && currentTime - lastInteract.get(playerUUID) < INTERACTION_COOLDOWN) {
                return; // Still on cooldown
            }
            lastInteract.put(playerUUID, currentTime);

            // Get egg data
            UUID eggUUID = eggManager.getDragonUUID(item);
            if (eggUUID == null) {
                player.sendMessage("§c" + plugin.getLanguageString("messages.errors.corruptedEgg"));
                return; // Corrupted egg
            }
            String eggId = eggUUID.toString();
            String playerUuidString = playerUUID.toString();

            // Ensure the egg belongs to the player (check active dragon OR stored eggs)
            boolean isPlayerEgg = (eggId.equals(plugin.getDatabaseManager().getDragonEggId(playerUuidString))) || 
                                  (plugin.getDatabaseManager().getEggData(playerUuidString, eggId) != null && !plugin.getDatabaseManager().getEggData(playerUuidString, eggId).isEmpty());

            if (!isPlayerEgg) {
                player.sendMessage("§c" + plugin.getLanguageString("messages.errors.eggNotBelongToYou"));
                
                // --- Egg Explosion Logic ---
                // Remove the egg from player's hand
                player.getInventory().setItemInHand(Item.get(Item.AIR));
                player.getInventory().sendContents(player); // Update client inventory
                
                // Create explosion effect (sound and particle)
                player.getLevel().addLevelSoundEvent(player, cn.nukkit.network.protocol.LevelSoundEventPacket.SOUND_EXPLODE, -1, cn.nukkit.entity.Entity.NETWORK_ID, false, false);
                player.getLevel().addParticle(new cn.nukkit.level.particle.ExplodeParticle(player.getPosition()));
                
                // Damage the player
                player.attack(new cn.nukkit.event.entity.EntityDamageEvent(player, cn.nukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 1f));
                // --- End Egg Explosion Logic ---
                
                return; // Stop further processing
            }

            boolean isHatched = plugin.getDatabaseManager().isEggHatched(eggId);

            if (player.isSneaking()) {
                // --- Shift-Right-Click: Toggle Incubation ---
                if (isHatched) {
                    player.sendMessage("§c" + plugin.getLanguageString("messages.errors.eggAlreadyHatched"));
                    return;
                }

                String currentlyIncubatingEggId = plugin.getDatabaseManager().getIncubatingEggId(playerUuidString);
                if (currentlyIncubatingEggId != null && !currentlyIncubatingEggId.equals(eggId)) {
                    player.sendMessage("§c" + plugin.getLanguageString("messages.errors.oneEggAtATime"));
                    return;
                }

                boolean isCurrentlyIncubating = plugin.getDatabaseManager().isEggIncubating(eggId);
                plugin.getDatabaseManager().setEggIncubating(eggId, !isCurrentlyIncubating);
                eggManager.updateEggLore(item, eggId); // Update lore on the item in hand
                player.getInventory().setItemInHand(item); // Update inventory slot

                String messageKey = !isCurrentlyIncubating ? "messages.success.incubationStarted" : "messages.success.incubationStopped";
                String messageColor = !isCurrentlyIncubating ? "§a" : "§c";
                player.sendMessage(messageColor + plugin.getLanguageString(messageKey));

            } else {
                // --- Regular Right-Click: Summon/Despawn ---
                if (isHatched) {
                    if (plugin.hasActiveDragon(player)) {
                        plugin.despawnDragon(player); // Despawn existing dragon
                        player.sendMessage("§a" + plugin.getLanguageString("messages.success.dragonDismissed"));
                    } else {
                        handleSummonDragon(player); // Attempt to summon
                    }
                } else {
                    // Egg not hatched, inform about shift-click
                    player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.info.useShiftClickToIncubate"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // If player was riding a dragon, handle dismount logic (XP restoration removed)
        if (player.riding instanceof DragonEntity) {
            // DragonEntity dragon = (DragonEntity) player.riding; // No longer needed
            // Removed call to non-existent restorePlayerXP
        }
        // Despawn their dragon if spawned
        plugin.despawnDragon(player);
        lastInteract.remove(player.getUniqueId()); // Clean up cooldown map entry
    }
    
    private void handleSummonDragon(Player player) {
        // Check for cooldown
        if (plugin.isOnCooldown(player.getName())) {
            long remainingSeconds = plugin.getCooldownTime(player.getName());
            player.sendMessage("§c" + MessageFormat.format(
                plugin.getLanguageString("messages.errors.cooldown"), 
                remainingSeconds
            ));
            return;
        }
        
        // Check if already has an active dragon
        if (plugin.hasActiveDragon(player)) {
            player.sendMessage("§c" + plugin.getLanguageString("messages.errors.alreadySummoned"));
            return;
        }
        
        // Check if player is in an allowed world
        if (plugin.areWorldRestrictionsEnabled() && !plugin.isWorldAllowed(player.getLevel().getName())) {
            player.sendMessage("§c" + "Dragons cannot be summoned in this world.");
            return;
        }
        
        // Summon dragon using the manager
        DragonEntity dragon = plugin.getDragonManager().spawnDragon(player);
        if (dragon != null) {
            // Register the dragon using the new method
            plugin.registerActiveDragon(player, dragon);
            
            // Set cooldown
            int summonCooldown = plugin.getConfig().getInt("timing.cooldowns.summon_seconds", 30);
            plugin.setCooldown(player.getName(), summonCooldown); 
        } else {
            player.sendMessage("§c" + plugin.getLanguageString("messages.errors.summonFailed"));
            // Suggest a solution to the player
            player.sendMessage("§e" + "Try moving to a different location or relogging.");
        }
    }
    /**
     * Handles player right-clicking on a DragonEntity.
     * Used for feeding Dragon Loaf.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        Item item = event.getItem();

        // Check if interacting with a DragonEntity and holding Dragon Loaf
        if (entity instanceof DragonEntity && ItemDragonLoaf.isDragonLoaf(item)) {
            DragonEntity dragon = (DragonEntity) entity;

            // Check if the player owns the dragon (optional, maybe allow anyone to feed?)
            // if (dragon.getOwner() != null && !dragon.getOwner().getUniqueId().equals(player.getUniqueId())) {
            //     player.sendMessage(TextFormat.RED + "You can only feed your own dragon!");
            //     return;
            // }

            // Check if dragon is already at full health - USE PLUGIN HEALTH
            if (dragon.getPluginHealth() >= dragon.getPluginMaxHealth()) { // Use new getter methods
                // Update message key to use the generic one from lang file
                player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.info.dragonFullHealth", dragon.getName()));
                return;
            }

            // Calculate heal amount (10% of max health) - USE PLUGIN HEALTH
            float healAmount = dragon.getPluginMaxHealth() * 0.10f;
            dragon.healPluginHealth(healAmount); // Use the new healing method

            // Consume one Dragon Loaf
            if (player.isSurvival() || player.isAdventure()) {
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);
            }

            player.sendMessage(TextFormat.GREEN + "You fed " + dragon.getName() + " a Dragon Loaf. It healed " + (int)healAmount + " HP!");
            // Optional: Add particle effect to dragon
            // dragon.getLevel().addParticle(...);

            event.setCancelled(true); // Prevent default interaction (mounting)
        }
    }

    /**
     * Handles player consuming an item.
     * Used for Dragon Loaf effects on players.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItem();

        if (ItemDragonLoaf.isDragonLoaf(item)) {
            // Apply effects to player
            player.getFoodData().setLevel(20); // Max food level
            player.addEffect(Effect.getEffect(Effect.SLOWNESS)
                .setAmplifier(0) // Slowness I
                .setDuration(30 * 20) // 30 seconds
                .setVisible(true));

            player.sendMessage(TextFormat.GOLD + "The Dragon Loaf is incredibly filling... maybe too filling.");
            // Note: The item is consumed automatically by the event unless cancelled.
        }
    }

    /**
     * Handles player join events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check for incubating eggs
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            // Check a bit later to ensure player data is fully loaded
            plugin.checkAndResumeIncubation(player);
        }, 40); // Check after 2 seconds (40 ticks)
    }
}