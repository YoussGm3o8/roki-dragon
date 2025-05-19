package com.youssgm3o8.rokidragon.listeners; // Updated package

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
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
import cn.nukkit.nbt.tag.CompoundTag;

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
        if (event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_AIR || event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            Item item = event.getItem();
            Player player = event.getPlayer();
            
            // Add cooldown to prevent spam clicking
            long currentTime = System.currentTimeMillis();
            UUID playerUUID = player.getUniqueId();
            if (lastInteract.containsKey(playerUUID) && currentTime - lastInteract.get(playerUUID) < INTERACTION_COOLDOWN) {
                // Still on cooldown, ignore this interaction
                return;
            }
            lastInteract.put(playerUUID, currentTime);
            
            // Check if this is a dragon egg
            if (!eggManager.isDragonEgg(item)) {
                return;
            }
            
            // Extract the dragon_egg_id from NBT
            if (!item.hasCompoundTag()) {
                return;
            }
            
            CompoundTag nbt = item.getNamedTag();
            if (nbt == null) {
                return;
            }
            
            String eggId = nbt.getString("dragon_egg_id");
            if (eggId == null || eggId.isEmpty()) {
                return;
            }
            
            String playerUuidString = player.getUniqueId().toString();
            
            // Check if this egg belongs to the player
            if (!plugin.getDatabaseManager().isEggOwnedByPlayer(eggId, playerUuidString)) {
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
                
                return; // Stop further processing - no contradictory messages will be sent
            }
            
            // Check if this egg was reported as lost but still exists
            // This can happen if the egg was reported lost in the GUI but the item is still in inventory
            if (!plugin.getDatabaseManager().doesEggExist(eggId)) {
                plugin.getLogger().info("Player " + player.getName() + " tried to use a reported lost egg: " + eggId);
                player.sendMessage("§c" + plugin.getLanguageString("messages.errors.lostEggExploded"));
                
                // --- Egg Explosion Logic ---
                player.getInventory().setItemInHand(Item.get(Item.AIR));
                player.getInventory().sendContents(player); 
                
                player.getLevel().addLevelSoundEvent(player, cn.nukkit.network.protocol.LevelSoundEventPacket.SOUND_EXPLODE, -1, cn.nukkit.entity.Entity.NETWORK_ID, false, false);
                player.getLevel().addParticle(new cn.nukkit.level.particle.ExplodeParticle(player.getPosition()));
                
                // Damage the player more for using a reported lost egg
                player.attack(new cn.nukkit.event.entity.EntityDamageEvent(player, cn.nukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 2f));
                return;
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
                // --- Regular Right-Click: Summon/Despawn OR Show Incubating Status --- 
                if (isHatched) {
                    if (plugin.hasActiveDragon(player)) {
                        // Get the player's active dragon
                        DragonEntity activeDragon = plugin.getActiveDragons().get(player.getUniqueId());
                        
                        // Check if the active dragon matches this egg's ID
                        if (activeDragon != null && eggId.equals(activeDragon.getDragonId())) {
                            // Egg matches the active dragon, so despawn it
                            plugin.despawnDragon(player);
                            player.sendMessage("§a" + plugin.getLanguageString("messages.success.dragonDismissed"));
                        } else {
                            // Egg doesn't match the active dragon
                            player.sendMessage("§c" + plugin.getLanguageString("messages.errors.wrongEgg"));
                        }
                    } else {
                        // *** Check if dragon needs naming BEFORE summoning ***
                        String existingName = plugin.getDatabaseManager().getDragonName(eggId);
                        boolean needsNaming = existingName == null || existingName.trim().isEmpty() || existingName.equals("Dragon");

                        plugin.getLogger().info("[Summon Check] EggId: " + eggId + ", Existing Name: '" + existingName + "', Needs Naming: " + needsNaming);

                        if (needsNaming) {
                            // Dragon hasn't been named yet, open the naming form
                            plugin.getDragonGUI().openFirstNamingForm(player, eggId);
                            plugin.getLogger().info("Opening first naming form for egg " + eggId);
                            // Do not summon the dragon yet, it will be summoned after naming
                        } else {
                            // Dragon already has a name, proceed with summoning
                            plugin.getLogger().info("Dragon for egg " + eggId + " already named ('" + existingName + "'), summoning directly.");
                            handleSummonDragon(player, eggId); // Pass eggId to ensure the right dragon is summoned
                        }
                    }
                } else {
                    // Egg is NOT hatched. Check if it IS incubating.
                    boolean isIncubating = plugin.getDatabaseManager().isEggIncubating(eggId);
                    plugin.getLogger().info("[Egg Listener Debug] onPlayerInteract (EventListenerEdit): eggId='" + eggId + "', isHatched=" + isHatched + ", isIncubating=" + isIncubating);

                    if (isIncubating) {
                        // It's not hatched, but it IS incubating. Tell the player the remaining time.
                        int requiredSeconds = plugin.getConfig().getInt("timing.eggs.incubation_time_seconds", 3600);
                        int currentProgress = 0;
                        try {
                            Map<String, String> eggData = plugin.getDatabaseManager().getEggData(player.getUniqueId().toString(), eggId);
                            if (eggData != null && eggData.containsKey("incubationProgress")) {
                                currentProgress = Integer.parseInt(eggData.get("incubationProgress"));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Could not retrieve incubation progress for egg " + eggId + " during interact check: " + e.getMessage());
                        }
                        int remainingSeconds = Math.max(0, requiredSeconds - currentProgress);
                        String remainingTimeFormatted = formatTime(remainingSeconds); // Use helper method

                        player.sendMessage(plugin.getLanguageString("messages.errors.alreadyIncubating", remainingTimeFormatted));
                    } else {
                        // It's not hatched AND not incubating. Tell them to use shift-click for incubation.
                        // Use the correct key from language file
                        player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.info.useShiftClickToIncubate"));
                    }
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
    
    private void handleSummonDragon(Player player, String eggId) {
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
        
        // Get the dragon's details from the database
        String dragonType = plugin.getDatabaseManager().getDragonType(eggId);
        String dragonName = plugin.getDatabaseManager().getDragonName(eggId);
        
        // Summon the specific dragon using the egg ID
        DragonEntity dragon = plugin.getDragonManager().spawnDragon(
            dragonType,
            dragonName,
            UUID.fromString(eggId),
            player.getLevel(),
            player.getPosition(),
            player
        );
        
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
            if (dragon.getPluginHealth() >= dragon.getPluginMaxHealth()) {
                // Update message key to use the generic one from lang file
                player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.info.dragonFullHealth", dragon.getName()));
                event.setCancelled(true); // Prevent default interaction (mounting)
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

            // Use language key for feeding message
            player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.dragonFed", 
                dragon.getName(), (int)healAmount));

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
            // Check if player is still online before processing
            if (player != null && player.isOnline()) {
                // Check a bit later to ensure player data is fully loaded
                plugin.checkAndResumeIncubation(player);
            }
        }, 100); // Check after 5 seconds (100 ticks) to ensure inventory is fully loaded
    }

    // Helper method to format time (moved here from DragonEggListener)
    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 seconds";
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        if (seconds > 0) {
            if (minutes > 0) {
                sb.append(", ");
            }
            sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }
        return sb.toString();
    }
}