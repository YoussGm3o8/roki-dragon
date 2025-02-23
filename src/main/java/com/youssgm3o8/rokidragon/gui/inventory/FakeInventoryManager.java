package com.youssgm3o8.rokidragon.gui.inventory;

import cn.nukkit.Player;
import cn.nukkit.inventory.InventoryType;

public class FakeInventoryManager {
    public static CustomFakeInventory createInventory(Player player, InventoryType type, String title) {
        return new CustomFakeInventory(type, title);
    }
} 