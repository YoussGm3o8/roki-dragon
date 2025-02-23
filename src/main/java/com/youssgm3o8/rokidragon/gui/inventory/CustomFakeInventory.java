package com.youssgm3o8.rokidragon.gui.inventory;

import cn.nukkit.Player;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import me.iwareq.fakeinventories.FakeInventory;
import me.iwareq.fakeinventories.util.ItemHandler;
import cn.nukkit.event.inventory.InventoryTransactionEvent;

import java.util.HashMap;
import java.util.Map;

public class CustomFakeInventory extends FakeInventory {
    private final String title;
    private final Map<Integer, ItemHandler> handlers = new HashMap<>();
    private ItemHandler defaultItemHandler;

    public CustomFakeInventory(InventoryType type, String title) {
        super(type);
        this.title = title;
        // Initialize with a default handler that cancels all transactions
        this.defaultItemHandler = (item, event) -> event.setCancelled(true);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void onClose(Player player) {
        super.onClose(player);
    }

    @Override
    public boolean setItem(int index, Item item) {
        return super.setItem(index, item);
    }

    public void setHandler(ItemHandler handler) {
        if (handler != null) {
            this.defaultItemHandler = handler;
        }
    }

    public void setHandler(int index, ItemHandler handler) {
        if (handler != null) {
            this.handlers.put(index, handler);
        } else {
            this.handlers.remove(index);
        }
    }

    @Override
    public void handle(int index, Item item, InventoryTransactionEvent event) {
        ItemHandler handler = this.handlers.getOrDefault(index, this.defaultItemHandler);
        handler.handle(item, event);
    }
} 