package com.youssgm3o8.rokidragon.gui;

import cn.nukkit.Player;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Config;
import cn.nukkit.block.BlockID;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;
import cn.nukkit.nbt.tag.CompoundTag;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.inventory.CustomFakeInventory;
import com.youssgm3o8.rokidragon.gui.inventory.FakeInventoryManager;
import me.iwareq.fakeinventories.util.ItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DragonManagementGUI {
    private static final int STORAGE_SIZE = 5; // Hopper size
    private final Map<String, Inventory> playerInventories = new HashMap<>();
    private final DragonPlugin plugin;
    private final Config langConfig;

    public DragonManagementGUI(DragonPlugin plugin) {
        this.plugin = plugin;
        this.langConfig = plugin.getLanguageConfig();
    }

    private String getLangString(String key) {
        String message = langConfig.getString("gui." + key);
        return message != null ? message : "Missing language key: " + key;
    }

    public void openMainMenu(Player player) {
        CustomFakeInventory inv = FakeInventoryManager.createInventory(player, InventoryType.HOPPER, "§9Dragon Management");
        
        // Dragon Customization Button
        Item customizeButton = Item.get(351, 4, 1); // Blue dye
        customizeButton.setCustomName("§9" + getLangString("mainMenu.buttons.customize"));
        ArrayList<String> customizeLore = new ArrayList<>();
        customizeLore.add("§7Change your dragon's appearance");
        customizeLore.add("§7and special effects");
        customizeButton.setLore(customizeLore.toArray(new String[0]));
        
        // Dragon Storage Button
        Item storageButton = Item.get(54, 0, 1); // Chest
        storageButton.setCustomName("§6" + getLangString("mainMenu.buttons.storage"));
        ArrayList<String> storageLore = new ArrayList<>();
        storageLore.add("§7Store and manage your");
        storageLore.add("§7dragon eggs safely");
        storageButton.setLore(storageLore.toArray(new String[0]));

        // Set items
        inv.setItem(1, customizeButton);
        inv.setItem(3, storageButton);

        // Set handlers for specific slots
        inv.setHandler(1, (item, event) -> {
            event.setCancelled(true);
            openCustomizationMenu((Player) event.getTransaction().getSource());
        });
        
        inv.setHandler(3, (item, event) -> {
            event.setCancelled(true);
            openStorage((Player) event.getTransaction().getSource());
        });

        // Set default handler to cancel all other interactions
        inv.setHandler((item, event) -> event.setCancelled(true));

        player.addWindow(inv);
    }

    public void openCustomizationMenu(Player player) {
        CustomFakeInventory inv = FakeInventoryManager.createInventory(player, InventoryType.CHEST, "§9Dragon Customization");
        
        // Fire Dragon Button
        Item fireButton = Item.get(Item.BLAZE_POWDER, 0, 1);
        fireButton.setCustomName("§c" + getLangString("customization.buttons.type.fire"));
        ArrayList<String> fireLore = new ArrayList<>();
        fireLore.add("§7Click to change your dragon");
        fireLore.add("§7into a Fire Dragon");
        fireButton.setLore(fireLore.toArray(new String[0]));
        
        // Ice Dragon Button
        Item iceButton = Item.get(Item.ICE, 0, 1);
        iceButton.setCustomName("§b" + getLangString("customization.buttons.type.ice"));
        ArrayList<String> iceLore = new ArrayList<>();
        iceLore.add("§7Click to change your dragon");
        iceLore.add("§7into an Ice Dragon");
        iceButton.setLore(iceLore.toArray(new String[0]));
        
        // Lightning Dragon Button
        Item lightningButton = Item.get(Item.GLOWSTONE_DUST, 0, 1);
        lightningButton.setCustomName("§e" + getLangString("customization.buttons.type.lightning"));
        ArrayList<String> lightningLore = new ArrayList<>();
        lightningLore.add("§7Click to change your dragon");
        lightningLore.add("§7into a Lightning Dragon");
        lightningButton.setLore(lightningLore.toArray(new String[0]));

        // Back Button
        Item backButton = Item.get(Item.ARROW, 0, 1);
        backButton.setCustomName("§c" + getLangString("customization.buttons.back"));
        ArrayList<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backButton.setLore(backLore.toArray(new String[0]));

        // Set items in specific slots
        inv.setItem(11, fireButton);
        inv.setItem(13, iceButton);
        inv.setItem(15, lightningButton);
        inv.setItem(26, backButton);

        // Set handlers for specific slots
        inv.setHandler((item, event) -> {
            event.setCancelled(true);
            Player p = (Player) event.getTransaction().getSource();
            int slot = -1;
            
            if (event.getTransaction().getActions().size() > 0) {
                for (var action : event.getTransaction().getActions()) {
                    if (action instanceof SlotChangeAction) {
                        slot = ((SlotChangeAction) action).getSlot();
                        break;
                    }
                }
            }

            switch (slot) {
                case 11: // Fire Dragon
                    handleDragonTypeSelection(p, "Fire Dragon");
                    break;
                case 13: // Ice Dragon
                    handleDragonTypeSelection(p, "Ice Dragon");
                    break;
                case 15: // Lightning Dragon
                    handleDragonTypeSelection(p, "Lightning Dragon");
                    break;
                case 26: // Back button
                    openMainMenu(p);
                    break;
            }
        });

        // Close any existing windows first
        if (player.getTopWindow().isPresent()) {
            player.removeWindow(player.getTopWindow().get());
        }
        
        player.addWindow(inv);
    }

    public void openStorage(Player player) {
        CustomFakeInventory storage = FakeInventoryManager.createInventory(player, InventoryType.HOPPER, "Dragon Storage");
        
        // Load stored eggs from database
        loadStoredEggs(player, storage);
        
        // Set default handler that checks slot permissions and item type
        storage.setHandler((item, event) -> {
            int slot = -1;
            for (var action : event.getTransaction().getActions()) {
                if (action instanceof SlotChangeAction) {
                    SlotChangeAction slotAction = (SlotChangeAction) action;
                    slot = slotAction.getSlot();
                    
                    // Check if this is a valid storage slot
                    if (!isStorageSlot(slot)) {
                        event.setCancelled(true);
                        return;
                    }
                    
                    // Get the item being placed
                    Item newItem = slotAction.getTargetItem();
                    if (!newItem.isNull() && !isDragonEgg(newItem)) {
                        event.setCancelled(true);
                        player.sendMessage(TextFormat.RED + getLangString("storage.messages.onlyEggs"));
                        return;
                    }
                    
                    // If we're removing an item, no need for further checks
                    if (newItem.isNull()) {
                        return;
                    }
                    
                    // Verify the egg belongs to the player
                    String eggId = newItem.getNamedTag().getString("eggId");
                    List<String> playerEggs = plugin.getDatabaseManager().getPlayerEggIds(player.getUniqueId().toString());
                    if (!playerEggs.contains(eggId)) {
                        event.setCancelled(true);
                        player.sendMessage(TextFormat.RED + getLangString("storage.messages.notYourEgg"));
                        return;
                    }
                }
            }
        });

        // Add right-click handler for incubation toggle
        storage.setHandler((item, event) -> {
            if (event.getTransaction().getSource() instanceof Player) {
                Player p = (Player) event.getTransaction().getSource();
                Item clickedItem = item;
                if (isDragonEgg(clickedItem)) {
                    event.setCancelled(true);
                    String eggId = clickedItem.getNamedTag().getString("eggId");
                    
                    // Check if this egg is already hatched
                    if (clickedItem.getNamedTag().getBoolean("hatched")) {
                        p.sendMessage(TextFormat.RED + "This egg has already hatched!");
                        return;
                    }
                    
                    // Check if another egg is already incubating
                    String incubatingEggId = plugin.getDatabaseManager().getIncubatingEggId(p.getUniqueId().toString());
                    if (incubatingEggId != null && !incubatingEggId.equals(eggId)) {
                        p.sendMessage(TextFormat.RED + "You can only incubate one egg at a time!");
                        return;
                    }
                    
                    // Toggle incubation
                    boolean isIncubating = plugin.getDatabaseManager().isEggIncubating(eggId);
                    plugin.getDatabaseManager().setEggIncubating(eggId, !isIncubating);
                    
                    // Refresh the inventory
                    loadStoredEggs(p, storage);
                    
                    // Send message
                    String message = !isIncubating ? 
                        "§aStarted incubating this egg!" :
                        "§cStopped incubating this egg.";
                    p.sendMessage(message);
                }
            }
        });

        playerInventories.put(player.getUniqueId().toString(), storage);
        player.addWindow(storage);
    }

    private boolean isDragonEgg(Item item) {
        return item.getId() == BlockID.DRAGON_EGG && 
               item.hasCompoundTag() && 
               item.getNamedTag().contains("eggId");
    }

    private void loadStoredEggs(Player player, CustomFakeInventory storage) {
        String playerUUID = player.getUniqueId().toString();
        Map<Integer, CompoundTag> storedEggs = plugin.getDatabaseManager().getStoredEggs(playerUUID);
        
        for (Map.Entry<Integer, CompoundTag> entry : storedEggs.entrySet()) {
            int slot = entry.getKey();
            CompoundTag eggData = entry.getValue();
            
            Item eggItem = Item.get(BlockID.DRAGON_EGG);
            eggItem.setNamedTag(eggData);
            String eggId = eggData.getString("eggId");
            
            // Update egg lore
            plugin.getEggManager().updateEggLore(eggItem, eggId);
            
            storage.setItem(slot, eggItem);
        }
    }

    public void saveStorage(Player player) {
        String playerUUID = player.getUniqueId().toString();
        Inventory storage = playerInventories.get(playerUUID);
        if (storage != null) {
            // Save each egg's data to the database
            Map<Integer, CompoundTag> eggsToStore = new HashMap<>();
            for (int i = 0; i < STORAGE_SIZE; i++) {
                Item item = storage.getItem(i);
                if (!item.isNull() && isDragonEgg(item)) {
                    eggsToStore.put(i, item.getNamedTag());
                }
            }
            
            // Update database
            plugin.getDatabaseManager().saveStoredEggs(playerUUID, eggsToStore);
            playerInventories.remove(playerUUID);
            player.sendMessage(TextFormat.GREEN + getLangString("storage.messages.saved"));
        }
    }

    public boolean isStorageInventory(Inventory inventory) {
        return inventory != null && inventory.getTitle().equals("Dragon Storage");
    }

    public boolean isStorageSlot(int slot) {
        return slot >= 0 && slot < STORAGE_SIZE;
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
        
        // Send confirmation message
        player.sendMessage(TextFormat.GREEN + getLangString("customization.messages.typeChanged")
            .replace("{type}", dragonType));
        
        // Return to main menu
        openMainMenu(player);
    }
}