package com.youssgm3o8.rokidragon.gui.inventory;

import cn.nukkit.Player;
import cn.nukkit.inventory.ContainerInventory;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;

import java.util.HashMap;
import java.util.Map;

public class DragonInventory extends ContainerInventory {
    private final Map<Integer, DragonInventoryAction> slotActions;
    private final boolean allowPlayerInventoryActions;

    public DragonInventory(InventoryType type, InventoryHolder holder, String title) {
        super(holder, type, new HashMap<>(), type.getDefaultSize(), title);
        this.slotActions = new HashMap<>();
        this.allowPlayerInventoryActions = false;
    }

    public DragonInventory(InventoryType type, InventoryHolder holder, String title, boolean allowPlayerInventoryActions) {
        super(holder, type, new HashMap<>(), type.getDefaultSize(), title);
        this.slotActions = new HashMap<>();
        this.allowPlayerInventoryActions = allowPlayerInventoryActions;
    }

    @Override
    public void onOpen(Player player) {
        super.onOpen(player);
    }

    @Override
    public void onClose(Player player) {
        super.onClose(player);
    }

    public void setSlotAction(int slot, DragonInventoryAction action) {
        slotActions.put(slot, action);
    }

    public boolean executeAction(Player player, int slot) {
        DragonInventoryAction action = slotActions.get(slot);
        if (action != null) {
            action.execute(player, this.getItem(slot));
            return true;
        }
        return false;
    }

    public boolean isAllowPlayerInventoryActions() {
        return allowPlayerInventoryActions;
    }

    @Override
    public boolean setItem(int index, Item item, boolean send) {
        return super.setItem(index, item, send);
    }

    @FunctionalInterface
    public interface DragonInventoryAction {
        void execute(Player player, Item item);
    }
} 