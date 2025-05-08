package com.youssgm3o8.rokidragon.items;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.youssgm3o8.rokidragon.DragonPlugin; // Import DragonPlugin
import com.youssgm3o8.rokidragon.language.LanguageManager;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBread;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Config; // Import Config
import cn.nukkit.utils.TextFormat;

public class ItemDragonLoaf extends ItemBread {

    public static final String NBT_TAG = "IsDragonLoaf";

    // Constructor now requires DragonPlugin instance
    public ItemDragonLoaf(DragonPlugin plugin) {
        super(0, 1); // Meta 0, Count 1

        LanguageManager languageManager = plugin.getLanguageManager();
        
        // Fetch name using LanguageManager
        String name = languageManager.get("items.dragon_loaf.name", "§6Dragon Loaf"); // Default name with color
        this.setCustomName(name); // Name includes color code from lang file

        // Get lore from LanguageManager directly using getRawObject which returns the raw object
        Object rawLore = languageManager.getRawObject("items.dragon_loaf.lore");
        List<String> loreList = new ArrayList<>();
        
        if (rawLore instanceof List) {
            // Convert the raw object to a List<String>
            @SuppressWarnings("unchecked")
            List<Object> rawLoreList = (List<Object>) rawLore;
            
            for (Object line : rawLoreList) {
                if (line != null) {
                    loreList.add(line.toString());
                }
            }
        }

        if (!loreList.isEmpty()) {
            // Apply color codes to lore lines if not already formatted
            for (int i = 0; i < loreList.size(); i++) {
                loreList.set(i, TextFormat.colorize('&', loreList.get(i)));
            }
            this.setLore(loreList.toArray(new String[0]));
        } else {
            // Fallback lore if not found in lang file
            plugin.getLogger().warning("Lore for 'items.dragon_loaf.lore' not found in language file. Using default.");
            this.setLore(
                TextFormat.colorize('&', "&7A hearty loaf baked with special ingredients."),
                TextFormat.colorize('&', "&7Feed it to your dragon (&aRight-Click&7 on dragon)"),
                TextFormat.colorize('&', "&7to restore its health."),
                "",
                TextFormat.colorize('&', "&cWarning: Not recommended for human consumption!")
            );
        }

        // Add NBT tag to identify this specific item
        CompoundTag tag = this.getNamedTag();
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putBoolean(NBT_TAG, true);
        // Add enchantment tag for glistening effect (visual only)
        tag.putList(new cn.nukkit.nbt.tag.ListTag<>("ench"));
        this.setNamedTag(tag);
    }

    // Method remains the same
    public String getNamespaceId() {
        return "rokidragon:dragon_loaf";
    }

    // Method remains the same
    public boolean hasEnchantmentGlint() {
        return true;
    }

    // Static method remains the same
    public static boolean isDragonLoaf(Item item) {
        return item != null && item.getId() == Item.BREAD && item.hasCompoundTag() && item.getNamedTag().getBoolean(NBT_TAG);
    }
}