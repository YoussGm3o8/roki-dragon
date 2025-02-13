package com.youssgm3o8.rokidragon.listeners;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.entities.DragonEntity;
import cn.nukkit.Player;

public class DragonDeathListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof DragonEntity) {
            DragonEntity dragon = (DragonEntity) event.getEntity();
            Player owner = dragon.getOwner();
            if (owner != null) {
                String ownerUUID = owner.getUniqueId().toString();
                DragonPlugin.getInstance().getDatabaseManager().removeDragon(ownerUUID);
            }
        }
    }
}
