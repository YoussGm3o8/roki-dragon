package com.youssgm3o8.rokidragon.manager; // Updated package declaration

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;
import com.youssgm3o8.rokidragon.util.DragonUtils;

import cn.nukkit.Player;
import cn.nukkit.inventory.ShapedRecipe;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;

/**
 * Manages the creation, crafting recipes, and usage of Dragon Shard items,
 * which act as ammunition for dragon abilities.
 */
public class DragonShardManager {
    private final DragonPlugin plugin;
    private FormBasedDragonGUI dragonGUI;
    
    // Custom item IDs (using existing item IDs)
    public static final int FIRE_SHARD_ID = Item.FIRE_CHARGE;
    public static final int ICE_SHARD_ID = Item.PRISMARINE_SHARD;
    public static final int LIGHTNING_SHARD_ID = Item.GHAST_TEAR;
    public static final int WATER_SHARD_ID = Item.PRISMARINE_CRYSTALS;
    public static final int EARTH_SHARD_ID = Item.CLAY_BALL;

    /**
     * Constructs a new DragonShardManager.
     * @param plugin The main plugin instance.
     */
    public DragonShardManager(DragonPlugin plugin) {
        this.plugin = plugin;
        // registerItems(); // No longer needed as we use NBT on existing items
        registerCraftingRecipes();
    }

    /**
     * Set the GUI reference after construction.
     * This is needed to avoid circular dependencies during initialization.
     * 
     * @param dragonGUI The FormBasedDragonGUI instance
     */
    public void setDragonGUI(FormBasedDragonGUI dragonGUI) {
        this.dragonGUI = dragonGUI;
    }

    private void registerItems() {
        // No need to register items since we're using existing items with custom NBT
    }

    /**
     * Creates a Fire Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Fire Shard Item.
     */
    public Item createFireShard() {
        Item shard = Item.get(FIRE_SHARD_ID);
        String name = TextFormat.RED + plugin.getLanguageString("shard.fire.name"); // Use lang key
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.fire.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.fire.lore.usage", "Fire Dragon")); // Use lang key with dragon type
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.RED + plugin.getLanguageString("shard.fire.lore.effect")); // Use lang key
        
        // Set name and lore BEFORE adding NBT data
        shard.setCustomName(name);
        shard.setLore(lore.toArray(new String[0]));
        
        // Now apply NBT data
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "fire")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        return shard;
    }

    /**
     * Creates an Ice Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Ice Shard Item.
     */
    public Item createIceShard() {
        Item shard = Item.get(ICE_SHARD_ID);
        String name = TextFormat.AQUA + plugin.getLanguageString("shard.ice.name"); // Use lang key
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.ice.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.ice.lore.usage", "Ice Dragon")); // Use lang key with dragon type
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.AQUA + plugin.getLanguageString("shard.ice.lore.effect")); // Use lang key
        
        // Set name and lore BEFORE adding NBT data
        shard.setCustomName(name);
        shard.setLore(lore.toArray(new String[0]));
        
        // Now apply NBT data
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "ice")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        return shard;
    }

    /**
     * Creates a Lightning Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Lightning Shard Item.
     */
    public Item createLightningShard() {
        Item shard = Item.get(LIGHTNING_SHARD_ID);
        String name = TextFormat.YELLOW + plugin.getLanguageString("shard.lightning.name"); // Use lang key
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.lightning.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.lightning.lore.usage", "Lightning Dragon")); // Use lang key with dragon type
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.GOLD + plugin.getLanguageString("shard.lightning.lore.effect")); // Use lang key
        
        // Set name and lore BEFORE adding NBT data
        shard.setCustomName(name);
        shard.setLore(lore.toArray(new String[0]));
        
        // Now apply NBT data
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "lightning")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        return shard;
    }

    /**
     * Creates a Water Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Water Shard Item.
     */
    public Item createWaterShard() {
        Item shard = Item.get(WATER_SHARD_ID);
        String name = TextFormat.BLUE + plugin.getLanguageString("shard.water.name");
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.water.lore.description"));
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.water.lore.usage", "Water Dragon"));
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction"));
        lore.add(TextFormat.BLUE + plugin.getLanguageString("shard.water.lore.effect"));
        
        // Set name and lore BEFORE adding NBT data
        shard.setCustomName(name);
        shard.setLore(lore.toArray(new String[0]));
        
        // Now apply NBT data
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "water")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        return shard;
    }

    /**
     * Creates an Earth Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Earth Shard Item.
     */
    public Item createEarthShard() {
        Item shard = Item.get(EARTH_SHARD_ID);
        String name = TextFormat.GREEN + plugin.getLanguageString("shard.earth.name");
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.earth.lore.description"));
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.earth.lore.usage", "Earth Dragon"));
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction"));
        lore.add(TextFormat.GREEN + plugin.getLanguageString("shard.earth.lore.effect"));
        
        // Set name and lore BEFORE adding NBT data
        shard.setCustomName(name);
        shard.setLore(lore.toArray(new String[0]));
        
        // Now apply NBT data
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "earth")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        return shard;
    }

    /**
     * Registers the shaped crafting recipes for all types of Dragon Shards.
     * Recipes are registered with the server's crafting manager.
     */
    private void registerCraftingRecipes() {
        plugin.getLogger().info("Registering dragon shard crafting recipes...");
        
        try {
            // Fire Dragon Shard Recipe
            Item fireShard = createFireShard();
            String[] fireShape = {
                "FBF",
                "BTB",
                "FBF"
            };
            Map<Character, Item> fireIngredients = new HashMap<>();
            fireIngredients.put('T', Item.get(Item.TNT));
            fireIngredients.put('F', Item.get(Item.FIRE_CHARGE));
            fireIngredients.put('B', Item.get(Item.BLAZE_POWDER));
            
            // Create Fire Shard recipe
            ShapedRecipe fireRecipe = new ShapedRecipe(fireShard, fireShape, fireIngredients, new ArrayList<>());
            plugin.getServer().getCraftingManager().registerRecipe(fireRecipe);
            
            // Ice Dragon Shard Recipe
            Item iceShard = createIceShard();
            String[] iceShape = {
                "ISI",
                "STS",
                "ISI"
            };
            Map<Character, Item> iceIngredients = new HashMap<>();
            iceIngredients.put('T', Item.get(Item.TNT));
            iceIngredients.put('I', Item.get(Item.PACKED_ICE));
            iceIngredients.put('S', Item.get(Item.SNOWBALL));
            
            // Create Ice Shard recipe
            ShapedRecipe iceRecipe = new ShapedRecipe(iceShard, iceShape, iceIngredients, new ArrayList<>());
            plugin.getServer().getCraftingManager().registerRecipe(iceRecipe);
            
            // Lightning Dragon Shard Recipe
            Item lightningShard = createLightningShard();
            String[] lightningShape = {
                "GLG",
                "LTL",
                "GLG"
            };
            Map<Character, Item> lightningIngredients = new HashMap<>();
            lightningIngredients.put('T', Item.get(Item.TNT));
            lightningIngredients.put('L', Item.get(Item.REDSTONE_BLOCK));
            lightningIngredients.put('G', Item.get(Item.GLOWSTONE_DUST));
            
            // Create Lightning Shard recipe
            ShapedRecipe lightningRecipe = new ShapedRecipe(lightningShard, lightningShape, lightningIngredients, new ArrayList<>());
            plugin.getServer().getCraftingManager().registerRecipe(lightningRecipe);
            
            // Water Dragon Shard Recipe
            Item waterShard = createWaterShard();
            String[] waterShape = {
                "PLP",
                "LTL",
                "PLP"
            };
            Map<Character, Item> waterIngredients = new HashMap<>();
            waterIngredients.put('T', Item.get(Item.TNT));
            waterIngredients.put('L', Item.get(Item.BUCKET, 8)); // Bucket with water data value
            waterIngredients.put('P', Item.get(Item.PRISMARINE_CRYSTALS));
            
            // Create Water Shard recipe
            ShapedRecipe waterRecipe = new ShapedRecipe(waterShard, waterShape, waterIngredients, new ArrayList<>());
            plugin.getServer().getCraftingManager().registerRecipe(waterRecipe);
            
            // Earth Dragon Shard Recipe
            Item earthShard = createEarthShard();
            String[] earthShape = {
                "CEC",
                "ETE",
                "CEC"
            };
            Map<Character, Item> earthIngredients = new HashMap<>();
            earthIngredients.put('T', Item.get(Item.TNT));
            earthIngredients.put('E', Item.get(Item.DIRT));
            earthIngredients.put('C', Item.get(Item.CLAY_BALL));
            
            // Create Earth Shard recipe
            ShapedRecipe earthRecipe = new ShapedRecipe(earthShard, earthShape, earthIngredients, new ArrayList<>());
            plugin.getServer().getCraftingManager().registerRecipe(earthRecipe);
            
            // Rebuild the crafting data packet to ensure clients receive the recipes
            plugin.getServer().getCraftingManager().rebuildPacket();
            
            // Inform players about the recipes through chat messages
            // informPlayersAboutRecipes(); // Consider if this broadcast is needed on enable, maybe only on first join/dragon get?

            plugin.getLogger().info("Successfully registered all dragon shard crafting recipes");
        } catch (Exception e) {
            plugin.getLogger().error("Error registering crafting recipes: " + e.getMessage(), e);
        }
    }

    /**
     * Broadcasts information about available dragon shard recipes to online players
     * with the necessary permission.
     * Consider calling this less frequently (e.g., on demand with a command) instead of on enable.
     */
    private void informPlayersAboutRecipes() {
        // Create the recipe messages using language keys
        String title = TextFormat.GREEN.toString() + TextFormat.BOLD + plugin.getLanguageString("messages.recipes.broadcast.title");
        String fireRecipe = TextFormat.RED.toString() + TextFormat.BOLD + plugin.getLanguageString("shard.fire.name") + TextFormat.RESET + ": " + plugin.getLanguageString("messages.recipes.broadcast.fire");
        String iceRecipe = TextFormat.AQUA.toString() + TextFormat.BOLD + plugin.getLanguageString("shard.ice.name") + TextFormat.RESET + ": " + plugin.getLanguageString("messages.recipes.broadcast.ice");
        String lightningRecipe = TextFormat.YELLOW.toString() + TextFormat.BOLD + plugin.getLanguageString("shard.lightning.name") + TextFormat.RESET + ": " + plugin.getLanguageString("messages.recipes.broadcast.lightning");
        String guide = TextFormat.GRAY + plugin.getLanguageString("messages.recipes.broadcast.guide");

        // Send information to all players with permission
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.hasPermission("rokidragon.use")) {
                player.sendMessage(title);
                player.sendMessage(fireRecipe);
                player.sendMessage(iceRecipe);
                player.sendMessage(lightningRecipe);
                player.sendMessage(guide);
            }
        }
    }

    /**
     * Shows detailed recipe information for all Dragon Shards to a specific player.
     * Uses the form-based GUI instead of chat messages.
     *
     * @param player The player to show recipes to.
     */
    public void showRecipesToPlayer(Player player) {
        if (dragonGUI != null) {
            dragonGUI.showRecipesForm(player);
        } else {
            // Fallback in case the GUI reference isn't set yet
            player.sendMessage(TextFormat.RED + "Recipe display is not available at the moment.");
        }
    }

    /**
     * Checks if the provided item is a valid shard for the specified dragon type.
     * @param item The item to check.
     * @param dragonType The type of dragon.
     * @return {@code true} if the item is a valid shard for the dragon type, {@code false} otherwise.
     */
    public boolean isValidShardForDragon(Item item, String dragonType) {
        if (item == null || !item.hasCompoundTag() || !dragonType.contains("Dragon")) {
            return false;
        }
        
        CompoundTag tag = item.getNamedTag();
        if (!tag.getBoolean("IsDragonShard")) {
            return false;
        }
        
        String shardType = tag.getString("DragonShardType").toLowerCase();
        String dragonTypeKey = dragonType.split(" ")[0].toLowerCase(); // Extract first word
        
        // Match shard type to dragon type (e.g., "fire" shard for "Fire Dragon")
        return shardType.equals(dragonTypeKey);
    }

    /**
     * Gives the player initial shards based on their dragon type when a dragon is first obtained.
     * @param dragonType The type of dragon.
     * @param player The player to give shards to.
     */
    public void giveInitialShards(String dragonType, Player player) {
        // Check if this player already has this dragon type in metadata
        // to avoid giving shards multiple times
        String playerUUID = player.getUniqueId().toString();
        
        String metaKey = "gave_shards_" + dragonType.toLowerCase().replace(" ", "_");
        plugin.getLogger().info("[ShardManager] Checking if player " + player.getName() + 
                              " already has metadata key '" + metaKey + "' for " + dragonType);
        
        if (plugin.getDatabaseManager().hasPlayerMetadata(playerUUID, metaKey)) {
            plugin.getLogger().info("[ShardManager] Player " + player.getName() + 
                                  " already received initial shards for " + dragonType + 
                                  " (found metadata key '" + metaKey + "')");
            return;
        }
        
        plugin.getLogger().info("[ShardManager] Player " + player.getName() + 
                              " has not received shards for " + dragonType + " yet, giving initial shards");
        
        // Create the appropriate shards based on dragon type
        Item[] shards;
        String coloredTypeName;
        
        switch (dragonType) {
            case "Fire Dragon":
                shards = new Item[]{createFireShard(), createFireShard(), createFireShard()};
                coloredTypeName = TextFormat.RED + dragonType;
                break;
            case "Ice Dragon":
                shards = new Item[]{createIceShard(), createIceShard(), createIceShard()};
                coloredTypeName = TextFormat.AQUA + dragonType;
                break;
            case "Lightning Dragon":
                shards = new Item[]{createLightningShard(), createLightningShard(), createLightningShard()};
                coloredTypeName = TextFormat.YELLOW + dragonType;
                break;
            case "Water Dragon":
                shards = new Item[]{createWaterShard(), createWaterShard(), createWaterShard()};
                coloredTypeName = TextFormat.BLUE + dragonType;
                break;
            case "Earth Dragon":
                shards = new Item[]{createEarthShard(), createEarthShard(), createEarthShard()};
                coloredTypeName = TextFormat.GREEN + dragonType;
                break;
            default:
                plugin.getLogger().warning("[ShardManager] Unknown dragon type for shards: " + dragonType);
                return;
        }
        
        int shardCount = 10; // Consider making this configurable
        for (Item shard : shards) {
            shard.setCount(shardCount);
            player.getInventory().addItem(shard);
        }
        
        // Send the message using the correct arguments
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.initialShards", shardCount, coloredTypeName, coloredTypeName));
        
        // Mark that this player has received initial shards for this dragon type
        plugin.getDatabaseManager().setPlayerMetadata(playerUUID, metaKey, "true");
        plugin.getLogger().info("[ShardManager] Set metadata key '" + metaKey + "' for player " + 
                              player.getName() + " to mark shards as given");
    }

    /**
     * Consumes a dragon shard from the player's inventory.
     * @param player The player to consume the shard from.
     * @param dragonType The type of dragon.
     * @return {@code true} if a shard was consumed, {@code false} otherwise.
     */
    public boolean consumeShard(Player player, String dragonType) {
        if (player == null || dragonType == null) return false;
        
        int shardId;
        
        switch (dragonType) {
            case "Fire Dragon":
                shardId = FIRE_SHARD_ID;
                break;
            case "Ice Dragon":
                shardId = ICE_SHARD_ID;
                break;
            case "Lightning Dragon":
                shardId = LIGHTNING_SHARD_ID;
                break;
            case "Water Dragon":
                shardId = WATER_SHARD_ID;
                break;
            case "Earth Dragon":
                shardId = EARTH_SHARD_ID;
                break;
            default:
                return false; // Unknown dragon type
        }

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            // Check if item is the correct type and is a valid dragon shard
            if (item != null && item.getId() == shardId && 
                item.hasCompoundTag() && 
                item.getNamedTag().getBoolean("IsDragonShard")) {
                
                if (item.getCount() > 1) {
                    item.setCount(item.getCount() - 1);
                    player.getInventory().setItem(slot, item);
                } else {
                    player.getInventory().setItem(slot, Item.get(Item.AIR));
                }
                player.getInventory().sendContents(player); // Update client inventory
                return true;
            }
        }
        return false;
    }
}