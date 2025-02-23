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
            if (event.getItem() instanceof ItemFireCharge) {
                event.setCancelled(true);
                DragonEntity dragon = (DragonEntity) player.riding;
                dragon.shootFireball(player);
            }
        }

        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        
        // Egg placement/interaction with cooldown
        if (event.getItem().getId() == BlockID.DRAGON_EGG &&
            event.getItem().hasCompoundTag() &&
            event.getItem().getNamedTag().contains("eggId")) {
                    
            UUID uuid = event.getPlayer().getUniqueId();
            long current = System.currentTimeMillis();
            if (lastInteract.containsKey(uuid) && current - lastInteract.get(uuid) < INTERACTION_COOLDOWN) {
                event.setCancelled(true);
                return;
            }
            lastInteract.put(uuid, current);
            
            event.setCancelled(true); // Prevent egg placement
            
            // NEW: Get "eggId" from the item's NBT and compare with the stored egg for the player.
            Item item = event.getItem();
            String itemEggId = item.getNamedTag().getString("eggId");
            String storedEggId = DragonPlugin.getInstance().getDatabaseManager().getDragonEggId(event.getPlayer().getUniqueId().toString());
            if (storedEggId != null && storedEggId.equals(itemEggId)) {
                // If the dragon is already spawned (record exists), despawn; otherwise, summon.
                if (DragonPlugin.getInstance().getDatabaseManager().playerHasDragon(player.getUniqueId().toString())) {
                    DragonPlugin.getInstance().despawnDragon(player);
                    player.sendMessage("§aYour dragon has been despawned!");
                } else {
                    DragonPlugin.getInstance().handleSummonDragonCommand(player);
                }
            } else {
                player.sendMessage("§cThis dragon egg doesn't belong to you! It is going to explode!");
            }
            return;
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
