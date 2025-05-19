package com.youssgm3o8.rokidragon.manager; // Updated package declaration

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;
import com.youssgm3o8.rokidragon.items.ItemDragonShard;
import com.youssgm3o8.rokidragon.items.ItemFireShard;
import com.youssgm3o8.rokidragon.items.ItemIceShard;
import com.youssgm3o8.rokidragon.items.ItemLightningShard;
import com.youssgm3o8.rokidragon.items.ItemWaterShard;
import com.youssgm3o8.rokidragon.items.ItemEarthShard;
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

    /**
     * Creates a Fire Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Fire Shard Item.
     */
    public Item createFireShard() {
        return new ItemFireShard(plugin);
    }

    /**
     * Creates an Ice Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Ice Shard Item.
     */
    public Item createIceShard() {
        return new ItemIceShard(plugin);
    }

    /**
     * Creates a Lightning Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Lightning Shard Item.
     */
    public Item createLightningShard() {
        return new ItemLightningShard(plugin);
    }

    /**
     * Creates a Water Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Water Shard Item.
     */
    public Item createWaterShard() {
        return new ItemWaterShard(plugin);
    }

    /**
     * Creates an Earth Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Earth Shard Item.
     */
    public Item createEarthShard() {
        return new ItemEarthShard(plugin);
    }

    /**
     * Registers the shaped crafting recipes for all types of Dragon Shards.
     * Recipes are registered with the server's crafting manager.
     */
    private void registerCraftingRecipes() {
        plugin.getLogger().info("Registering dragon shard crafting recipes...");
        
        try {
            // Get the crafting manager instance once
            cn.nukkit.inventory.CraftingManager craftingManager = plugin.getServer().getCraftingManager();
            
            // Fire Dragon Shard Recipe
            Item fireShard = createFireShard().clone(); // Make sure to clone the item
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
            craftingManager.registerRecipe(fireRecipe);
            
            // Ice Dragon Shard Recipe
            Item iceShard = createIceShard().clone(); // Make sure to clone the item
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
            craftingManager.registerRecipe(iceRecipe);
            
            // Lightning Dragon Shard Recipe
            Item lightningShard = createLightningShard().clone(); // Make sure to clone the item
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
            craftingManager.registerRecipe(lightningRecipe);
            
            // Water Dragon Shard Recipe
            Item waterShard = createWaterShard().clone(); // Make sure to clone the item
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
            craftingManager.registerRecipe(waterRecipe);
            
            // Earth Dragon Shard Recipe
            Item earthShard = createEarthShard().clone(); // Make sure to clone the item
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
            craftingManager.registerRecipe(earthRecipe);
            
            // Rebuild the crafting data packet to ensure clients receive updated recipes
            craftingManager.rebuildPacket();
            
            plugin.getLogger().info("Successfully registered all dragon shard recipes");
        } catch (Exception e) {
            plugin.getLogger().error("Failed to register dragon shard recipes", e);
        }
    }

    /**
     * Shows the shard recipes to a player via a form.
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
        if (item == null || !dragonType.contains("Dragon")) {
            return false;
        }
        
        String dragonTypeKey;
        
        switch (dragonType) {
            case "Fire Dragon":
                dragonTypeKey = "fire";
                break;
            case "Ice Dragon":
                dragonTypeKey = "ice";
                break;
            case "Lightning Dragon":
                dragonTypeKey = "lightning";
                break;
            case "Water Dragon":
                dragonTypeKey = "water";
                break;
            case "Earth Dragon":
                dragonTypeKey = "earth";
                break;
            default:
                plugin.getLogger().warning("Unknown dragon type in isValidShardForDragon: " + dragonType);
                return false; // Unknown dragon type
        }
        
        // Use the new static method in ItemDragonShard
        return ItemDragonShard.isDragonShardOfType(item, dragonTypeKey);
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
        
        // Normalize the dragon type string to just get the element type
        String dragonTypeKey;
        
        if (dragonType.contains("Fire")) {
            dragonTypeKey = "fire";
        } else if (dragonType.contains("Ice")) {
            dragonTypeKey = "ice";
        } else if (dragonType.contains("Lightning")) {
            dragonTypeKey = "lightning";
        } else if (dragonType.contains("Water")) {
            dragonTypeKey = "water";
        } else if (dragonType.contains("Earth")) {
            dragonTypeKey = "earth";
        } else {
            plugin.getLogger().warning("Unknown dragon type in consumeShard: " + dragonType);
            return false; // Unknown dragon type
        }

        plugin.getLogger().info("[ShardManager] Looking for " + dragonTypeKey + " shard for dragon type: " + dragonType);

        // First priority: Look for custom shard items with the right NBT data
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            
            if (item != null && item.hasCompoundTag()) {
                plugin.getLogger().debug("[ShardManager] Checking item in slot " + slot + ": " + item.getName() + " (ID: " + item.getId() + ")");
                
                // Check if it's a valid dragon shard of the right type using NBT data
                if (ItemDragonShard.isDragonShardOfType(item, dragonTypeKey)) {
                    plugin.getLogger().info("[ShardManager] Found custom " + dragonTypeKey + " shard in slot " + slot);
                    
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
        }
        
        plugin.getLogger().info("[ShardManager] No custom shard found, falling back to vanilla items");
        
        // Second priority: Fall back to vanilla items for compatibility
        int vanillaItemId;
        switch (dragonTypeKey) {
            case "fire":
                vanillaItemId = FIRE_SHARD_ID;
                break;
            case "ice":
                vanillaItemId = ICE_SHARD_ID;
                break;
            case "lightning":
                vanillaItemId = LIGHTNING_SHARD_ID;
                break;
            case "water":
                vanillaItemId = WATER_SHARD_ID;
                break;
            case "earth":
                vanillaItemId = EARTH_SHARD_ID;
                break;
            default:
                return false;
        }
        
        plugin.getLogger().info("[ShardManager] Looking for vanilla item ID " + vanillaItemId + " for " + dragonTypeKey);
        
        // Look for vanilla items if no custom shard was found
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            
            // Check if this is a vanilla item that matches what we need
            if (item != null && item.getId() == vanillaItemId) {
                // Only match vanilla items that don't have the dragon shard NBT tag
                // (to avoid considering real shards as vanilla items)
                if (!item.hasCompoundTag() || !item.getNamedTag().contains(ItemDragonShard.NBT_TAG)) {
                    plugin.getLogger().info("[ShardManager] Found vanilla item in slot " + slot);
                    
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
        }
        
        plugin.getLogger().info("[ShardManager] No matching shard or vanilla item found for " + dragonType);
        return false; // No matching items found
    }
}