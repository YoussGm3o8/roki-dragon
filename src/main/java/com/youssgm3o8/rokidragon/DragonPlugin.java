package com.youssgm3o8.rokidragon;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.event.Listener;
import cn.nukkit.permission.Permission;
import me.onebone.economyapi.EconomyAPI;
import cn.nukkit.entity.custom.EntityManager;
import com.youssgm3o8.rokidragon.util.DragonEggManager;
import com.youssgm3o8.rokidragon.entities.DragonEntity;
import com.youssgm3o8.rokidragon.commands.SummonDragonCommand;
import com.youssgm3o8.rokidragon.entities.EventListenerEdit;

import cn.nukkit.level.format.FullChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

public class DragonPlugin extends PluginBase implements Listener {
    private DragonEggManager eggManager;
    private DatabaseManager dbManager;
    private double dragonEggPrice;
    private int dragonHatchingTime;
    private int dragonBuyCooldown;
    private Map<String, Long> lastPurchaseTime = new HashMap<>();
    public static DragonPlugin instance;

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

                // Process all items in inventory without stopping on first match.
                Item dragonEgg = null;
                for (Item item : p.getInventory().getContents().values()) {
                    CompoundTag tag = item.getNamedTag();
                    if (tag != null && tag.contains("eggId")) {
                        String currentEggId = tag.getString("eggId");
                        if (currentEggId.equals(eggId)) {
                            // Store the valid egg but continue iteration.
                            dragonEgg = item;
                        } else {
                            getLogger().warning("Found a dragon egg that doesn't belong to " + p.getName() + " (Item: " + item.getName() + ")");
                            p.getInventory().removeItem(item);
                            p.sendMessage(TextFormat.RED + "A dragon egg has exploded in your inventory because it doesn't belong to you.");
                            p.setHealth(p.getHealth() - 1);
                        }
                    } else {
                        if (item.getId() == BlockID.DRAGON_EGG) {
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
        instance = this;
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
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("summondragon")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("buy")) {
                    return handleBuyDragonCommand(player);
                } else if (args[0].equalsIgnoreCase("lost")) {
                    return handleLostEggCommand(player);
                } else if (args[0].equalsIgnoreCase("admin")) {
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /summondragon admin <player>");
                        return true;
                    }
                    String targetName = args[1];
                    return handleAdminHatchEggCommand(player, targetName);
                }
            }
            return handleSummonDragonCommand(player);
        }
        return false;
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
        if (dbManager.playerHasDragon(playerUUID)) {
            player.sendMessage("§cYou already own a dragon.");
            return true;
        }
        
        double effectivePrice = dragonEggPrice;
        // If an egg record already exists, prompt to use /summondragon lost instead.
        if (dbManager.hasDragonEgg(playerUUID)) {
            effectivePrice = dragonEggPrice * 0.5;
            player.sendMessage("§cYou already bought an egg in the past. If you lost it,  You can repurchase it at a reduced cost of $" + effectivePrice + ". use /summondragon lost.");
            return true;
        }

        boolean hasExistingEgg = dbManager.hasDragonEgg(playerUUID);

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

    public boolean handleLostEggCommand(Player player) {
        // Reuse the same permission as buying an egg
        if (!player.hasPermission("rokidragon.command.buy")) {
            player.sendMessage("§cYou do not have permission to repurchase a lost egg.");
            return true;
        }
        
        String playerUUID = player.getUniqueId().toString();
        // Check if a dragon is already active
        if (dbManager.playerHasDragon(playerUUID)) {
            player.sendMessage("§cYou already have a dragon summoned.");
            return true;
        }
        
        // Get the egg record from database
        String eggId = dbManager.getDragonEggId(playerUUID);
        // Scan player inventory for an egg with matching tag
        boolean eggFound = false;
        for (Item item : player.getInventory().getContents().values()) {
            CompoundTag tag = item.getNamedTag();
            if (tag != null && tag.contains("eggId") && tag.getString("eggId").equals(eggId)) {
                eggFound = true;
                break;
            }
        }
        if (eggFound) {
            player.sendMessage("§cYour egg is still in your inventory. Wait for the egg to hatch then use /summondragon to summon your dragon.");
            return true;
        }
        
        // Proceed to repurchase lost egg at reduced cost if an egg record exists
        if (eggId == null) {
            player.sendMessage("§cYou do not have an egg record. Use /summondragon buy to purchase a dragon egg for $" + dragonEggPrice + ".");
            return true;
        }
        
        double effectivePrice = dragonEggPrice * 0.5; // Repurchase is at half price
        double playerBalance;
        try {
            playerBalance = EconomyAPI.getInstance().myMoney(player);
        } catch (Exception e) {
            getLogger().error("Error getting player balance: " + e.getMessage());
            player.sendMessage("§cAn error occurred while checking your balance.");
            return true;
        }
        
        if (playerBalance < effectivePrice) {
            player.sendMessage("§cYou do not have enough money to repurchase your lost egg. It costs $" + effectivePrice + ".");
            return true;
        }
        
        try {
            EconomyAPI.getInstance().reduceMoney(player, effectivePrice);
            // Generate a new eggId for the repurchased egg
            String newEggId = eggManager.purchaseEgg(playerUUID);
            CompoundTag tag = new CompoundTag().putString("eggId", newEggId);
            Item dragonEgg = Item.get(BlockID.DRAGON_EGG, 0, 1);
            dragonEgg.setNamedTag(tag);
            updateEggLore(dragonEgg, dragonHatchingTime / 60);
            player.getInventory().addItem(dragonEgg);
            // Replace the old record with the new eggId
            dbManager.removeDragonEgg(eggId);
            dbManager.insertDragonEgg(newEggId, playerUUID);
        
            player.sendMessage("§aYou have successfully repurchased your lost dragon egg at a reduced cost of $" + effectivePrice + "!");
            lastPurchaseTime.put(playerUUID, System.currentTimeMillis() / 1000);
            return true;
        } catch (Exception e) {
            getLogger().error("Error during repurchase: " + e.getMessage());
            player.sendMessage("§cAn error occurred during the repurchase.");
            return true;
        }
    }

    public boolean handleSummonDragonCommand(Player player) {
        String playerUUID = player.getUniqueId().toString();

        // If a dragon exists, despawn it.
        if (dbManager.playerHasDragon(playerUUID)) {
            despawnDragon(player);
            player.sendMessage("§aYour dragon has been despawned!");
            return true;
        }
        
        // Proceed to summon if no dragon is spawned.
        if (!dbManager.hasDragonEgg(playerUUID)) {
            player.sendMessage("§cYou do not own a dragon. Use /summondragon buy to buy one for $" + dragonEggPrice + ".");
            return true;
        } else {
            String eggId = dbManager.getDragonEggId(playerUUID);
            if (eggId != null && !dbManager.isEggHatched(eggId)) {
                double effectivePrice = dragonEggPrice;
                effectivePrice = dragonEggPrice * 0.5;
                player.sendMessage("§cYour dragon egg hasn't hatched yet! If you lost it, use /summondragon lost to repurchase it. It costs $" + effectivePrice + ".");
                return true;
            }
        }

        // NEW: Before summoning, verify that all eggs in inventory belong to the player.
        String eggId = dbManager.getDragonEggId(playerUUID);
        for (Item item : player.getInventory().getContents().values()) {
            CompoundTag tag = item.getNamedTag();
            if (tag != null && tag.contains("eggId")) {
                if (!tag.getString("eggId").equals(eggId)) {
                    player.sendMessage("§cThis egg does not belong to you. Summoning cancelled.");
                    return true;
                }
            }
        }
        
        // Summon the dragon from the egg in inventory.
        Item dragonEgg = null;
        for (Item item : player.getInventory().getContents().values()) {
            CompoundTag tag = item.getNamedTag();
            if (tag != null && tag.contains("eggId")) {
                if (tag.getString("eggId").equals(eggId)) {
                    dragonEgg = item;
                    break;
                }
            }
        }
        if (dragonEgg == null) {
            player.sendMessage("§cPlease hold the dragon egg in your inventory to summon the dragon!");
            return true;
        }
        if (dbManager.isEggHatched(eggId)) {
            FullChunk chunk = player.getChunk();
            // Replace CompoundTag creation with properly built list tags
            CompoundTag nbt = new CompoundTag();
            // Build the "Pos" list
            ListTag<DoubleTag> posList = new ListTag<>("Pos");
            posList.add(new DoubleTag("", player.x));
            posList.add(new DoubleTag("", player.y));
            posList.add(new DoubleTag("", player.z));
            nbt.put("Pos", posList);
            // Build the "Motion" list
            ListTag<DoubleTag> motionList = new ListTag<>("Motion");
            motionList.add(new DoubleTag("", 0));
            motionList.add(new DoubleTag("", 0));
            motionList.add(new DoubleTag("", 0));
            nbt.put("Motion", motionList);
            // Build the "Rotation" list
            ListTag<FloatTag> rotationList = new ListTag<>("Rotation");
            rotationList.add(new FloatTag("", (float) player.getYaw()));
            rotationList.add(new FloatTag("", (float) player.getPitch()));
            nbt.put("Rotation", rotationList);
            
            DragonEntity dragon = new DragonEntity(chunk, nbt);
            dragon.setOwner(player);
            dragon.setPosition(player.getPosition().add(2, 0, 0));
            dragon.spawnToAll();
            dbManager.insertDragon(playerUUID, eggId);
            player.sendMessage("§aYour dragon has been summoned!");
            return true;
        } else {
            player.sendMessage("§cYour dragon egg hasn't hatched yet!");
            return true;
        }    
    }

    public boolean handleAdminHatchEggCommand(Player issuer, String targetName) {
        Player target = getServer().getPlayerExact(targetName);
        if (target == null) {
            issuer.sendMessage("§cTarget player not found.");
            return true;
        }
        String targetUUID = target.getUniqueId().toString();
        if (dbManager.playerHasDragon(targetUUID)) {
            issuer.sendMessage("§cThat player already has a dragon.");
            return true;
        }
        // Look for a dragon egg in the target's inventory
        cn.nukkit.item.Item foundEgg = null;
        String eggId = null;
        for (cn.nukkit.item.Item item : target.getInventory().getContents().values()) {
            if (item.getId() == cn.nukkit.block.BlockID.DRAGON_EGG && item.hasCompoundTag()) {
                eggId = item.getNamedTag().getString("eggId");
                if (eggId != null && !eggId.isEmpty()) {
                    foundEgg = item;
                    break;
                }
            }
        }
        if (foundEgg == null) {
            issuer.sendMessage("§cThat player does not have a dragon egg in their inventory.");
            return true;
        }
        dbManager.markEggAdminHatched(eggId);

        // Add lore update for admin-hatched eggs:
        for (cn.nukkit.item.Item item : target.getInventory().getContents().values()) {
            if (item.getId() == cn.nukkit.block.BlockID.DRAGON_EGG &&
                item.hasCompoundTag() &&
                item.getNamedTag().contains("eggId") &&
                item.getNamedTag().getString("eggId").equals(eggId)) {
                    
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(cn.nukkit.utils.TextFormat.colorize("§aEgg is hatched! Do /summondragon to spawn or crack the egg on the ground"));
                item.setLore(lore.toArray(new String[0]));
                break;
            }
        }

        issuer.sendMessage("§aThe egg for " + target.getName() + " has been marked as hatched. They must use /summondragon to summon or crack the egg on the ground.");
        target.sendMessage("§aYour dragon egg has hatched! Do /summondragon to spawn or crack the egg on the ground.");
        return true;
    }

    // Change method visibility from private to public
    public void despawnDragon(Player player) {
        String playerUUID = player.getUniqueId().toString();
        // Loop over all levels to find and despawn the player's dragon entity.
        for (cn.nukkit.level.Level level : getServer().getLevels().values()) {
            for (cn.nukkit.entity.Entity entity : level.getEntities()) {
                if (entity instanceof DragonEntity) {
                    DragonEntity dragon = (DragonEntity) entity;
                    if (dragon.getOwner() != null && dragon.getOwner().equals(player)) {
                        // NEW: Unmount all passengers before despawning
                        dragon.dismountAllPassengers();
                        dragon.close();
                    }
                }
            }
        }
        getDatabaseManager().removeDragon(playerUUID);
    }

    public DragonEggManager getEggManager() {
        return eggManager;
    }

    public static DragonPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    private void updateEggLore(Item dragonEgg, long remainingMinutes) {
        List<String> lore = new ArrayList<>();
        if (remainingMinutes > 0) {
            lore.add(TextFormat.colorize("§7Time to hatch: §a" + remainingMinutes + "m"));
        } else {
            lore.add(TextFormat.colorize("§aEgg is ready to hatch!"));
        }
        // Adding additional lore details:
        lore.add(TextFormat.colorize("§7Keep this egg warm with your body heat to hatch it."));
        lore.add(TextFormat.colorize("§7Do /summondragon when hatched to summon your dragon."));
        lore.add(TextFormat.colorize("§r§4§lWarning: It may explode if it doesn't belong to you!"));
        dragonEgg.setLore(lore.toArray(new String[0]));
    }
}
