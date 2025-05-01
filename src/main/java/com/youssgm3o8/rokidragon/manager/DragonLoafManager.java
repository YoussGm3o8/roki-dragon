package com.youssgm3o8.rokidragon.manager;

import cn.nukkit.inventory.CraftingManager;
import cn.nukkit.item.Item;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.items.ItemDragonLoaf; // Import the item class

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the creation and crafting recipes for the Dragon Loaf item.
 */
public class DragonLoafManager {
    private final DragonPlugin plugin;

    /**
     * Constructs a new DragonLoafManager.
     * @param plugin The main plugin instance.
     */
    public DragonLoafManager(DragonPlugin plugin) {
        this.plugin = plugin;
        registerLoafRecipes(); // Call the recipe registration method from constructor
    }

    /**
     * Registers the crafting recipes for the Dragon Loaf.
     * Creates multiple recipes based on allowed meat/fish ingredients.
     */
    private void registerLoafRecipes() { // Make private, rename for clarity
        CraftingManager craftingManager = plugin.getServer().getCraftingManager();
        
        plugin.getLogger().info("Registering Dragon Loaf recipes...");
        // Create the result item using its constructor, which handles NBT, name, and lore
        Item dragonLoafResult = new ItemDragonLoaf(plugin).clone(); 

        // Define the shape
        String[] loafShape = {
            "WWW",
            "MMM",
            "WWW"
        };

        // Define the allowed middle ingredients
        List<Item> middleIngredients = List.of(
            Item.get(Item.COOKED_BEEF),
            Item.get(Item.RAW_BEEF),
            Item.get(Item.COOKED_CHICKEN),
            Item.get(Item.RAW_CHICKEN),
            Item.get(Item.COOKED_FISH), // Use COOKED_FISH
            Item.get(Item.RAW_FISH)   // Use RAW_FISH for raw cod/salmon
        );

        // Register a shaped recipe for each allowed middle ingredient
        int recipeCount = 0;
        for (Item middleItem : middleIngredients) {
            Map<Character, Item> ingredients = new HashMap<>();
            ingredients.put('W', Item.get(Item.WHEAT));
            ingredients.put('M', middleItem); // Set the middle ingredient

            try {
                cn.nukkit.inventory.ShapedRecipe loafRecipe = new cn.nukkit.inventory.ShapedRecipe(
                    dragonLoafResult, 
                    loafShape,
                    ingredients,
                    new ArrayList<>()
                );
                // Nukkit handles recipe identification implicitly
                craftingManager.registerRecipe(loafRecipe);
                recipeCount++;
            } catch (Exception e) {
                 plugin.getLogger().error("Failed to register Dragon Loaf recipe for middle item: " + middleItem.getName(), e);
            }
        }
        plugin.getLogger().info("Registered " + recipeCount + " Dragon Loaf SHAPED crafting recipes.");
        
        // Optional: Rebuild packet if recipes don't show up immediately, 
        // but often calling registerRecipe multiple times handles it.
        // craftingManager.rebuildPacket(); 
    }

    // Note: Logic for using/consuming the loaf remains in EventListenerEdit as it involves player/entity events.
    // The isDragonLoaf check remains static in ItemDragonLoaf.
} 