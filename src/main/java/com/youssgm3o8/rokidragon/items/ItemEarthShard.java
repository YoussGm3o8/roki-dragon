package com.youssgm3o8.rokidragon.items;

import com.youssgm3o8.rokidragon.DragonPlugin;
import cn.nukkit.item.Item;

/**
 * The Earth Dragon Shard item that dragons use to shoot earth projectiles
 */
public class ItemEarthShard extends ItemDragonShard {
    
    /**
     * Constructs a new Earth Dragon Shard
     * @param plugin The main plugin instance
     */
    public ItemEarthShard(DragonPlugin plugin) {
        super(plugin, Item.CLAY_BALL, "earth");
    }
} 