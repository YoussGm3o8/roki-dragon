package com.youssgm3o8.rokidragon.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.DragonManagementGUI;
import com.youssgm3o8.rokidragon.entities.DragonEntity;
import cn.nukkit.event.player.PlayerQuitEvent;

public class DragonGUIListener implements Listener {
    private final DragonPlugin plugin;
    private final DragonManagementGUI gui;

    public DragonGUIListener(DragonPlugin plugin, DragonManagementGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = event.getPlayer();
        Inventory inventory = event.getInventory();
        Item clickedItem = event.getSourceItem();
        int slot = event.getSlot();

        if (inventory == null || clickedItem == null) return;

        String title = inventory.getTitle();
        
        // Handle main menu clicks
        if (title.equals("§9Dragon Management")) {
            event.setCancelled(true);
            
            if (slot == 1) { // Customize button
                gui.openCustomizationMenu(player);
            } else if (slot == 3) { // Storage button
                gui.openStorage(player);
            }
            return;
        }

        // Handle customization menu clicks
        if (title.equals("§9Dragon Customization")) {
            event.setCancelled(true);
            
            switch (slot) {
                case 11: // Fire Dragon
                    handleDragonTypeSelection(player, "Fire Dragon");
                    break;
                case 13: // Ice Dragon
                    handleDragonTypeSelection(player, "Ice Dragon");
                    break;
                case 15: // Lightning Dragon
                    handleDragonTypeSelection(player, "Lightning Dragon");
                    break;
                case 26: // Back button
                    gui.openMainMenu(player);
                    break;
            }
            return;
        }

        // Handle storage inventory
        if (title.equals("Dragon Storage")) {
            // Only allow interactions with storage slots
            if (!gui.isStorageSlot(slot)) {
                event.setCancelled(true);
            }
        }
    }

    private void handleDragonTypeSelection(Player player, String dragonType) {
        String playerUUID = player.getUniqueId().toString();
        String eggId = plugin.getDatabaseManager().getDragonEggId(playerUUID);
        
        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "You need to have a dragon egg first!");
            return;
        }
        
        // Update the dragon type in the database
        plugin.getDatabaseManager().setDragonType(eggId, dragonType);
        
        // If dragon is spawned, update it
        for (cn.nukkit.entity.Entity entity : player.getLevel().getEntities()) {
            if (entity instanceof DragonEntity) {
                DragonEntity dragon = (DragonEntity) entity;
                if (dragon.getOwner() != null && dragon.getOwner().equals(player)) {
                    dragon.setDragonType(dragonType);
                    break;
                }
            }
        }
        
        // Send confirmation message
        player.sendMessage(TextFormat.GREEN + "Dragon type set to " + dragonType + "!");
        
        // Return to main menu
        gui.openMainMenu(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != null && gui.isStorageInventory(event.getInventory())) {
            gui.saveStorage((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getTopWindow().isPresent() && gui.isStorageInventory(player.getTopWindow().get())) {
            gui.saveStorage(player);
        }
    }
} 