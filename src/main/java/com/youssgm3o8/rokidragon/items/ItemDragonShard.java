package com.youssgm3o8.rokidragon.items;

import java.util.ArrayList;
import java.util.List;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.language.LanguageManager;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;

/**
 * Base class for all Dragon Shard items
 */
public abstract class ItemDragonShard extends Item {

    public static final String NBT_TAG = "IsDragonShard";

    protected final DragonPlugin plugin;
    protected final String dragonType;
    
    /**
     * Constructs a new Dragon Shard item
     * 
     * @param plugin The plugin instance
     * @param id The item ID to use
     * @param dragonType The type of dragon this shard is for (e.g., "fire", "ice", etc.)
     */
    protected ItemDragonShard(DragonPlugin plugin, int id, String dragonType) {
        super(id, 0, 1); // meta 0, count 1
        this.plugin = plugin;
        this.dragonType = dragonType.toLowerCase();
        
        setupNameAndLore();
        setupNBT();
    }
    
    /**
     * Sets up the item's name and lore using language entries
     */
    protected void setupNameAndLore() {
        LanguageManager languageManager = plugin.getLanguageManager();
        
        // Get name from language file and trim to remove any trailing newlines
        String name = languageManager.get("shard." + dragonType + ".name", "§6" + capitalizeFirst(dragonType) + " Dragon Shard");
        name = name.trim(); // Remove any trailing or leading whitespace
        setCustomName(name);
        
        // Create lore list
        ArrayList<String> lore = new ArrayList<>();
        
        // Add description line
        String description = languageManager.get("shard." + dragonType + ".lore.description", 
            "§7A powerful shard infused with " + dragonType + " energy");
        lore.add(description.trim()); // Trim to remove any whitespace
        
        // Add usage line with dragon type substitution
        String dragonTypeFull = capitalizeFirst(dragonType) + " Dragon";
        String usage = languageManager.get("shard." + dragonType + ".lore.usage", 
            "§7Used by " + dragonTypeFull + " to shoot " + dragonType + " projectiles");
        usage = usage.replace("{0}", dragonTypeFull);
        lore.add(TextFormat.colorize(usage.trim())); // Trim and colorize
        
        // Add spacing
        lore.add("");
        
        // Add instruction
        String instruction = languageManager.get("shard.lore.instruction", "§eRight-click while riding to shoot");
        lore.add(instruction.trim()); // Trim to remove any whitespace
        
        // Add effect based on type
        String effect = languageManager.get("shard." + dragonType + ".lore.effect", 
            "§" + getColorCodeForType() + "Unleashes " + dragonType + " power");
        lore.add(effect.trim()); // Trim to remove any whitespace
        
        // Set the final lore
        setLore(lore.toArray(new String[0]));
    }
    
    /**
     * Sets up the NBT data for the item
     */
    protected void setupNBT() {
        // Add NBT tag to identify this specific item
        CompoundTag tag = getNamedTag();
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putBoolean(NBT_TAG, true);
        tag.putString("DragonShardType", dragonType);
        // Add enchantment tag for glistening effect (visual only)
        tag.putList(new cn.nukkit.nbt.tag.ListTag<>("ench"));
        setNamedTag(tag);
    }
    
    /**
     * Returns the namespace ID for this shard
     */
    public String getNamespaceId() {
        return "rokidragon:dragon_shard_" + dragonType;
    }
    
    /**
     * Makes the item have an enchantment glint
     */
    public boolean hasEnchantmentGlint() {
        return true;
    }
    
    /**
     * Checks if an item is a Dragon Shard of any type
     */
    public static boolean isDragonShard(Item item) {
        return item != null && item.hasCompoundTag() && item.getNamedTag().getBoolean(NBT_TAG);
    }
    
    /**
     * Checks if an item is a Dragon Shard of a specific type
     */
    public static boolean isDragonShardOfType(Item item, String type) {
        return isDragonShard(item) && 
               item.getNamedTag().getString("DragonShardType").equalsIgnoreCase(type);
    }
    
    /**
     * Helper method to capitalize the first letter of a string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Gets the color code to use for this dragon type
     */
    protected char getColorCodeForType() {
        switch (dragonType.toLowerCase()) {
            case "fire": return 'c'; // Red
            case "ice": return 'b'; // Aqua
            case "lightning": return 'e'; // Yellow
            case "water": return '9'; // Blue
            case "earth": return '2'; // Dark green
            default: return '7'; // Gray (default)
        }
    }
} 