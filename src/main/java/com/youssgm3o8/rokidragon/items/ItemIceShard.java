package com.youssgm3o8.rokidragon.items;

import com.youssgm3o8.rokidragon.DragonPlugin;
import cn.nukkit.item.Item;

/**
 * The Ice Dragon Shard item that dragons use to shoot ice balls
 */
public class ItemIceShard extends ItemDragonShard {
    
    /**
     * Constructs a new Ice Dragon Shard
     * @param plugin The main plugin instance
     */
    public ItemIceShard(DragonPlugin plugin) {
        super(plugin, Item.PRISMARINE_SHARD, "ice");
    }
} 