package com.youssgm3o8.rokidragon.listeners;

import com.youssgm3o8.rokidragon.dragon.DragonEntity;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;

public class DragonDeathListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof DragonEntity) {
            DragonEntity dragon = (DragonEntity) event.getEntity();
            Player owner = dragon.getOwner();
            if (owner != null) {
                // Database removal is now handled within DragonEntity.handleDeath()
                // String ownerUUID = owner.getUniqueId().toString();
                // DragonPlugin.getInstance().getDatabaseManager().removeDragon(ownerUUID);
            }
        }
    }
}
