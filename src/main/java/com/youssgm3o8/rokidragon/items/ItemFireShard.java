package com.youssgm3o8.rokidragon.items;

import com.youssgm3o8.rokidragon.DragonPlugin;
import cn.nukkit.item.ItemFireCharge;

/**
 * The Fire Dragon Shard item that dragons use to shoot fireballs
 */
public class ItemFireShard extends ItemDragonShard {
    
    /**
     * Constructs a new Fire Dragon Shard
     * @param plugin The main plugin instance
     */
    public ItemFireShard(DragonPlugin plugin) {
        super(plugin, ItemFireCharge.FIRE_CHARGE, "fire");
    }
} 