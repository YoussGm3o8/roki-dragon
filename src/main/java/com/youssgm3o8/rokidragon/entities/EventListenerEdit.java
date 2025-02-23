package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.network.protocol.PlayerAuthInputPacket;
import nukkitcoders.mobplugin.EventListener;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.entities.animal.walking.Llama;
import nukkitcoders.mobplugin.entities.animal.walking.Pig;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.block.BlockID;
import com.youssgm3o8.rokidragon.DragonPlugin;
import java.util.HashMap;
import java.util.UUID;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.ItemFireCharge;
import cn.nukkit.math.Vector3;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;

public class EventListenerEdit extends EventListener {

    // Store last interact timestamps per player (cooldown in milliseconds)
    private final HashMap<UUID, Long> lastInteract = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 2000; // 2 seconds cooldown

    @Override
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void DataPacketReceiveEvent(DataPacketReceiveEvent ev) {
        if (ev.getPacket() instanceof PlayerAuthInputPacket) {
            Player p = ev.getPlayer();
            if (!p.locallyInitialized) {
                return;
            }
            if (p.riding == null) {
                return;
            }
            PlayerAuthInputPacket pk = (PlayerAuthInputPacket) ev.getPacket();
            double inputX = pk.getMotion().getX();
            double inputY = pk.getMotion().getY();
            if (inputX >= -1.0 && inputX <= 1.0 && inputY >= -1.0 && inputY <= 1.0) {
                if (p.riding instanceof HorseBase && !(p.riding instanceof Llama)) {
                    ((HorseBase) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Pig) {
                    ((Pig) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Strider) {
                    ((Strider) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof DragonEntity) {
                    ((DragonEntity) p.riding).onPlayerInput(p, inputX, inputY);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_AIR && 
            event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        if (player.riding instanceof DragonEntity) {
            Item item = event.getItem();
            if (item != null && item.hasCompoundTag() && item.getNamedTag().getBoolean("IsDragonShard")) {
                event.setCancelled(true);
                DragonEntity dragon = (DragonEntity) player.riding;
                dragon.shoot();
            }
        }

        Item item = event.getItem();
        if (item == null || item.getId() != BlockID.DRAGON_EGG || !item.hasCompoundTag() || !item.getNamedTag().contains("eggId")) {
            return;
        }
        
        // Egg interaction with cooldown
        UUID uuid = event.getPlayer().getUniqueId();
        long current = System.currentTimeMillis();
        if (lastInteract.containsKey(uuid) && current - lastInteract.get(uuid) < INTERACTION_COOLDOWN) {
            event.setCancelled(true);
            return;
        }
        lastInteract.put(uuid, current);
        
        event.setCancelled(true); // Prevent egg placement
        
        // Get "eggId" from the item's NBT and compare with the stored egg for the player.
        String itemEggId = item.getNamedTag().getString("eggId");
        String storedEggId = DragonPlugin.getInstance().getDatabaseManager().getDragonEggId(event.getPlayer().getUniqueId().toString());

        // Check if player is sneaking (shift) for incubation
        if (player.isSneaking()) {
            // Handle incubation
            if (storedEggId != null && storedEggId.equals(itemEggId)) {
                // Check if egg is already hatched
                if (DragonPlugin.getInstance().getDatabaseManager().isEggHatched(itemEggId)) {
                    player.sendMessage("§cThis egg has already hatched!");
                    return;
                }

                // Check if another egg is already incubating
                String incubatingEggId = DragonPlugin.getInstance().getDatabaseManager().getIncubatingEggId(player.getUniqueId().toString());
                if (incubatingEggId != null && !incubatingEggId.equals(itemEggId)) {
                    player.sendMessage("§cYou can only incubate one egg at a time!");
                    return;
                }

                // Toggle incubation
                boolean isIncubating = DragonPlugin.getInstance().getDatabaseManager().isEggIncubating(itemEggId);
                DragonPlugin.getInstance().getDatabaseManager().setEggIncubating(itemEggId, !isIncubating);
                
                // Update egg lore
                DragonPlugin.getInstance().getEggManager().updateEggLore(item, itemEggId);

                // Send message
                String message = !isIncubating ? 
                    "§aStarted incubating this egg!" :
                    "§cStopped incubating this egg.";
                player.sendMessage(message);
            } else {
                player.sendMessage("§cThis dragon egg doesn't belong to you! It is going to explode!");
            }
        } else {
            // Normal right-click behavior (summon/despawn)
            if (storedEggId != null && storedEggId.equals(itemEggId)) {
                if (DragonPlugin.getInstance().getDatabaseManager().playerHasDragon(player.getUniqueId().toString())) {
                    DragonPlugin.getInstance().despawnDragon(player);
                } else {
                    DragonPlugin.getInstance().handleSummonDragonCommand(player, new String[0]);
                }
            } else {
                player.sendMessage("§cThis dragon egg doesn't belong to you! It is going to explode!");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // If player was riding a dragon, restore their XP
        if (player.riding instanceof DragonEntity) {
            DragonEntity dragon = (DragonEntity) player.riding;
            dragon.restorePlayerXP(player);
        }
        // Despawn their dragon if spawned
        DragonPlugin.getInstance().despawnDragon(player);
    }
}
