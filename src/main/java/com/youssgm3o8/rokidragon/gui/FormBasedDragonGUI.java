package com.youssgm3o8.rokidragon.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.item.Item;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.nbt.tag.CompoundTag;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.DatabaseManager;
import com.youssgm3o8.rokidragon.dragon.DragonEntity;
import com.youssgm3o8.rokidragon.manager.DragonEggManager;
import com.youssgm3o8.rokidragon.manager.DragonShardManager;
import com.youssgm3o8.rokidragon.util.DragonUtils;
import me.onebone.economyapi.EconomyAPI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.text.MessageFormat;

/**
 * Handles dragon management using Nukkit forms instead of fake inventories.
 */
public class FormBasedDragonGUI {
    private final DragonPlugin plugin;
    private final DatabaseManager databaseManager;
    private final DragonEggManager eggManager;
    private final DragonShardManager shardManager;
    
    // Form IDs for identifying responses - using precomputed hash-based values to ensure uniqueness
    // These need to be compile-time constants for use in switch statements
    private static final String PLUGIN_NAMESPACE = "com.youssgm3o8.rokidragon";
    // Precomputed hash values for form IDs to make them compile-time constants
    public static final int FORM_MAIN_MENU = -1395729433; // hash of "com.youssgm3o8.rokidragon.mainmenu"
    public static final int FORM_CUSTOMIZATION = 1496915066; // hash of "com.youssgm3o8.rokidragon.customization"
    public static final int FORM_STORAGE = 1078718968; // hash of "com.youssgm3o8.rokidragon.storage"
    public static final int FORM_BUY_EGG = 1234598991; // hash of "com.youssgm3o8.rokidragon.buyegg"
    public static final int FORM_LOST_EGGS = -1985504793; // hash of "com.youssgm3o8.rokidragon.losteggs"
    public static final int FORM_LOST_EGG_CONFIRM = -1781855235; // hash of "com.youssgm3o8.rokidragon.losteggconfirm"
    public static final int FORM_HELP_MENU = 2103615617; // hash of "com.youssgm3o8.rokidragon.helpmenu"
    public static final int FORM_INFO = -1628154958; // hash of "com.youssgm3o8.rokidragon.info"
    public static final int FORM_RECIPES = 1984446415; // hash of "com.youssgm3o8.rokidragon.recipes"
    public static final int FORM_CUSTOMIZATION_RESULT = 983648570; // hash of "com.youssgm3o8.rokidragon.customizationresult"
    public static final int FORM_CUSTOMIZATION_ERROR = 1825503147; // hash of "com.youssgm3o8.rokidragon.customizationerror"
    public static final int FORM_PURCHASE_RESULT = 1389003045; // hash of "com.youssgm3o8.rokidragon.purchaseresult"
    public static final int FORM_PURCHASE_ERROR = 1272975774; // hash of "com.youssgm3o8.rokidragon.purchaseerror"
    public static final int FORM_INVENTORY_STORAGE = 1738651224; // hash of "com.youssgm3o8.rokidragon.inventorystorage"
    public static final int FORM_FIRST_NAMING = 571937880; // hash of "com.youssgm3o8.rokidragon.firstnaming"
    
    // Store form responses for processing
    private final Map<String, String> playerSelectedEggs = new HashMap<>();
    
    public FormBasedDragonGUI(DragonPlugin plugin, DatabaseManager databaseManager, 
                           DragonEggManager eggManager, DragonShardManager shardManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.eggManager = eggManager;
        this.shardManager = shardManager;
    }
    
    /**
     * Shows the main menu form to the player
     */
    public void openMainMenu(Player player) {
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.DARK_BLUE + "Dragon Management",
            TextFormat.GOLD + "Welcome to Dragon Management!\n" +
            TextFormat.WHITE + "Choose an option below:"
        );

        // Add buttons for each menu option
        form.addButton(new ElementButton(TextFormat.GREEN + "Dragon Egg Storage",
            new ElementButtonImageData("path", "textures/blocks/chest_front.png")));

        form.addButton(new ElementButton(TextFormat.YELLOW + "Buy Dragon Egg",
            new ElementButtonImageData("path", "textures/items/gold_ingot.png")));

        form.addButton(new ElementButton(TextFormat.RED + "Report Lost Dragon Egg",
            new ElementButtonImageData("path", "textures/items/book_writable.png")));

        form.addButton(new ElementButton(TextFormat.LIGHT_PURPLE + "Dragon Help",
            new ElementButtonImageData("path", "textures/items/book_enchanted.png")));

        player.showFormWindow(form, FORM_MAIN_MENU);
    }
    
    /**
     * Shows the dragon egg storage form
     */
    public void openStorage(Player player) {
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.DARK_BLUE + "Dragon Egg Storage",
            TextFormat.GOLD + "Your stored dragon eggs:" +
            TextFormat.GRAY + "\nClick an egg to retrieve it."
        );
        
        // Get stored eggs from database
        List<Map<String, Object>> storedEggs = databaseManager.getStoredEggsForPlayer(player.getName());
        
        if (storedEggs.isEmpty()) {
            form.setContent(form.getContent() + "\n" + TextFormat.RED + "No eggs stored.");
        } else {
            // Add each egg as a button
            for (Map<String, Object> eggData : storedEggs) {
                String eggId = (String) eggData.get("id");
                String eggName = (String) eggData.get("name");
                String eggType = (String) eggData.get("type");
                
                form.addButton(new ElementButton(
                    TextFormat.GREEN + eggName + 
                    TextFormat.GRAY + " (" + eggType + ")"
                ));
                
                // Store the eggId for this button index
                playerSelectedEggs.put(player.getName() + "_" + (form.getButtons().size() - 1), eggId);
            }
        }
        
        // Add a Store Eggs button
        form.addButton(new ElementButton(TextFormat.YELLOW + "Store Eggs from Inventory"));
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
        
        player.showFormWindow(form, FORM_STORAGE);
    }
    
    /**
     * Shows the inventory eggs that can be stored
     */
    public void openEggInventoryForStorage(Player player) {
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.DARK_BLUE + "Store Dragon Eggs",
            TextFormat.GOLD + "Select a hatched egg from your inventory to store:"
        );
        
        // Clear any previous inventory mappings
        for (String key : new ArrayList<>(playerSelectedEggs.keySet())) {
            if (key.startsWith(player.getName() + "_inv_")) {
                playerSelectedEggs.remove(key);
            }
        }
        
        // Get all eggs from player's inventory
        List<Item> eggItems = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) { // Iterate by slot index
            Item item = player.getInventory().getItem(slot);
            if (eggManager.isDragonEgg(item)) {
                plugin.getLogger().info("[Storage Check] Found potential egg in slot " + slot + ": " + item.getName());

                // Robust Egg ID retrieval
                String eggId = null;
                CompoundTag tag = item.getNamedTag();
                
                // Try all possible tag names
                if (tag != null) {
                    if (tag.contains("dragon_egg_id")) {
                        eggId = tag.getString("dragon_egg_id");
                        plugin.getLogger().info("[Storage Check] Found egg ID via dragon_egg_id tag: " + eggId);
                    } else if (tag.contains("eggId")) {
                        eggId = tag.getString("eggId");
                        plugin.getLogger().info("[Storage Check] Found egg ID via eggId tag: " + eggId);
                    } else if (tag.contains("DragonUUID")) {
                        eggId = tag.getString("DragonUUID");
                        plugin.getLogger().info("[Storage Check] Found egg ID via DragonUUID tag: " + eggId);
                    }
                }
                
                // If still null, try using the eggManager
                if (eggId == null || eggId.isEmpty()) {
                    UUID eggUUID = eggManager.getDragonUUID(item);
                    if (eggUUID != null) {
                        eggId = eggUUID.toString();
                        plugin.getLogger().info("[Storage Check] Found egg ID via eggManager.getDragonUUID: " + eggId);
                    }
                }

                if (eggId != null && !eggId.isEmpty()) {
                    boolean isHatched = databaseManager.isEggHatched(eggId);
                    plugin.getLogger().info("[Storage Check] Egg " + eggId + " in slot " + slot + " - Is Hatched: " + isHatched);
                    if (isHatched) { // Keep the 'hatched only' logic for now
                        eggItems.add(item);
                        plugin.getLogger().info("[Storage Check] Added hatched egg from slot " + slot + " to storage list.");
                    }
                } else {
                    plugin.getLogger().warning("[Storage Check] Could not determine Egg ID for item in slot " + slot);
                }
            }
        }
        
        plugin.getLogger().info("Found " + eggItems.size() + " hatched eggs in " + player.getName() + "'s inventory");
        
        // Check if player has any eggs in inventory
        if (eggItems.isEmpty()) {
            form.setContent(form.getContent() + "\n" + TextFormat.RED + "No hatched dragon eggs found in your inventory.");
        } else {
            // Get existing egg count
            List<Map<String, Object>> allEggs = databaseManager.getAllEggsForPlayer(player.getUniqueId().toString());
            int totalEggs = allEggs.size();
            int maxEggs = DragonEggManager.MAX_EGGS_PER_PLAYER;
            int availableSlots = maxEggs - totalEggs;
            
            // Show available storage slots info
            form.setContent(form.getContent() + "\n" + TextFormat.GRAY + 
                            "Available storage slots: " + TextFormat.WHITE + availableSlots + "/" + maxEggs + 
                            "\n" + TextFormat.YELLOW + "Found " + eggItems.size() + " hatched dragon eggs in your inventory.");
            
            // Add each egg as a button
            for (Item eggItem : eggItems) {
                String dragonType = eggManager.getDragonType(eggItem);
                String dragonName = eggManager.getDragonName(eggItem);
                // Use the getDragonUUID method for consistent retrieval
                UUID eggUUID = eggManager.getDragonUUID(eggItem);
                String eggId = (eggUUID != null) ? eggUUID.toString() : "";
                
                int slot = player.getInventory().first(eggItem);
                form.addButton(new ElementButton(
                    TextFormat.GREEN + dragonName + 
                    TextFormat.DARK_GRAY + " (" + dragonType + ")" +
                    "\n" + TextFormat.YELLOW + "Slot: " + slot
                ));
                
                // Store the egg inventory slot for this button index
                playerSelectedEggs.put(player.getName() + "_inv_" + (form.getButtons().size() - 1), String.valueOf(slot));
                plugin.getLogger().info("Mapped button " + (form.getButtons().size() - 1) + " to inventory slot " + slot + " for " + player.getName());
            }
            
            // Add a Store All button if multiple eggs available and enough space
            if (eggItems.size() > 1 && availableSlots >= eggItems.size()) {
                form.addButton(new ElementButton(TextFormat.GOLD + "Store All Hatched Eggs (" + eggItems.size() + ")"));
            }
        }
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Storage"));
        
        player.showFormWindow(form, FORM_INVENTORY_STORAGE);
    }
    
    /**
     * Shows the purchase confirmation form
     */
    public void openDragonPurchaseMenu(Player player) {
        int price = plugin.getConfig().getInt("economy.dragon_egg_price", 128000);
        
        // Check if economy is available
        if (!isEconomyAvailable()) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.economyNotFound"));
            openMainMenu(player);
            return;
        }
        
        // Get player balance
        double balance = EconomyAPI.getInstance().myMoney(player);
        
        FormWindowModal form = new FormWindowModal(
            TextFormat.DARK_BLUE + "Purchase Dragon Egg",
            TextFormat.GOLD + "Dragon Egg Price: " + TextFormat.GREEN + price + " coins\n" +
            TextFormat.GOLD + "Your Balance: " + TextFormat.GREEN + (int)balance + " coins\n\n" +
            (balance >= price ? 
                TextFormat.GREEN + "You have enough money to purchase a dragon egg!" :
                TextFormat.RED + "You don't have enough money to purchase a dragon egg."),
            TextFormat.GREEN + "Purchase",
            TextFormat.RED + "Cancel"
        );
        
        player.showFormWindow(form, FORM_BUY_EGG);
    }
    
    /**
     * Shows the lost eggs selection menu
     */
    public void openLostEggsMenu(Player player) {
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.DARK_BLUE + "Lost Dragon Eggs",
            TextFormat.GOLD + "Select a dragon egg to report as lost:"
        );
        
        // Get player's eggs from database
        List<Map<String, Object>> playerEggs = databaseManager.getAllEggsForPlayer(player.getUniqueId().toString());
        
        if (playerEggs.isEmpty()) {
            form.setContent(form.getContent() + "\n" + TextFormat.RED + "You don't have any dragon eggs.");
        } else {
            // Add each egg as a button
            for (Map<String, Object> eggData : playerEggs) {
                String eggId = (String) eggData.get("egg_id");
                String eggName = (String) eggData.get("name");
                String eggType = (String) eggData.get("type");
                boolean isHatched = (boolean) eggData.get("is_hatched");
                
                // Check if this egg corresponds to the currently active dragon
                boolean isActive = false;
                if (plugin.hasActiveDragon(player)) {
                    DragonEntity activeDragon = plugin.getActiveDragons().get(player.getUniqueId());
                    if (activeDragon != null && eggId.equals(activeDragon.getDragonId())) {
                        isActive = true;
                    }
                }

                String statusText = "";
                String statusColor = TextFormat.GRAY.toString();
                // Use correct key structure with messages prefix
                String langKey = "messages.lostEgg.menu.egg_status_egg"; // Default key

                if (isActive) {
                    // Use correct key structure
                    langKey = "messages.lostEgg.menu.egg_status_active";
                    statusColor = TextFormat.AQUA.toString();
                } else if (isHatched) {
                    // Use correct key structure
                    langKey = "messages.lostEgg.menu.egg_status_hatched";
                    statusColor = TextFormat.GREEN.toString();
                }

                String retrievedValue = plugin.getLanguageString(langKey);
                // Log still uses the variable langKey, which now holds the new flat key
                plugin.getLogger().info("[Lang Debug] Requesting key: '" + langKey + "', Retrieved value: '" + retrievedValue + "' for player " + player.getName());

                statusText = statusColor + retrievedValue;

                String typeColor = DragonUtils.getColorByType(eggType); // Use utility

                form.addButton(new ElementButton(
                    typeColor + eggName + TextFormat.RESET + " (" + eggType + ")" + "\n" +
                    statusText
                ));
                
                // Store the eggId for this button index
                // Use a consistent prefix for lost egg mapping
                String mapKey = player.getName() + "_lost_" + (form.getButtons().size() - 1); // <<< Key being generated
                playerSelectedEggs.put(mapKey, eggId); // <<< Storing the value

                // *** ADDED LOGGING ***
                plugin.getLogger().info("[Form Debug] Storing in playerSelectedEggs: Key=\'" + mapKey + "\', Value=\'" + eggId + "\'");
                // *** END LOGGING ***
            }
        }
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));

        // *** ADDED LOGGING ***
        plugin.getLogger().info("[Form Debug] Showing FORM_LOST_EGGS to " + player.getName() + ". Current playerSelectedEggs keys for player: " +
                playerSelectedEggs.keySet().stream()
                        .filter(k -> k.startsWith(player.getName() + "_"))
                        .collect(java.util.stream.Collectors.joining(", ")));
        // *** END LOGGING ***

        player.showFormWindow(form, FORM_LOST_EGGS);
    }
    
    /**
     * Shows confirmation for reporting a lost egg
     */
    public void openLostEggConfirmation(Player player, String eggId) {
        // Get egg data
        Map<String, Object> eggData = databaseManager.getEggById(eggId);
        if (eggData == null) {
            player.sendMessage(TextFormat.RED + "Error: Egg data not found.");
            openMainMenu(player);
            return;
        }
        
        String eggName = (String) eggData.get("name");
        String eggType = (String) eggData.get("type");
        boolean isActive = (boolean) eggData.get("is_active");
        boolean isIncubating = databaseManager.isEggIncubating(eggId);
        int incubationProgress = 0;
        
        if (isIncubating) {
            Map<String, String> detailedEggData = databaseManager.getEggData(databaseManager.getPlayerUUIDFromEggId(eggId), eggId);
            if (detailedEggData.containsKey("incubationProgress")) {
                try {
                    incubationProgress = Integer.parseInt(detailedEggData.get("incubationProgress"));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Failed to parse incubation progress: " + detailedEggData.get("incubationProgress"));
                }
            }
        }
        
        // Current location
        String location = player.getLevel().getName() + " (" + 
                         (int)player.getX() + ", " + 
                         (int)player.getY() + ", " + 
                         (int)player.getZ() + ")";
        
        String status;
        if (isActive) {
            status = TextFormat.GREEN + "ACTIVE";
        } else if (isIncubating) {
            status = TextFormat.AQUA + "INCUBATING (" + incubationProgress + "%)";
        } else {
            status = TextFormat.YELLOW + "Inactive";
        }
        
        FormWindowModal form = new FormWindowModal(
            TextFormat.DARK_RED + "Confirm: Report Lost Egg",
            TextFormat.GOLD + "Are you sure you want to report this dragon egg as lost?\n\n" +
            TextFormat.WHITE + "Egg Name: " + TextFormat.AQUA + eggName + "\n" +
            TextFormat.WHITE + "Type: " + TextFormat.AQUA + eggType + "\n" +
            TextFormat.WHITE + "Status: " + status + "\n" +
            TextFormat.WHITE + "Location: " + TextFormat.YELLOW + location + "\n\n" +
            TextFormat.RED + "WARNING: " + TextFormat.WHITE + "This will permanently remove this dragon egg from your account!\n" +
            TextFormat.WHITE + "You will need to purchase a new egg if you want a replacement.",
            TextFormat.RED + "Confirm Report",
            TextFormat.GREEN + "Cancel"
        );
        
        // Store the eggId and location for confirmation
        playerSelectedEggs.put(player.getName() + "_lostEggId", eggId);
        playerSelectedEggs.put(player.getName() + "_lostLocation", location);
        
        player.showFormWindow(form, FORM_LOST_EGG_CONFIRM);
    }
    
    /**
     * Display dragon help information in a form
     */
    private void sendDragonHelp(Player player) {
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.GOLD + "Dragon Help",
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon summon" + TextFormat.WHITE + " - Summon your dragon\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon despawn" + TextFormat.WHITE + " - Despawn your dragon\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon info" + TextFormat.WHITE + " - View info about your dragon\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon name <name>" + TextFormat.WHITE + " - Rename your dragon\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon recipes" + TextFormat.WHITE + " - View dragon recipes\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon lost" + TextFormat.WHITE + " - Report a lost dragon egg\n" +
            TextFormat.WHITE + "• " + TextFormat.YELLOW + "/dragon" + TextFormat.WHITE + " - Open this dragon management menu"
        );
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
        
        // Use a different form ID for help form
        player.showFormWindow(form, FORM_HELP_MENU);
    }
    
    /**
     * Shows dragon information in a form
     */
    public void showDragonInfo(Player player) {
        String playerUUID = player.getUniqueId().toString();
        
        // Check if player has a dragon
        if (!databaseManager.playerHasDragon(playerUUID)) {
            FormWindowSimple form = new FormWindowSimple(
                TextFormat.RED + "No Dragon",
                TextFormat.RED + plugin.getLanguageString("messages.errors.noDragonYet")
            );
            form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
            player.showFormWindow(form, FORM_INFO);
            return;
        }
        
        // Get dragon info
        String eggId = databaseManager.getDragonEggId(playerUUID);
        if (eggId == null) {
            FormWindowSimple form = new FormWindowSimple(
                TextFormat.RED + "Error",
                TextFormat.RED + plugin.getLanguageString("messages.errors.genericError")
            );
            form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
            player.showFormWindow(form, FORM_INFO);
            return;
        }
        
        String dragonType = databaseManager.getDragonType(eggId);
        String dragonName = databaseManager.getDragonName(eggId);
        
        // Build info content
        StringBuilder content = new StringBuilder();
        content.append(TextFormat.GREEN + "Dragon Name: " + 
            DragonUtils.getColorByType(dragonType) + dragonName + TextFormat.RESET + "\n\n");
        content.append(TextFormat.GREEN + "Dragon Type: " + 
            DragonUtils.getColorByType(dragonType) + dragonType + TextFormat.RESET + "\n\n");
        
        // Add health info if dragon is currently spawned
        if (plugin.getActiveDragons().containsKey(player.getUniqueId())) {
            com.youssgm3o8.rokidragon.dragon.DragonEntity dragon = plugin.getActiveDragons().get(player.getUniqueId());
            float currentHealth = dragon.getHealth();
            float maxHealth = dragon.getMaxHealth();
            content.append(TextFormat.GREEN + "Health: " + TextFormat.WHITE + 
                (int)currentHealth + "/" + (int)maxHealth + "\n\n");
            content.append(TextFormat.GREEN + "Status: " + TextFormat.AQUA + "Summoned\n\n");
        } else {
            content.append(TextFormat.GREEN + "Status: " + TextFormat.GRAY + "Not Summoned\n\n");
        }
        
        // Create form
        FormWindowSimple form = new FormWindowSimple(
            TextFormat.GOLD + "Dragon Information",
            content.toString()
        );
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
        
        player.showFormWindow(form, FORM_INFO);
    }
    
    /**
     * Shows dragon recipes in a form
     */
    public void showRecipesForm(Player player) {
        // Get language strings for recipes
        String title = plugin.getLanguageString("messages.recipes.title");
        String header = plugin.getLanguageString("messages.recipes.header");
        String footer = plugin.getLanguageString("messages.recipes.footer");
        String separator = plugin.getLanguageString("messages.recipes.separator");
        String shardPattern = plugin.getLanguageString("messages.recipes.shard_pattern");

        // Build content for recipes
        StringBuilder content = new StringBuilder();
        content.append(header + "\n\n");
        
        // Fire shard recipe
        String fireName = plugin.getLanguageString("messages.recipes.fire_shard.name");
        String fireDesc = plugin.getLanguageString("messages.recipes.fire_shard.description");
        String fireIngredients = plugin.getLanguageString("messages.recipes.fire_shard.ingredients");
        String fireIngredients2 = plugin.getLanguageString("messages.recipes.fire_shard.ingredients2");
        
        content.append(fireName + "\n");
        content.append(fireDesc + "\n");
        content.append(fireIngredients + "\n");
        content.append(fireIngredients2 + "\n");
        content.append(shardPattern + "\n\n");
        content.append(separator + "\n\n");
        
        // Ice shard recipe
        String iceName = plugin.getLanguageString("messages.recipes.ice_shard.name");
        String iceDesc = plugin.getLanguageString("messages.recipes.ice_shard.description");
        String iceIngredients = plugin.getLanguageString("messages.recipes.ice_shard.ingredients");
        String iceIngredients2 = plugin.getLanguageString("messages.recipes.ice_shard.ingredients2");
        
        content.append(iceName + "\n");
        content.append(iceDesc + "\n");
        content.append(iceIngredients + "\n");
        content.append(iceIngredients2 + "\n");
        content.append(shardPattern + "\n\n");
        content.append(separator + "\n\n");
        
        // Lightning shard recipe
        String lightningName = plugin.getLanguageString("messages.recipes.lightning_shard.name");
        String lightningDesc = plugin.getLanguageString("messages.recipes.lightning_shard.description");
        String lightningIngredients = plugin.getLanguageString("messages.recipes.lightning_shard.ingredients");
        String lightningIngredients2 = plugin.getLanguageString("messages.recipes.lightning_shard.ingredients2");
        
        content.append(lightningName + "\n");
        content.append(lightningDesc + "\n");
        content.append(lightningIngredients + "\n");
        content.append(lightningIngredients2 + "\n");
        content.append(shardPattern + "\n\n");
        content.append(separator + "\n\n");
        
        // Dragon Loaf recipe
        String loafName = plugin.getLanguageString("messages.recipes.dragon_loaf.name");
        String loafDesc = plugin.getLanguageString("messages.recipes.dragon_loaf.description");
        String loafIngredients = plugin.getLanguageString("messages.recipes.dragon_loaf.ingredients");
        String loafIngredients2 = plugin.getLanguageString("messages.recipes.dragon_loaf.ingredients2");
        String loafIngredients3 = plugin.getLanguageString("messages.recipes.dragon_loaf.ingredients3");
        
        content.append(loafName + "\n");
        content.append(loafDesc + "\n");
        content.append(loafIngredients + "\n");
        content.append(loafIngredients2 + "\n");
        content.append(loafIngredients3 + "\n\n");
        content.append(footer);
        
        // Create form
        FormWindowSimple form = new FormWindowSimple(
            title,
            content.toString()
        );
        
        // Add a back button
        form.addButton(new ElementButton(TextFormat.RED + "Back to Main Menu"));
        
        player.showFormWindow(form, FORM_RECIPES);
    }
    
    /**
     * Handle form responses from the player
     */
    public void handleFormResponse(Player player, int formId, Object response) {
        // Log before the null check, especially for the naming form
        if (formId == FORM_FIRST_NAMING) {
            plugin.getLogger().info("[Form Debug] Received response for FORM_FIRST_NAMING from " + player.getName() + ". Response object: " + (response == null ? "null" : response.getClass().getName()));
        }

        // Skip if player closed the form without response
        if (response == null) {
            plugin.getLogger().warning("[Form Debug] Received null response for formId " + formId + " from player " + player.getName() + ". Form likely closed by player.");
            return;
        }
        
        switch (formId) {
            case FORM_MAIN_MENU:
                handleMainMenuResponse(player, (int) response);
                break;
                
            case FORM_STORAGE:
                handleStorageResponse(player, (int) response);
                break;
                
            case FORM_BUY_EGG:
                handlePurchaseResponse(player, (boolean) response);
                break;
                
            case FORM_LOST_EGGS:
                handleLostEggsResponse(player, (int) response);
                break;
                
            case FORM_LOST_EGG_CONFIRM:
                handleLostEggConfirmResponse(player, (boolean) response);
                break;
                
            case FORM_INVENTORY_STORAGE:
                handleInventoryStorageResponse(player, (int) response);
                break;
                
            case FORM_HELP_MENU:
                // Since help menu only has one button (back to main menu)
                openMainMenu(player);
                break;
                
            case FORM_INFO:
                // Since info form only has one button (back to main menu)
                openMainMenu(player);
                break;
                
            case FORM_RECIPES:
                // Since recipes form only has one button (back to main menu)
                openMainMenu(player);
                break;
                
            case FORM_PURCHASE_RESULT:
                // Handle purchase result
                openMainMenu(player);
                break;
                
            case FORM_PURCHASE_ERROR:
                // Handle purchase error
                openMainMenu(player);
                break;
                
            case FORM_FIRST_NAMING:
                // Check response type before casting
                if (response instanceof FormResponseCustom) {
                    handleFirstNamingResponse(player, (FormResponseCustom) response);
                } else {
                    plugin.getLogger().error("[Form Debug] Received unexpected response type for FORM_FIRST_NAMING: " + response.getClass().getName());
                }
                break;
        }
    }
    
    /**
     * Process main menu button clicks
     */
    private void handleMainMenuResponse(Player player, int buttonId) {
        switch (buttonId) {
            case 0: // Dragon Egg Storage
                openStorage(player);
                break;
                
            case 1: // Buy Dragon Egg
                openDragonPurchaseMenu(player);
                break;
                
            case 2: // Report Lost Dragon Egg
                openLostEggsMenu(player);
                break;
                
            case 3: // Dragon Help
                sendDragonHelp(player);
                break;
        }
    }
    
    /**
     * Process storage menu button clicks
     */
    private void handleStorageResponse(Player player, int buttonId) {
        List<Map<String, Object>> storedEggs = databaseManager.getStoredEggsForPlayer(player.getName());
        
        // Check if it's the store eggs button
        if (buttonId == storedEggs.size()) {
            openEggInventoryForStorage(player);
            return;
        }
        
        // Check if it's the back button (last button)
        if (buttonId > storedEggs.size()) {
            openMainMenu(player);
            return;
        }
        
        // Get the egg ID from the stored map
        String eggId = playerSelectedEggs.get(player.getName() + "_" + buttonId);
        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "Error: Could not find the selected egg.");
            openStorage(player);
            return;
        }
        
        // Retrieve the egg from storage
        Item eggItem = eggManager.retrieveEggFromStorage(player, eggId);
        if (eggItem != null) {
            // Add the egg to player's inventory
            if (player.getInventory().canAddItem(eggItem)) {
                player.getInventory().addItem(eggItem);
                player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggRetrieved"));
            } else {
                player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.inventoryFull"));
                databaseManager.storeEgg(player.getName(), eggId); // Put it back in storage
            }
        }
        
        // Refresh the storage view
        openStorage(player);
    }
    
    /**
     * Process inventory egg selection for storage
     */
    private void handleInventoryStorageResponse(Player player, int buttonId) {
        // Check if it's a "Store All" button
        if (buttonId == player.getInventory().getSize()) {
            plugin.getLogger().info("Player " + player.getName() + " clicked Store All button");
            
            // Track the results
            int storedCount = 0;
            int skippedCount = 0;
            List<Item> eggItems = new ArrayList<>();
            
            // Find all dragon eggs in the inventory
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                Item item = player.getInventory().getItem(slot);
                if (item != null && eggManager.isDragonEgg(item)) {
                    eggItems.add(item);
                }
            }
            
            if (eggItems.isEmpty()) {
                player.sendMessage(TextFormat.YELLOW + "No dragon eggs found in your inventory.");
                openStorage(player);
                return;
            }
            
            // Process each egg
            for (Item eggItem : eggItems) {
                String eggId = null;
                CompoundTag tag = eggItem.getNamedTag();
                
                // Try all possible tag names
                if (tag != null) {
                    if (tag.contains("dragon_egg_id")) {
                        eggId = tag.getString("dragon_egg_id");
                    } else if (tag.contains("eggId")) {
                        eggId = tag.getString("eggId");
                    } else if (tag.contains("DragonUUID")) {
                        eggId = tag.getString("DragonUUID");
                    }
                }
                
                // If still null, try using the eggManager
                if (eggId == null || eggId.isEmpty()) {
                    UUID eggUUID = eggManager.getDragonUUID(eggItem);
                    if (eggUUID != null) {
                        eggId = eggUUID.toString();
                    }
                }
                
                // Skip invalid eggs
                if (eggId == null || eggId.isEmpty()) {
                    skippedCount++;
                    continue;
                }
                
                // Skip incubating eggs
                if (databaseManager.isEggIncubating(eggId)) {
                    skippedCount++;
                    continue;
                }
                
                // Skip unhatched eggs
                if (!databaseManager.isEggHatched(eggId)) {
                    skippedCount++;
                    continue;
                }
                
                // Store the egg
                try {
                    storeEggFromInventory(player, eggItem);
                    storedCount++;
                } catch (Exception e) {
                    plugin.getLogger().error("Error storing egg " + eggId + ": " + e.getMessage());
                    skippedCount++;
                }
            }
            
            // Send result message to player
            if (storedCount > 0) {
                player.sendMessage(TextFormat.GREEN + "Successfully stored " + storedCount + " dragon egg(s).");
                if (skippedCount > 0) {
                    player.sendMessage(TextFormat.YELLOW + "Skipped " + skippedCount + " egg(s) that were not valid for storage (unhatched or incubating).");
                }
            } else {
                player.sendMessage(TextFormat.YELLOW + "No valid eggs were found for storage. Eggs must be hatched and not incubating.");
            }
            
            openStorage(player);
            return;
        }
        
        // Check if it's the back button (last button)
        if (buttonId >= player.getInventory().getSize() + 1) {
            openStorage(player);
            return;
        }
        
        String slotString = playerSelectedEggs.get(player.getName() + "_inv_" + buttonId);
        plugin.getLogger().info("Player " + player.getName() + " clicked button " + buttonId + ", slot mapping: " + slotString);
        
        if (slotString == null) {
            player.sendMessage(TextFormat.RED + "Error: Could not find the selected egg. Please try again.");
            openStorage(player);
            return;
        }
        
        try {
            int slot = Integer.parseInt(slotString);
            plugin.getLogger().info("Retrieving item from slot " + slot + " for " + player.getName());
            
            Item eggItem = player.getInventory().getItem(slot);
            
            if (eggItem == null) {
                player.sendMessage(TextFormat.RED + "Error: No item found in the selected slot.");
                openStorage(player);
                return;
            }
            
            if (!eggManager.isDragonEgg(eggItem)) {
                player.sendMessage(TextFormat.RED + "Error: The selected item is not a dragon egg.");
                openStorage(player);
                return;
            }
            
            // Get egg ID using multiple approaches
            String eggId = null;
            CompoundTag tag = eggItem.getNamedTag();
            
            // Try all possible tag names
            if (tag != null) {
                if (tag.contains("dragon_egg_id")) {
                    eggId = tag.getString("dragon_egg_id");
                    plugin.getLogger().info("Found egg ID via dragon_egg_id tag: " + eggId);
                } else if (tag.contains("eggId")) {
                    eggId = tag.getString("eggId");
                    plugin.getLogger().info("Found egg ID via eggId tag: " + eggId);
                } else if (tag.contains("DragonUUID")) {
                    eggId = tag.getString("DragonUUID");
                    plugin.getLogger().info("Found egg ID via DragonUUID tag: " + eggId);
                }
            }
            
            // If still null, try using the eggManager
            if (eggId == null || eggId.isEmpty()) {
                UUID eggUUID = eggManager.getDragonUUID(eggItem);
                if (eggUUID != null) {
                    eggId = eggUUID.toString();
                    plugin.getLogger().info("Found egg ID via eggManager.getDragonUUID: " + eggId);
                }
            }
            
            // Final verification
            if (eggId == null || eggId.isEmpty()) {
                player.sendMessage(TextFormat.RED + "Error: Could not identify the dragon egg.");
                plugin.getLogger().warning("Failed to identify egg ID for player " + player.getName() + " in slot " + slot);
                openStorage(player);
                return;
            }
            
            // Verify the egg is hatched
            boolean isHatched = databaseManager.isEggHatched(eggId);
            plugin.getLogger().info("Checking if egg " + eggId + " is hatched: " + isHatched);
            
            if (!isHatched) {
                player.sendMessage(TextFormat.RED + "Error: Only hatched dragon eggs can be stored.");
                openStorage(player);
                return;
            }
            
            // Store the egg
            plugin.getLogger().info("Storing hatched egg from slot " + slot + " for " + player.getName());
            storeEggFromInventory(player, eggItem);
            openStorage(player);
        } catch (Exception e) {
            plugin.getLogger().error("Error handling inventory storage response: " + e.getMessage(), e);
            player.sendMessage(TextFormat.RED + "Error processing egg selection.");
            openStorage(player);
        }
    }
    
    /**
     * Process purchase confirmation
     */
    private void handlePurchaseResponse(Player player, boolean confirmed) {
        if (!confirmed) {
            // Show cancellation message in a form
            FormWindowModal resultForm = new FormWindowModal(
                TextFormat.YELLOW + "Purchase Cancelled",
                TextFormat.YELLOW + plugin.getLanguageString("messages.success.purchaseCancelled"),
                TextFormat.AQUA + "Back to Main Menu",
                TextFormat.RED + "Close"
            );
            player.showFormWindow(resultForm, FORM_PURCHASE_RESULT);
            return;
        }
        
        int price = plugin.getConfig().getInt("economy.dragon_egg_price", 128000);
        
        // Check if economy is available
        if (!isEconomyAvailable()) {
            // Show error in a form
            FormWindowModal errorForm = new FormWindowModal(
                TextFormat.RED + "Economy Not Available",
                TextFormat.RED + plugin.getLanguageString("messages.errors.economyNotFound"),
                TextFormat.AQUA + "Back to Main Menu",
                TextFormat.RED + "Close"
            );
            player.showFormWindow(errorForm, FORM_PURCHASE_ERROR);
            return;
        }
        
        // Check player balance
        double balance = EconomyAPI.getInstance().myMoney(player);
        if (balance < price) {
            // Show not enough money error in a form
            FormWindowModal errorForm = new FormWindowModal(
                TextFormat.RED + "Insufficient Funds",
                TextFormat.RED + plugin.getLanguageString("messages.errors.notEnoughMoney"),
                TextFormat.AQUA + "Back to Main Menu",
                TextFormat.RED + "Close"
            );
            player.showFormWindow(errorForm, FORM_PURCHASE_ERROR);
            return;
        }
        
        // Get all eggs owned by player (including stored and regular dragon eggs)
        List<Map<String, Object>> allEggs = databaseManager.getAllEggsForPlayer(player.getUniqueId().toString());
        int totalEggs = allEggs.size();
        int maxEggs = DragonEggManager.MAX_EGGS_PER_PLAYER;
        
        // Check total egg limit (including all types of eggs)
        if (totalEggs >= maxEggs) {
            // Show egg limit error in a form
            FormWindowModal errorForm = new FormWindowModal(
                TextFormat.RED + "Egg Limit Reached",
                TextFormat.RED + "You have reached the maximum limit of " + maxEggs + " dragon eggs.",
                TextFormat.AQUA + "Back to Main Menu",
                TextFormat.RED + "Close"
            );
            player.showFormWindow(errorForm, FORM_PURCHASE_ERROR);
            return;
        }
        
        // Process purchase
        if (EconomyAPI.getInstance().reduceMoney(player, price) == EconomyAPI.RET_SUCCESS) {
            // Create and give egg
            Item eggItem = eggManager.createNewDragonEgg(player);
            if (player.getInventory().canAddItem(eggItem)) {
                player.getInventory().addItem(eggItem);
                
                // Show success message in a form
                FormWindowModal resultForm = new FormWindowModal(
                    TextFormat.GREEN + "Egg Purchased",
                    TextFormat.GREEN + plugin.getLanguageString("messages.success.eggPurchased", price),
                    TextFormat.AQUA + "Back to Main Menu",
                    TextFormat.RED + "Close"
                );
                player.showFormWindow(resultForm, FORM_PURCHASE_RESULT);
            } else {
                // Store the egg if inventory is full
                String eggId = eggItem.getNamedTag().getString("dragon_egg_id");
                databaseManager.storeEgg(player.getName(), eggId);
                
                // Show stored notification in a form
                FormWindowModal resultForm = new FormWindowModal(
                    TextFormat.YELLOW + "Egg Stored",
                    TextFormat.YELLOW + plugin.getLanguageString("messages.success.eggStoredFull"),
                    TextFormat.AQUA + "Back to Main Menu",
                    TextFormat.RED + "Close"
                );
                player.showFormWindow(resultForm, FORM_PURCHASE_RESULT);
            }
        } else {
            // Show purchase failed error in a form
            FormWindowModal errorForm = new FormWindowModal(
                TextFormat.RED + "Purchase Failed",
                TextFormat.RED + plugin.getLanguageString("messages.errors.purchaseFailed"),
                TextFormat.AQUA + "Back to Main Menu",
                TextFormat.RED + "Close"
            );
            player.showFormWindow(errorForm, FORM_PURCHASE_ERROR);
        }
    }
    
    /**
     * Process lost eggs menu button clicks
     */
    private void handleLostEggsResponse(Player player, int buttonId) {
        // *** ADDED LOGGING ***
        plugin.getLogger().info("[Form Debug] Handling FORM_LOST_EGGS response for " + player.getName() + ", buttonId=" + buttonId);
        plugin.getLogger().info("[Form Debug] Current playerSelectedEggs keys for player before lookup: " +
                playerSelectedEggs.keySet().stream()
                        .filter(k -> k.startsWith(player.getName() + "_"))
                        .collect(java.util.stream.Collectors.joining(", ")));
        // *** END LOGGING ***

        List<Map<String, Object>> playerEggs = databaseManager.getAllEggsForPlayer(player.getUniqueId().toString());
        
        // Check if it's the back button (last button)
        if (buttonId >= playerEggs.size()) {
            plugin.getLogger().info("[Form Debug] Button ID " + buttonId + " is >= egg count " + playerEggs.size() + ", treating as Back button."); // Log back button case
            openMainMenu(player);
            return;
        }

        // Get the egg ID from the stored map
        // The key format used in openLostEggsMenu was: player.getName() + "_lost_" + buttonIndex
        String mapKey = player.getName() + "_lost_" + buttonId; // <<< Reconstruct the key

        // *** ADDED LOGGING ***
        plugin.getLogger().info("[Form Debug] Attempting lookup with Key=\'" + mapKey + "\'");
        // *** END LOGGING ***

        String eggId = playerSelectedEggs.get(mapKey); // <<< Retrieve from map

        // *** ADDED LOGGING ***
        plugin.getLogger().info("[Form Debug] Lookup result for Key=\'" + mapKey + "\': Value=\'" + eggId + "\'");
        // *** END LOGGING ***

        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "Error: Could not find the selected egg."); // <<< This is the error message
            plugin.getLogger().warning("[Form Debug] Egg ID is null for Key=\'" + mapKey + "\'"); // Log the error case
            openLostEggsMenu(player);
            return;
        }

        // Open confirmation for the selected egg
        openLostEggConfirmation(player, eggId);
    }
    
    /**
     * Process lost egg confirmation
     */
    private void handleLostEggConfirmResponse(Player player, boolean confirmed) {
        if (!confirmed) {
            player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.success.reportCancelled"));
            openLostEggsMenu(player);
            return;
        }
        
        // Get the egg ID and location from stored values
        String eggId = playerSelectedEggs.remove(player.getName() + "_lostEggId");
        String location = playerSelectedEggs.remove(player.getName() + "_lostLocation");
        
        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "Error: Lost egg data not found.");
            openMainMenu(player);
            return;
        }
        
        // Process the report
        Map<String, Object> eggData = databaseManager.getEggById(eggId);
        if (eggData == null) {
            player.sendMessage(TextFormat.RED + "Error: Egg data not found.");
            openMainMenu(player);
            return;
        }
        
        String eggName = (String) eggData.get("name");
        String eggType = (String) eggData.get("type");
        boolean isActive = (boolean) eggData.get("is_active");
        boolean isIncubating = databaseManager.isEggIncubating(eggId);
        
        // Log the lost egg report
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        
        databaseManager.logLostEgg(player.getName(), eggId, eggName, eggType, location, timestamp);
        
        // If the egg is incubating, explicitly stop incubation
        if (isIncubating) {
            databaseManager.setEggIncubating(eggId, false);
        }
        
        // Remove the egg from the player's account
        if (isActive) {
            // Despawn active dragon if needed
            plugin.despawnDragon(player);
        }
        
        databaseManager.deleteEgg(eggId);

        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggReportedLost"));
        openMainMenu(player);
    }
    
    /**
     * Utility method to check if economy is available
     */
    private boolean isEconomyAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("EconomyAPI") != null;
    }
    
    /**
     * Store an egg from player's inventory into storage
     */
    public void storeEggFromInventory(Player player, Item eggItem) {
        plugin.getLogger().info("Attempting to store egg for " + player.getName());
        
        if (eggItem == null) {
            player.sendMessage(TextFormat.RED + "Error: Null egg item.");
            plugin.getLogger().warning("Null egg item when trying to store for player " + player.getName());
            return;
        }
        
        if (!eggManager.isDragonEgg(eggItem)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.invalidEgg"));
            plugin.getLogger().warning("Item is not a dragon egg for player " + player.getName());
            return;
        }
        
        // Get egg ID using multiple approaches
        String eggId = null;
        CompoundTag tag = eggItem.getNamedTag();
        
        // Try all possible tag names
        if (tag != null) {
            if (tag.contains("dragon_egg_id")) {
                eggId = tag.getString("dragon_egg_id");
                plugin.getLogger().info("Found egg ID via dragon_egg_id tag: " + eggId);
            } else if (tag.contains("eggId")) {
                eggId = tag.getString("eggId");
                plugin.getLogger().info("Found egg ID via eggId tag: " + eggId);
            } else if (tag.contains("DragonUUID")) {
                eggId = tag.getString("DragonUUID");
                plugin.getLogger().info("Found egg ID via DragonUUID tag: " + eggId);
            }
        }
        
        // If still null, try using the eggManager
        if (eggId == null || eggId.isEmpty()) {
            UUID eggUUID = eggManager.getDragonUUID(eggItem);
            if (eggUUID != null) {
                eggId = eggUUID.toString();
                plugin.getLogger().info("Found egg ID via eggManager.getDragonUUID: " + eggId);
            }
        }
        
        // Final verification
        if (eggId == null || eggId.isEmpty()) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.invalidEgg"));
            plugin.getLogger().warning("Failed to identify egg ID for player " + player.getName());
            return;
        }
        
        plugin.getLogger().info("Found valid dragon egg with ID: " + eggId + " for player " + player.getName());
        
        // Check if the egg is incubating
        if (databaseManager.isEggIncubating(eggId)) {
            player.sendMessage(TextFormat.RED + "You cannot store an egg that is currently incubating.");
            plugin.getLogger().info("Player " + player.getName() + " tried to store an incubating egg: " + eggId);
            return;
        }
        
        // Check if the egg is hatched - we ONLY want to store hatched eggs
        if (!databaseManager.isEggHatched(eggId)) {
            player.sendMessage(TextFormat.RED + "You can only store hatched dragon eggs. This egg hasn't hatched yet.");
            plugin.getLogger().info("Player " + player.getName() + " tried to store an unhatched egg: " + eggId);
            return;
        }
        
        // Check total egg limit (including all types of eggs)
        List<Map<String, Object>> allEggs = databaseManager.getAllEggsForPlayer(player.getUniqueId().toString());
        int totalEggs = allEggs.size();
        int maxEggs = DragonEggManager.MAX_EGGS_PER_PLAYER;
        
        plugin.getLogger().info("Player " + player.getName() + " has " + totalEggs + "/" + maxEggs + " eggs");
        
        if (totalEggs >= maxEggs) {
            player.sendMessage(TextFormat.RED + "You have reached the maximum limit of " + maxEggs + " dragon eggs.");
            return;
        }
        
        // Store the egg in database
        if (databaseManager.storeEgg(player.getName(), eggId)) {
            // Create a copy of the egg item for safe removal
            Item itemToRemove = eggItem.clone();
            itemToRemove.setCount(1);
            
            // Remove egg from inventory
            player.getInventory().removeItem(itemToRemove);
            player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggStored"));
            plugin.getLogger().info("Successfully stored hatched egg " + eggId + " for player " + player.getName());
        } else {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.storeFailed"));
            plugin.getLogger().warning("Failed to store hatched egg " + eggId + " for player " + player.getName());
        }
    }

    /**
     * Opens the first naming form for a newly hatched dragon.
     * This is shown the first time a player summons a hatched dragon.
     * @param player The player who owns the dragon
     * @param eggId The dragon egg ID
     */
    public void openFirstNamingForm(Player player, String eggId) {
        plugin.getLogger().info("Opening first naming form for egg " + eggId + " and player " + player.getName());
        
        // Get dragon type for context
        String dragonType = plugin.getDatabaseManager().getDragonType(eggId);
        if (dragonType == null) {
            dragonType = "Dragon";
        }
        String typeColor = DragonUtils.getColorByType(dragonType);
        
        // Create custom form for naming
        FormWindowCustom form = new FormWindowCustom(typeColor + "Name Your " + dragonType);
        
        // Add dragon type info
        form.addElement(new ElementLabel("§aYour " + dragonType + " has hatched!\n§eChoose a name for your new companion:"));
        
        // Add text input for name with a default
        form.addElement(new ElementInput("Dragon Name", "Enter name here...", "Dragon"));
        
        // Store eggId for response handling
        playerSelectedEggs.put(player.getName() + "_namingEggId", eggId);
        
        // Show the form
        player.showFormWindow(form, FORM_FIRST_NAMING);
    }
    
    /**
     * Handle response from the first naming form.
     */
    public void handleFirstNamingResponse(Player player, FormResponseCustom response) {
        if (response == null) {
            player.sendMessage(TextFormat.YELLOW + "You need to name your dragon before summoning it.");
            return;
        }
        
        String eggId = playerSelectedEggs.remove(player.getName() + "_namingEggId");
        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "Error: Could not find egg data for naming.");
            return;
        }
        
        // Get the entered name
        String name = response.getInputResponse(1);
        if (name == null || name.trim().isEmpty()) {
            name = "Dragon"; // Default name if empty
        }
        
        // Validate name length
        int minNameLength = plugin.getConfig().getInt("dragon_settings.min_name_length", 3);
        int maxNameLength = plugin.getConfig().getInt("dragon_settings.max_name_length", 16);
        
        if (name.length() < minNameLength || name.length() > maxNameLength) {
            player.sendMessage(TextFormat.RED + MessageFormat.format(
                plugin.getLanguageString("messages.errors.nameLength"),
                minNameLength, maxNameLength
            ));
            // Re-open the form if name is invalid
            openFirstNamingForm(player, eggId);
            return;
        }
        
        // Update the dragon name in database
        plugin.getDatabaseManager().setDragonName(eggId, name);
        player.sendMessage(TextFormat.GREEN + MessageFormat.format(
            plugin.getLanguageString("messages.success.dragonRenamed"),
            name
        ));
        
        // Now handle summoning the newly named dragon
        plugin.getLogger().info("First-time dragon naming completed for " + eggId + ", name: " + name);
        
        // Give initial dragon shards as a gift for first hatched dragon
        String dragonTypeForShards = plugin.getDatabaseManager().getDragonType(eggId);
        if (dragonTypeForShards != null) {
            plugin.getShardManager().giveInitialShards(dragonTypeForShards, player);
        }
        
        // Now summon the dragon after naming and giving shards
        String dragonType = plugin.getDatabaseManager().getDragonType(eggId);
        String dragonName = plugin.getDatabaseManager().getDragonName(eggId);

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
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.summonFailed"));
            player.sendMessage(TextFormat.YELLOW + "Try moving to a different location or relogging.");
        }
    }
} 