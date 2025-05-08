package com.youssgm3o8.rokidragon.items;

import com.youssgm3o8.rokidragon.DragonPlugin;
import cn.nukkit.item.Item;

/**
 * The Lightning Dragon Shard item that dragons use to shoot lightning balls
 */
public class ItemLightningShard extends ItemDragonShard {
    
    /**
     * Constructs a new Lightning Dragon Shard
     * @param plugin The main plugin instance
     */
    public ItemLightningShard(DragonPlugin plugin) {
        super(plugin, Item.GHAST_TEAR, "lightning");
    }
} 