package com.youssgm3o8.rokidragon.items;

import com.youssgm3o8.rokidragon.DragonPlugin;
import cn.nukkit.item.Item;

/**
 * The Water Dragon Shard item that dragons use to shoot water balls
 */
public class ItemWaterShard extends ItemDragonShard {
    
    /**
     * Constructs a new Water Dragon Shard
     * @param plugin The main plugin instance
     */
    public ItemWaterShard(DragonPlugin plugin) {
        super(plugin, Item.PRISMARINE_CRYSTALS, "water");
    }
} 