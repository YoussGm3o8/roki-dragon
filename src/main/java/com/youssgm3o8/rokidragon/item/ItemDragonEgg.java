package com.youssgm3o8.rokidragon.item;

import cn.nukkit.item.Item;

public class ItemDragonEgg extends Item {

    public ItemDragonEgg() {
        this(0, 1);
    }

    public ItemDragonEgg(Integer meta) {
        this(meta, 1);
    }

    public ItemDragonEgg(Integer meta, int count) {
        super(DRAGON_EGG, meta, count, "Dragon Egg");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
