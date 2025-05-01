package com.youssgm3o8.rokidragon.manager; // Updated package declaration

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;

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
        shard.setCustomName(name);
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.fire.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.fire.lore.usage")); // Use lang key
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.RED + plugin.getLanguageString("shard.fire.lore.effect")); // Use lang key
        
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "fire")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        shard.setLore(lore.toArray(new String[0]));
        return shard;
    }

    /**
     * Creates an Ice Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Ice Shard Item.
     */
    public Item createIceShard() {
        Item shard = Item.get(ICE_SHARD_ID);
        String name = TextFormat.AQUA + plugin.getLanguageString("shard.ice.name"); // Use lang key
        shard.setCustomName(name);
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.ice.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.ice.lore.usage")); // Use lang key
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.AQUA + plugin.getLanguageString("shard.ice.lore.effect")); // Use lang key
        
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "ice")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        shard.setLore(lore.toArray(new String[0]));
        return shard;
    }

    /**
     * Creates a Lightning Dragon Shard item with custom NBT data, name, and lore.
     * @return The created Lightning Shard Item.
     */
    public Item createLightningShard() {
        Item shard = Item.get(LIGHTNING_SHARD_ID);
        String name = TextFormat.YELLOW + plugin.getLanguageString("shard.lightning.name"); // Use lang key
        shard.setCustomName(name);
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.lightning.lore.description")); // Use lang key
        lore.add(TextFormat.GRAY + plugin.getLanguageString("shard.lightning.lore.usage")); // Use lang key
        lore.add("");
        lore.add(TextFormat.YELLOW + plugin.getLanguageString("shard.lore.instruction")); // Use lang key
        lore.add(TextFormat.GOLD + plugin.getLanguageString("shard.lightning.lore.effect")); // Use lang key
        
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "lightning")
            .putBoolean("IsDragonShard", true)
            .putString("ShardId", UUID.randomUUID().toString());
            
        shard.setNamedTag(tag);
        shard.setLore(lore.toArray(new String[0]));
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
     * Checks if a given item is the correct type of Dragon Shard for a specific dragon type.
     * Verifies item ID and the 'DragonShardType' NBT tag.
     *
     * @param item       The item to check.
     * @param dragonType The type of the dragon (e.g., "Fire Dragon").
     * @return {@code true} if the item is the valid shard for the dragon type, {@code false} otherwise.
     */
    public boolean isValidShardForDragon(Item item, String dragonType) {
        if (item == null || !item.hasCompoundTag() || !item.getNamedTag().getBoolean("IsDragonShard")) { // Added null check
            return false;
        }

        String shardType = item.getNamedTag().getString("DragonShardType");
        switch (dragonType) {
            case "Fire Dragon":
                return item.getId() == FIRE_SHARD_ID && "fire".equals(shardType);
            case "Ice Dragon":
                return item.getId() == ICE_SHARD_ID && "ice".equals(shardType);
            case "Lightning Dragon":
                return item.getId() == LIGHTNING_SHARD_ID && "lightning".equals(shardType);
            default:
                return false;
        }
    }

    /**
     * Gives a player an initial stack of the appropriate Dragon Shards when their dragon hatches
     * or potentially when they change dragon type.
     *
     * @param dragonType The type of the dragon.
     * @param player     The player to give the shards to.
     */
    public void giveInitialShards(String dragonType, Player player) {
        Item shard;
        switch (dragonType) {
            case "Fire Dragon":
                shard = createFireShard();
                break;
            case "Ice Dragon":
                shard = createIceShard();
                break;
            case "Lightning Dragon":
                shard = createLightningShard();
                break;
            default:
                plugin.getLogger().warning("Attempted to give initial shards for unknown dragon type: " + dragonType);
                return;
        }
        
        shard.setCount(10); // Consider making this configurable
        player.getInventory().addItem(shard);
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.initialShards", 10, shard.getCustomName() + TextFormat.GREEN, dragonType)); // Use lang key
    }

    /**
     * Consumes one shard of the appropriate type from the player's inventory.
     * Used when a dragon uses its shooting ability.
     *
     * @param player     The player whose inventory to consume from.
     * @param dragonType The type of the dragon, determining which shard type is required.
     * @return {@code true} if a shard was successfully found and consumed, {@code false} otherwise.
     */
    public boolean consumeShard(Player player, String dragonType) {
        int requiredShardId;
        String requiredShardType;
        
        switch (dragonType) {
            case "Fire Dragon":
                requiredShardId = FIRE_SHARD_ID;
                requiredShardType = "fire";
                break;
            case "Ice Dragon":
                requiredShardId = ICE_SHARD_ID;
                requiredShardType = "ice";
                break;
            case "Lightning Dragon":
                requiredShardId = LIGHTNING_SHARD_ID;
                requiredShardType = "lightning";
                break;
            default:
                return false;
        }

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            // Check if item is the correct type and is a valid dragon shard
            if (item != null && item.getId() == requiredShardId && 
                item.hasCompoundTag() && 
                item.getNamedTag().getBoolean("IsDragonShard") && 
                requiredShardType.equals(item.getNamedTag().getString("DragonShardType"))) {
                
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