package com.youssgm3o8.rokidragon.items;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.inventory.ShapelessRecipe;
import cn.nukkit.Player;
import cn.nukkit.inventory.ShapedRecipe;
import com.youssgm3o8.rokidragon.DragonPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DragonShardManager {
    private final DragonPlugin plugin;
    
    // Custom item IDs (using existing item IDs)
    public static final int FIRE_SHARD_ID = Item.FIRE_CHARGE;
    public static final int ICE_SHARD_ID = Item.PRISMARINE_SHARD;
    public static final int LIGHTNING_SHARD_ID = Item.GHAST_TEAR;

    public DragonShardManager(DragonPlugin plugin) {
        this.plugin = plugin;
        registerItems();
        registerCraftingRecipes();
    }

    private void registerItems() {
        // No need to register items since we're using existing items with custom NBT
    }

    public Item createFireShard() {
        Item shard = Item.get(FIRE_SHARD_ID);
        shard.setCustomName(TextFormat.RED + "Fire Dragon Shard");
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "fire")
            .putBoolean("IsDragonShard", true);
        shard.setNamedTag(tag);
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + "Used by Fire Dragons to shoot fireballs");
        lore.add(TextFormat.GRAY + "Right-click while riding to shoot");
        shard.setLore(lore.toArray(new String[0]));
        return shard;
    }

    public Item createIceShard() {
        Item shard = Item.get(ICE_SHARD_ID);
        shard.setCustomName(TextFormat.AQUA + "Ice Dragon Shard");
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "ice")
            .putBoolean("IsDragonShard", true);
        shard.setNamedTag(tag);
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + "Used by Ice Dragons to shoot ice balls");
        lore.add(TextFormat.GRAY + "Right-click while riding to shoot");
        shard.setLore(lore.toArray(new String[0]));
        return shard;
    }

    public Item createLightningShard() {
        Item shard = Item.get(LIGHTNING_SHARD_ID);
        shard.setCustomName(TextFormat.YELLOW + "Lightning Dragon Shard");
        CompoundTag tag = new CompoundTag()
            .putString("DragonShardType", "lightning")
            .putBoolean("IsDragonShard", true);
        shard.setNamedTag(tag);
        ArrayList<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY + "Used by Lightning Dragons to shoot lightning balls");
        lore.add(TextFormat.GRAY + "Right-click while riding to shoot");
        shard.setLore(lore.toArray(new String[0]));
        return shard;
    }

    private void registerCraftingRecipes() {
        // Fire Dragon Shard Recipe
        Item fireShard = createFireShard();
        String[] fireShape = {
            "FTF",
            "TTT",
            "FTF"
        };
        Map<Character, Item> fireIngredients = new HashMap<>();
        fireIngredients.put('T', Item.get(Item.TNT));
        fireIngredients.put('F', Item.get(Item.FIRE_CHARGE));
        ArrayList<Item> fireOutput = new ArrayList<>();
        ShapedRecipe fireRecipe = new ShapedRecipe(fireShard, fireShape, fireIngredients, fireOutput);
        plugin.getServer().getCraftingManager().registerShapedRecipe(fireRecipe);
        plugin.getServer().getCraftingManager().registerRecipe(fireRecipe);
        plugin.getServer().addRecipe(fireRecipe);

        // Ice Dragon Shard Recipe
        Item iceShard = createIceShard();
        String[] iceShape = {
            "ITI",
            "TTT",
            "ITI"
        };
        Map<Character, Item> iceIngredients = new HashMap<>();
        iceIngredients.put('T', Item.get(Item.TNT));
        iceIngredients.put('I', Item.get(Item.ICE));
        ArrayList<Item> iceOutput = new ArrayList<>();
        ShapedRecipe iceRecipe = new ShapedRecipe(iceShard, iceShape, iceIngredients, iceOutput);
        plugin.getServer().getCraftingManager().registerShapedRecipe(iceRecipe);
        plugin.getServer().getCraftingManager().registerRecipe(iceRecipe);
        plugin.getServer().addRecipe(iceRecipe);

        // Lightning Dragon Shard Recipe
        Item lightningShard = createLightningShard();
        String[] lightningShape = {
            "LTL",
            "TTT",
            "LTL"
        };
        Map<Character, Item> lightningIngredients = new HashMap<>();
        lightningIngredients.put('T', Item.get(Item.TNT));
        lightningIngredients.put('L', Item.get(Item.REDSTONE_BLOCK));
        ArrayList<Item> lightningOutput = new ArrayList<>();
        ShapedRecipe lightningRecipe = new ShapedRecipe(lightningShard, lightningShape, lightningIngredients, lightningOutput);
        plugin.getServer().getCraftingManager().registerShapedRecipe(lightningRecipe);
        plugin.getServer().getCraftingManager().registerRecipe(lightningRecipe);
        plugin.getServer().addRecipe(lightningRecipe);

        // Rebuild the crafting packet
        plugin.getServer().getCraftingManager().rebuildPacket();
    }

    public boolean isValidShardForDragon(Item item, String dragonType) {
        if (!item.hasCompoundTag() || !item.getNamedTag().getBoolean("IsDragonShard")) {
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
                return;
        }
        
        shard.setCount(10);
        player.getInventory().addItem(shard);
        player.sendMessage(TextFormat.GREEN + "You received 10 " + shard.getCustomName() + "s for your " + dragonType + "!");
    }

    public boolean consumeShard(Player player, String dragonType) {
        int requiredShardId;
        String shardType;
        
        switch (dragonType) {
            case "Fire Dragon":
                requiredShardId = FIRE_SHARD_ID;
                shardType = "fire";
                break;
            case "Ice Dragon":
                requiredShardId = ICE_SHARD_ID;
                shardType = "ice";
                break;
            case "Lightning Dragon":
                requiredShardId = LIGHTNING_SHARD_ID;
                shardType = "lightning";
                break;
            default:
                return false;
        }

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            if (item.getId() == requiredShardId && 
                item.hasCompoundTag() && 
                item.getNamedTag().getBoolean("IsDragonShard") && 
                item.getNamedTag().getString("DragonShardType").equals(shardType)) {
                
                if (item.getCount() > 1) {
                    item.setCount(item.getCount() - 1);
                    player.getInventory().setItem(slot, item);
                } else {
                    player.getInventory().setItem(slot, Item.get(Item.AIR));
                }
                return true;
            }
        }
        return false;
    }
} 