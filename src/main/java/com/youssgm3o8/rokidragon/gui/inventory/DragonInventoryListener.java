package com.youssgm3o8.rokidragon.gui.inventory;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;

public class DragonInventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() instanceof DragonInventory) {
            DragonInventory inv = (DragonInventory) event.getInventory();
            
            // Execute the action if it exists for this slot
            if (inv.executeAction(event.getPlayer(), event.getSlot())) {
                event.setCancelled();
            }
            
            // Cancel the event if we don't allow player inventory actions
            if (!inv.isAllowPlayerInventoryActions()) {
                event.setCancelled();
            }
        }
    }

    @EventHandler
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        Player player = event.getTransaction().getSource();
        
        for (InventoryAction action : event.getTransaction().getActions()) {
            if (action instanceof SlotChangeAction) {
                SlotChangeAction slotAction = (SlotChangeAction) action;
                
                if (slotAction.getInventory() instanceof DragonInventory) {
                    DragonInventory inv = (DragonInventory) slotAction.getInventory();
                    
                    // Cancel transaction if we don't allow player inventory actions
                    if (!inv.isAllowPlayerInventoryActions()) {
                        event.setCancelled();
                    }
                }
            }
        }
    }
} 