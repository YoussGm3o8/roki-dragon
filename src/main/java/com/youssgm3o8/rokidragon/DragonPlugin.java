package com.youssgm3o8.rokidragon;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.LongTag;
import cn.nukkit.event.Listener;
import cn.nukkit.permission.Permission;
import me.onebone.economyapi.EconomyAPI;
import cn.nukkit.entity.custom.EntityManager;
import com.youssgm3o8.rokidragon.util.DragonEggManager;
import com.youssgm3o8.rokidragon.entities.DragonEntity;
import com.youssgm3o8.rokidragon.commands.SummonDragonCommand;
import com.youssgm3o8.rokidragon.entities.EventListenerEdit;
import com.youssgm3o8.rokidragon.item.ItemDragonEgg;

import cn.nukkit.level.format.FullChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DragonPlugin extends PluginBase implements Listener {
    private DragonEggManager eggManager;
    private DatabaseManager dbManager;
    private double dragonEggPrice;
    private int dragonHatchingTime;
    private int dragonBuyCooldown;
    private Map<String, Long> lastPurchaseTime = new HashMap<>();

    @Override
    public void onLoad() {
        // Load SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            getLogger().error("Failed to load SQLite JDBC driver", e);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        
        // Wait for MobPlugin to be loaded first
        if (getServer().getPluginManager().getPlugin("MobPlugin") == null) {
            getLogger().error("MobPlugin not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load configuration
        loadConfig();

        // Register the dragon entity
        EntityManager.get().registerDefinition(DragonEntity.DEFINITION);
        
        // Initialize managers
        this.eggManager = new DragonEggManager();
        this.dbManager = new DatabaseManager(this);

        // Register permissions
        registerPermissions();

        getServer().getCommandMap().register("summondragon", new SummonDragonCommand("summondragon", "Summon or buy a dragon", "/summondragon [buy]", this));

        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            for (Player p : getServer().getOnlinePlayers().values()) {
                String playerUUID = p.getUniqueId().toString();
                String eggId = dbManager.getDragonEggId(playerUUID);

                // Check if the player has a dragon egg in their inventory
                Item dragonEgg = null;
                for (Item item : p.getInventory().getContents().values()) {
                    CompoundTag tag = item.getNamedTag();
                    if (tag != null && tag.contains("eggId")) {
                        String currentEggId = tag.getString("eggId");
                        if (currentEggId.equals(eggId)) {
                            dragonEgg = item;
                            getLogger().info("Found dragon egg for " + p.getName() + " with eggId: " + eggId + " (Item: " + item.getName() + ")");
                            break;
                        } else {
                            getLogger().warning("Found a dragon egg that doesnt belong to " + p.getName() + " (Item: " + item.getName() + ")");
                            p.getInventory().removeItem(item);
                            p.sendMessage(TextFormat.RED + "A dragon egg has exploded in your inventory because it doesn't belong to you.");
                            p.setHealth(p.getHealth() - 1);
                        }
                    } else {
                         if(item.getId() == BlockID.DRAGON_EGG){
                            getLogger().info("Found a dragon egg item id but eggId does not match for " + p.getName() + " (Item: " + item.getName() + ")");
                        }
                    }
                }

                if (eggId != null) {
                    if (dragonEgg != null && !dbManager.isEggHatched(eggId)) {
                        int onlineTime = dbManager.getEggOnlineTime(eggId);
                        if (onlineTime >= dragonHatchingTime / 60) {
                            dbManager.setEggHatched(eggId);
                            p.sendMessage("§aYour dragon egg has hatched!");
                            p.getInventory().removeItem(dragonEgg);
                            dbManager.insertDragon(playerUUID, eggId);
                            updateEggLore(dragonEgg, 0);
                        } else {
                            dbManager.incrementEggOnlineTime(eggId);
                            onlineTime = dbManager.getEggOnlineTime(eggId);
                            updateEggLore(dragonEgg, (dragonHatchingTime / 60) - onlineTime);
                        }
                    } else if (dragonEgg == null && !dbManager.isEggHatched(eggId)) {
                        // If the egg is not in the inventory, ensure the hatch time is still valid
                        int onlineTime = dbManager.getEggOnlineTime(eggId);
                        if (onlineTime >= dragonHatchingTime / 60) {
                            dbManager.setEggHatched(eggId);
                        }
                    }
                } else if (dragonEgg != null) {
                    //If the player has a dragon egg in their inventory but it isnt in the database, add it to the database
                    String newEggId = eggManager.purchaseEgg(playerUUID);
                    CompoundTag tag = dragonEgg.getNamedTag() != null ? dragonEgg.getNamedTag() : new CompoundTag();
                    tag.putString("eggId", newEggId);
                    dragonEgg.setNamedTag(tag);
                    dbManager.insertDragonEgg(newEggId, playerUUID);
                    updateEggLore(dragonEgg, dragonHatchingTime / 60);
                }
            }
        }, 1200, true); // 1200 ticks = 1 minute

        this.getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new EventListenerEdit(), this);
    }

    @Override
    public void onDisable() {
        // Remove all spawned dragon entities when the server closes.
        for (cn.nukkit.level.Level level : getServer().getLevels().values()) {
            for (cn.nukkit.entity.Entity entity : level.getEntities()) {
                if (entity instanceof com.youssgm3o8.rokidragon.entities.DragonEntity) {
                    entity.close();
                }
            }
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        dragonEggPrice = getConfig().getDouble("dragon-egg-price", 128000);
        dragonHatchingTime = getConfig().getInt("dragon-hatching-time", 3600);
        dragonBuyCooldown = getConfig().getInt("dragon-buy-cooldown", 60);
    }

    private void registerPermissions() {
        // Permission to buy a dragon
        Permission buyDragonPerm = new Permission("rokidragon.command.buy", "Allows player to buy a dragon egg");
        getServer().getPluginManager().addPermission(buyDragonPerm);

        // Permission to summon a dragon
        Permission summonDragonPerm = new Permission("rokidragon.command.summon", "Allows player to summon a dragon");
        getServer().getPluginManager().addPermission(summonDragonPerm);
    }

    public boolean handleBuyDragonCommand(Player player) {
        if (!player.hasPermission("rokidragon.command.buy")) {
            player.sendMessage("§cYou do not have permission to buy a dragon.");
            return true;
        }

        String playerUUID = player.getUniqueId().toString();
        double effectivePrice = dragonEggPrice;

        if (dbManager.playerHasDragon(playerUUID)) {
            player.sendMessage("§cYou already own a dragon.");
            return true;
        }

        boolean hasExistingEgg = dbManager.hasDragonEgg(playerUUID);

        if (hasExistingEgg) {
            // Allow repurchase at a reduced cost
            effectivePrice = dragonEggPrice * 0.5; // 50% of original price
            player.sendMessage("§eYou lost your dragon egg! You can repurchase it at a reduced cost of $" + effectivePrice + ".");
        }

        // Check cooldown
        if (lastPurchaseTime.containsKey(playerUUID)) {
            long lastTime = lastPurchaseTime.get(playerUUID);
            long now = System.currentTimeMillis() / 1000;
            if (now - lastTime < dragonBuyCooldown) {
                player.sendMessage("§cYou must wait " + (dragonBuyCooldown - (now - lastTime)) + " seconds before buying another egg.");
                return true;
            }
        }

        double playerBalance;
        try {
            playerBalance = EconomyAPI.getInstance().myMoney(player);
        } catch (Exception e) {
            getLogger().error("Error getting player balance: " + e.getMessage());
            player.sendMessage("§cAn error occurred while checking your balance.");
            return true;
        }

        if (playerBalance < effectivePrice) {
            player.sendMessage("§cYou do not have enough money to buy a dragon. It costs $" + effectivePrice + ".");
            return true;
        }

        try {
            EconomyAPI.getInstance().reduceMoney(player, effectivePrice);
            String eggId = eggManager.purchaseEgg(playerUUID);

            if (hasExistingEgg) {
                // Reset the existing egg data
                String oldEggId = dbManager.getDragonEggId(playerUUID);
                dbManager.removeDragonEgg(oldEggId);
            }

            CompoundTag tag = new CompoundTag().putString("eggId", eggId);
            Item dragonEgg = Item.get(BlockID.DRAGON_EGG, 0, 1);
            dragonEgg.setNamedTag(tag);
            updateEggLore(dragonEgg, dragonHatchingTime / 60);
            player.getInventory().addItem(dragonEgg);
            dbManager.insertDragonEgg(eggId, playerUUID);

            player.sendMessage("§aYou have successfully bought a dragon egg! It will hatch in " + dragonHatchingTime / 60 + " minutes.");
            lastPurchaseTime.put(playerUUID, System.currentTimeMillis() / 1000);
            return true;
        } catch (Exception e) {
            getLogger().error("Error during purchase: " + e.getMessage());
            player.sendMessage("§cAn error occurred during the purchase.");
            return true;
        }
    }

    public boolean handleSummonDragonCommand(Player player) {
         if (!player.hasPermission("rokidragon.command.summon")) {
            player.sendMessage("§cYou do not have permission to summon a dragon.");
            return true;
        }

        String playerUUID = player.getUniqueId().toString();

        if (!dbManager.playerHasDragon(playerUUID)) {
            if (!dbManager.hasDragonEgg(playerUUID)) {
                player.sendMessage("§cYou do not own a dragon. Use /summondragon buy to buy one for $" + dragonEggPrice + ".");
                return true;
            } else {
                String eggId = dbManager.getDragonEggId(playerUUID);
                if (eggId != null && !dbManager.isEggHatched(eggId)) {
                    player.sendMessage("§cYour dragon egg hasn't hatched yet!");
                    return true;
                }
            }
        }

        if (!dbManager.playerHasDragon(playerUUID)) {
            String eggId = dbManager.getDragonEggId(playerUUID);
             if (eggId != null) {
                Item dragonEgg = null;
                for (Item item : player.getInventory().getContents().values()) {
                    CompoundTag tag = item.getNamedTag();
                     if (tag != null && tag.contains("eggId") && tag.getString("eggId").equals(eggId)) {
                        dragonEgg = item;
                        getLogger().info("Found dragon egg for " + player.getName() + " with eggId: " + eggId + " (Item: " + item.getName() + ")");
                        break;
                    } else {
                         if(item.getId() == BlockID.DRAGON_EGG){
                            getLogger().info("Found a dragon egg item id but eggId does not match for " + player.getName() + " (Item: " + item.getName() + ")");
                            player.getInventory().removeItem(item);
                            player.sendMessage(TextFormat.RED + "A dragon egg has exploded in your inventory because it doesn't belong to you.");
                            player.setHealth(player.getHealth() - 1);
                        }
                    }
                }
                if(dragonEgg == null){
                    player.sendMessage("§cPlease hold the dragon egg in your inventory to summon the dragon!");
                    return true;
                }
                if (dbManager.isEggHatched(eggId)) {
                    // Summon the dragon and associate it with the player
                    FullChunk chunk = player.getChunk();
                    CompoundTag nbt = new CompoundTag()
                            .putDouble("x", player.x)
                            .putDouble("y", player.y)
                            .putDouble("z", player.z)
                            .putDouble("yaw", player.getYaw())
                            .putDouble("pitch", player.getPitch());
                    DragonEntity dragon = new DragonEntity(chunk, nbt);
                    dragon.setOwner(player);
                    dragon.setPosition(player.getPosition().add(2, 0, 0));
                    dragon.spawnToAll();
                    dbManager.insertDragon(playerUUID, eggId);
                    eggManager.removeEgg(playerUUID);
                    dbManager.removeDragonEgg(eggId);
                    player.getInventory().removeItem(dragonEgg);
                    player.sendMessage("§aYour dragon has been summoned!");
                    return true;
                } else {
                    player.sendMessage("§cYour dragon egg hasn't hatched yet!");
                    return true;
                }
            }
        } else {
            player.sendMessage("§cYou already have a dragon summoned!");
            return true;
        }
        return true;
    }

    public DragonEggManager getEggManager() {
        return eggManager;
    }

    private void updateEggLore(Item dragonEgg, long remainingMinutes) {
        List<String> lore = new ArrayList<>();
        if (remainingMinutes > 0) {
            lore.add(TextFormat.colorize("§7Time to hatch: §a" + remainingMinutes + "m"));
        } else {
            lore.add(TextFormat.colorize("§aEgg is ready to hatch!"));
        }
        dragonEgg.setLore(lore.toArray(new String[0]));
    }
}
