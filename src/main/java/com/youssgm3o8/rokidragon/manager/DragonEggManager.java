package com.youssgm3o8.rokidragon.manager; // Updated package declaration

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.dragon.DragonEntity;
import com.youssgm3o8.rokidragon.data.DatabaseManager;
import com.youssgm3o8.rokidragon.util.DragonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the creation, storage, retrieval, and NBT data of Dragon Egg items.
 */
public class DragonEggManager {
    private final DragonPlugin plugin;
    private final DatabaseManager databaseManager;
    
    // Maximum number of eggs a player can have
    public static final int MAX_EGGS_PER_PLAYER = 5;

    /**
     * Constructs a new DragonEggManager.
     * @param plugin The main plugin instance.
     */
    public DragonEggManager(DragonPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * Creates a new Dragon Egg item with appropriate NBT data and lore.
     *
     * @param dragonType The type of dragon (e.g., "Fire Dragon").
     * @param dragonName The custom name of the dragon.
     * @param dragonUUID The unique ID associated with this dragon/egg.
     * @return The created Dragon Egg Item.
     */
    public Item createDragonEgg(String dragonType, String dragonName, UUID dragonUUID) {
        return createDragonEgg(dragonType, dragonName, dragonUUID, null);
    }
    
    public Item createDragonEgg(String dragonType, String dragonName, UUID dragonUUID, UUID ownerUUID) {
        Item egg = Item.get(Item.DRAGON_EGG);
        
        // Get the display name from language file
        String baseDisplayName = plugin.getLanguageString("messages.egg.name");
        
        // Set custom name based on dragon type with proper formatting
        String displayName = baseDisplayName;
        
        egg.setCustomName(displayName);
        
        // Create NBT data
        CompoundTag tag = new CompoundTag()
                .putString("DragonType", dragonType)
                .putString("DragonName", dragonName)
                .putString("DragonUUID", dragonUUID.toString())
                .putString("eggId", dragonUUID.toString())
                .putBoolean("IsDragonEgg", true);
        
        // Add owner UUID if provided
        if (ownerUUID != null) {
            tag.putString("OwnerUUID", ownerUUID.toString());
        }
        
        egg.setNamedTag(tag);
        
        // Update lore to ensure it's consistent
        updateEggLore(egg, dragonUUID.toString());
        
        return egg;
    }
    
    private String getColorForType(String dragonType) {
        switch (dragonType) {
            case "Fire Dragon":
                return TextFormat.RED.toString();
            case "Ice Dragon":
                return TextFormat.AQUA.toString();
            case "Lightning Dragon":
                return TextFormat.YELLOW.toString();
            default:
                return TextFormat.WHITE.toString();
        }
    }

    /**
     * Checks if the given item is a valid Dragon Egg managed by this plugin.
     * @param item The item to check.
     * @return {@code true} if it's a valid Dragon Egg, {@code false} otherwise.
     */
    public boolean isDragonEgg(Item item) {
        return item != null && // Added null check for safety
               item.getId() == Item.DRAGON_EGG &&
               item.hasCompoundTag() &&
               item.getNamedTag().getBoolean("IsDragonEgg");
    }

    /**
     * Gets the dragon type stored in the NBT data of a Dragon Egg item.
     * @param item The Dragon Egg item.
     * @return The dragon type string, or null if the item is not a valid Dragon Egg.
     */
    public String getDragonType(Item item) {
        if (!isDragonEgg(item)) {
            return null;
        }
        return item.getNamedTag().getString("DragonType");
    }

    /**
     * Gets the dragon name stored in the NBT data of a Dragon Egg item.
     * @param item The Dragon Egg item.
     * @return The dragon name string, or null if the item is not a valid Dragon Egg.
     */
    public String getDragonName(Item item) {
        if (!isDragonEgg(item)) {
            return null;
        }
        return item.getNamedTag().getString("DragonName");
    }

    /**
     * Gets the unique dragon UUID stored in the NBT data of a Dragon Egg item.
     * @param item The Dragon Egg item.
     * @return The UUID, or null if the item is not a valid Dragon Egg or the UUID is malformed.
     */
    public UUID getDragonUUID(Item item) {
        if (!isDragonEgg(item)) {
            return null;
        }

        try {
            // Ensure the key exists before trying to parse
            if (item.getNamedTag().contains("DragonUUID")) {
                return UUID.fromString(item.getNamedTag().getString("DragonUUID"));
            } else {
                 plugin.getLogger().warning("Missing DragonUUID tag in egg NBT.");
                 return null;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to parse DragonUUID from egg NBT: " + item.getNamedTag().getString("DragonUUID"));
            return null;
        }
    }
    
    /**
     * Checks if a player can store more eggs based on the defined limit.
     * @param player The player to check.
     * @return {@code true} if the player can store more eggs, {@code false} otherwise.
     */
    public boolean canPlayerStoreMoreEggs(Player player) {
        return databaseManager.getPlayerStoredEggCount(player.getUniqueId()) < MAX_EGGS_PER_PLAYER;
    }
    
    /**
     * Stores a Dragon Egg item in the database for the specified player.
     * Checks for validity, corruption, and storage limits before storing.
     *
     * @param player  The player storing the egg.
     * @param eggItem The Dragon Egg item to store.
     * @return {@code true} if the egg was successfully stored, {@code false} otherwise.
     */
    public boolean storeEgg(Player player, Item eggItem) {
        if (!isDragonEgg(eggItem)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.invalidEgg")); // Use lang key
            return false;
        }

        String dragonType = getDragonType(eggItem);
        String dragonName = getDragonName(eggItem);
        UUID dragonUUID = getDragonUUID(eggItem);

        if (dragonType == null || dragonName == null || dragonUUID == null) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.corruptedEgg")); // Use lang key
            return false;
        }

        // Check if player has reached the egg limit
        if (!canPlayerStoreMoreEggs(player)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.maxEggs", MAX_EGGS_PER_PLAYER));
            return false;
        }

        // Store the egg in the database
        boolean success = databaseManager.storeEgg(player.getUniqueId(), dragonUUID, dragonType, dragonName);

        if (success) {
            player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggStored",
                    getColorForType(dragonType) + dragonName + TextFormat.GREEN)); // Use lang key with param
            return true;
        } else {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.storeFailed")); // Use lang key
            return false;
        }
    }
    
    /**
     * Retrieves a list of stored eggs for a specific player from the database.
     * @param player The player whose stored eggs to retrieve.
     * @return A list of maps, where each map represents an egg's data.
     */
    public List<Map<String, Object>> getStoredEggs(Player player) {
        return databaseManager.getPlayerStoredEggs(player.getUniqueId());
    }
    
    /**
     * Retrieves a specific stored Dragon Egg for a player, removing it from storage
     * and adding it to the player's inventory if possible.
     *
     * @param player     The player retrieving the egg.
     * @param dragonUUID The UUID of the egg to retrieve.
     * @return {@code true} if the egg was successfully retrieved, {@code false} otherwise.
     */
    public boolean retrieveEgg(Player player, UUID dragonUUID) {
        String playerUuidString = player.getUniqueId().toString();
        String dragonUuidString = dragonUUID.toString();
        Map<String, String> eggData = databaseManager.getEggData(playerUuidString, dragonUuidString);

        if (eggData == null || eggData.isEmpty()) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggNotFoundStorage")); // Use lang key
            return false;
        }

        String dragonType = eggData.get("dragon_type");
        String dragonName = eggData.get("dragon_name");

        // Create the egg item
        Item eggItem = createDragonEgg(dragonType, dragonName, dragonUUID, player.getUniqueId());

        // Check if player has space in inventory
        if (player.getInventory().canAddItem(eggItem)) {
            // Remove from database
            boolean removed = databaseManager.removeEgg(playerUuidString, dragonUuidString);

            if (removed) {
                // Add to player inventory
                player.getInventory().addItem(eggItem);
                player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggRetrieved",
                        getColorForType(dragonType) + dragonName + TextFormat.GREEN)); // Use lang key with param
                return true;
            } else {
                player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.retrieveFailed")); // Use lang key
                return false;
            }
        } else {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.inventoryFull")); // Use lang key
            return false;
        }
    }
    
    /**
     * Handles the hatching of a Dragon Egg item. Spawns the corresponding DragonEntity,
     * marks the egg as hatched in the database, and updates the item's lore.
     *
     * @param player  The player hatching the egg.
     * @param eggItem The Dragon Egg item being hatched.
     * @return {@code true} if hatching was successful, {@code false} otherwise.
     */
    public boolean hatchEgg(Player player, Item eggItem) {
        // Verify it's a dragon egg
        if (!isDragonEgg(eggItem)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.invalidEgg"));
            return false;
        }

        // Get the egg ID
        UUID dragonUUID = getDragonUUID(eggItem);
        if (dragonUUID == null) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.corruptedEgg"));
            return false;
        }

        // Check if egg is already hatched
        if (databaseManager.isEggHatched(dragonUUID.toString())) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggAlreadyHatched"));
            return false;
        }

        String dragonType = getDragonType(eggItem);
        String dragonName = getDragonName(eggItem);

        // Verify dragon type
        if (dragonType == null || dragonType.isEmpty()) {
            dragonType = "Fire Dragon"; // Default
        }

        if (dragonName == null || dragonName.isEmpty()) {
            dragonName = "Dragon"; // Default
        }

        // Create dragon entity - we don't actually spawn it yet, just create the database entry
        // We don't need to call spawnDragon here, just mark it as hatched in the database
        // DragonEntity dragon = plugin.getDragonManager().createDragon(dragonUUID, dragonType, dragonName);
        
        // Success! We don't need to send a message here, as the ProcessIncubationTask or 
        // admin commands will handle notifications to avoid duplicates
        
        // Mark egg as hatched in DB
        databaseManager.setEggHatched(dragonUUID.toString(), true);
        // Update lore on the item in hand if possible (might need listener adjustment)
        updateEggLore(eggItem, dragonUUID.toString());
        return true;
    }

    /**
     * Updates the lore of a Dragon Egg item based on its current status (hatched, incubating, dead, etc.).
     * Reads status information from the database.
     *
     * @param item  The Dragon Egg item to update.
     * @param eggId The UUID string of the egg.
     */
    public void updateEggLore(Item item, String eggId) {
        if (!isDragonEgg(item)) {
            return;
        }

        plugin.getLogger().debug("Updating egg lore for egg ID: " + eggId);

        // Fetch the lore template using LanguageManager
        // Assuming the lore template itself is a list of strings under one key
        String loreTemplateKey = "messages.egg.lore";
        plugin.getLogger().debug("Looking for lore template at key: " + loreTemplateKey);
        
        Object rawLore = plugin.getLanguageManager().getRawObject(loreTemplateKey); // Use the new method
        List<String> loreTemplate;

        if (rawLore instanceof List) {
            plugin.getLogger().debug("Found lore template as List");
            loreTemplate = (List<String>) rawLore; // Cast if it's a List
             if (loreTemplate.isEmpty()) {
                 plugin.getLogger().warning("Lore template '" + loreTemplateKey + "' is empty in language file.");
                 return; // Cannot proceed with empty template
             }
             plugin.getLogger().debug("Lore template has " + loreTemplate.size() + " entries");
        } else {
            plugin.getLogger().warning("Lore template '" + loreTemplateKey + "' not found or not a list in language file. Raw value: " + (rawLore != null ? rawLore.toString() : "null"));
            return; // Cannot proceed without template
        }

        // --- Gather Data ---
        String statusText;
        String instructionText;
        String progressText = ""; // Default to empty
        String nameAndTypeText = ""; // For hatched eggs
        String deathInfoText = ""; // For dead dragons
        
        boolean isHatched = databaseManager.isEggHatched(eggId);
        boolean isDead = databaseManager.isDragonDead(eggId);
        boolean isIncubating = !isHatched && !isDead && databaseManager.isEggIncubating(eggId);

        if (isDead) {
            // Dragon is dead - show death info
            statusText = plugin.getLanguageString("messages.egg.status.dead", 
                databaseManager.getDragonName(eggId)); // Pass dragon name for message
            instructionText = plugin.getLanguageString("messages.egg.instructions.dead");
            
            // Get death details
            Map<String, Object> deathInfo = databaseManager.getDragonDeathInfo(eggId);
            if (deathInfo != null) {
                String killedBy = (String) deathInfo.get("killedBy");
                String deathTime = (String) deathInfo.get("deathTime");
                String deathLocation = (String) deathInfo.get("deathLocation");
                
                // Format death information
                deathInfoText = TextFormat.RED + "Killed by: " + TextFormat.GRAY + killedBy + "\n" +
                                TextFormat.RED + "Time: " + TextFormat.GRAY + deathTime + "\n" +
                                TextFormat.RED + "Location: " + TextFormat.GRAY + deathLocation;
            }
            
            // Add dragon info
            String dragonName = databaseManager.getDragonName(eggId);
            String dragonType = databaseManager.getDragonType(eggId);
            nameAndTypeText = DragonUtils.getColorByType(dragonType) + dragonName;
            
        } else if (isHatched) {
            statusText = plugin.getLanguageString("messages.egg.status.hatched");
            instructionText = plugin.getLanguageString("messages.egg.instructions.summon");
            // Get hatched dragon info
            String dragonName = databaseManager.getDragonName(eggId);
            String dragonType = databaseManager.getDragonType(eggId);
            nameAndTypeText = DragonUtils.getColorByType(dragonType) + dragonName;
        } else if (isIncubating) {
            statusText = plugin.getLanguageString("messages.egg.status.incubating");
            instructionText = plugin.getLanguageString("messages.egg.instructions.toStop");

            // Get progress from the database using getEggData
            int currentSeconds = 0; // Default progress
            String ownerUUID = databaseManager.getPlayerUUIDFromEggId(eggId);
            if (ownerUUID != null) {
                Map<String, String> eggData = databaseManager.getEggData(ownerUUID, eggId);
                if (eggData != null && eggData.containsKey("incubationProgress")) {
                    try {
                        currentSeconds = Integer.parseInt(eggData.get("incubationProgress"));
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Failed to parse incubation progress from DB for egg " + eggId + ": " + eggData.get("incubationProgress"));
                    }
                }
            } else {
                 plugin.getLogger().warning("Could not find owner UUID for egg " + eggId + " to fetch incubation progress.");
            }
            
            // Calculate percentage
            int requiredSeconds = plugin.getConfig().getInt("timing.eggs.incubation_time_seconds", 3600);
            int progressPercent = 0;
            if (requiredSeconds > 0) {
                progressPercent = (int) Math.min(100.0, ((double) currentSeconds / requiredSeconds) * 100.0);
            }
            
            progressText = plugin.getLanguageString("messages.egg.progressFormat", progressPercent); // Use the calculated percentage

        } else { // Not hatched, not incubating, not dead
            statusText = plugin.getLanguageString("messages.egg.status.notIncubating");
            instructionText = plugin.getLanguageString("messages.egg.instructions.toStart");
        }

        // Get Owner Name
        String ownerName = "Unknown"; // Default
        String ownerUUIDString = databaseManager.getPlayerUUIDFromEggId(eggId);
        if (ownerUUIDString != null) {
            try {
                UUID ownerUUID = UUID.fromString(ownerUUIDString);
                Player owner = plugin.getServer().getPlayer(ownerUUID).orElse(null); // Get online player
                if (owner != null) {
                    ownerName = owner.getName();
                } else {
                    // Try to get offline player data if needed/possible, otherwise keep UUID or "Unknown"
                    // For simplicity, we'll just show UUID if offline for now
                    ownerName = ownerUUIDString.substring(0, 8) + "..."; // Shortened UUID for offline
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid owner UUID format in database for egg " + eggId + ": " + ownerUUIDString);
                ownerName = "Error";
            }
        }

        // --- Format Lore ---
        List<String> formattedLore = new ArrayList<>();
        for (String line : loreTemplate) {
            String formattedLine = line;
            // Replace placeholders - Order matters based on the template in en_US.yml
            // {0} = Status Text
            // {1} = Instruction Text
            // {2} = Progress Text (or empty string)
            // {3} = Owner Name
            // {4} = Egg ID (now replaced conditionally)
            // {5} = Death Information (new placeholder)
            formattedLine = formattedLine.replace("{0}", statusText);
            formattedLine = formattedLine.replace("{1}", instructionText);
            formattedLine = formattedLine.replace("{2}", progressText);
            formattedLine = formattedLine.replace("{3}", ownerName);
            
            // Conditionally replace {4}
            if (isHatched || isDead) {
                formattedLine = formattedLine.replace("{4}", nameAndTypeText); // Show Name and Type if hatched or dead
            } else {
                formattedLine = formattedLine.replace("{4}", ""); // Hide ID if not hatched
            }
            
            // Replace {5} with death info if available
            formattedLine = formattedLine.replace("{5}", deathInfoText);

            // Only add non-empty lines (e.g., if {4} was replaced by empty string and the original line was just "{4}")
            if (!formattedLine.trim().isEmpty()) {
                formattedLore.add(formattedLine); // Color codes are already in the template
            }
        }

        item.setLore(formattedLore.toArray(new String[0]));

        // Also update NBT display tag if necessary (though setLore should handle client-side display)
        CompoundTag displayTag = item.getNamedTag().getCompound("display");
        if (displayTag == null) {
            displayTag = new CompoundTag("display");
        }
        cn.nukkit.nbt.tag.ListTag<cn.nukkit.nbt.tag.StringTag> loreListTag = new cn.nukkit.nbt.tag.ListTag<>("Lore");
        for (String line : formattedLore) {
            loreListTag.add(new cn.nukkit.nbt.tag.StringTag("", line));
        }
        displayTag.putList(loreListTag);
        item.getNamedTag().putCompound("display", displayTag);
        
        // For dead dragons, update the appearance of the egg item
        if (isDead) {
            // Set a custom color to the egg item (darker or reddish)
            // This is a visual indicator that the dragon is dead
            item.setCustomName(TextFormat.RED + "Dead Dragon Egg: " + 
                TextFormat.RESET + databaseManager.getDragonName(eggId));
        }
    }

    /**
     * Retrieves a dragon egg from storage by its ID
     * 
     * @param player The player retrieving the egg
     * @param eggId The ID of the egg to retrieve
     * @return The egg item, or null if retrieval failed
     */
    public Item retrieveEggFromStorage(Player player, String eggId) {
        try {
            // Get egg data from database
            Map<String, Object> eggData = plugin.getDatabaseManager().getEggById(eggId);
            
            if (eggData == null) {
                player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggNotFound"));
                return null;
            }
            
            String dragonType = (String) eggData.get("type");
            String dragonName = (String) eggData.get("name");
            
            // Create the egg item
            UUID dragonUUID = UUID.fromString(eggId);
            Item eggItem = createDragonEgg(dragonType, dragonName, dragonUUID);
            
            // Add owner UUID to the egg's NBT data
            CompoundTag tag = eggItem.getNamedTag();
            tag.putString("OwnerUUID", player.getUniqueId().toString());
            eggItem.setNamedTag(tag);
            
            // Remove the egg from storage
            plugin.getDatabaseManager().removeEgg(player.getUniqueId().toString(), eggId);
            
            return eggItem;
        } catch (Exception e) {
            plugin.getLogger().error("Error retrieving egg from storage: " + e.getMessage(), e);
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.eggRetrievalFailed"));
            return null;
        }
    }

    /**
     * Creates a new dragon egg for a player (used when purchasing)
     * 
     * @param player The player to create the egg for
     * @return The created egg item
     */
    public Item createNewDragonEgg(Player player) {
        // Generate a new UUID for the egg
        UUID eggUUID = UUID.randomUUID();
        
        // Default values
        String dragonType = "Fire Dragon"; // Default type
        String dragonName = "Dragon"; // Default name
        
        // Create the egg item
        Item eggItem = createDragonEgg(dragonType, dragonName, eggUUID, player.getUniqueId());
        
        // Register the egg in the database
        plugin.getDatabaseManager().registerDragon(player.getUniqueId().toString(), eggUUID.toString(), dragonType, dragonName);
        
        return eggItem;
    }
}