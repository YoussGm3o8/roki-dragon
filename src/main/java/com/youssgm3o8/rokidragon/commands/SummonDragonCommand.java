package com.youssgm3o8.rokidragon.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.DatabaseManager;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;
import com.youssgm3o8.rokidragon.manager.DragonEggManager;
import com.youssgm3o8.rokidragon.manager.DragonManager;
import com.youssgm3o8.rokidragon.manager.DragonShardManager;
import cn.nukkit.utils.TextFormat; // Added import
import cn.nukkit.item.Item; // Added import
import java.util.UUID; // Added import

public class SummonDragonCommand extends Command {

    private final DragonPlugin plugin;
    private final DatabaseManager databaseManager;
    private final DragonEggManager eggManager;
    private final DragonShardManager shardManager;
    private final FormBasedDragonGUI dragonGUI;
    private final DragonManager dragonManager;

    public SummonDragonCommand(String name, String description, String usageMessage, DragonPlugin plugin) {
        super(name, description, usageMessage);
        this.plugin = plugin;
        // Inject managers from plugin
        this.databaseManager = plugin.getDatabaseManager();
        this.eggManager = plugin.getEggManager();
        this.shardManager = plugin.getShardManager();
        this.dragonGUI = plugin.getDragonGUI();
        this.dragonManager = plugin.getDragonManager();
    }

    

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("buy")) {
                return handleBuyCommand(player);
            } else if (args[0].equalsIgnoreCase("lost")) {
                return handleLostCommand(player);
            } else if (args[0].equalsIgnoreCase("admin")) {
                if (args.length < 2) {
                    player.sendMessage(TextFormat.RED + "Usage: /summondragon admin <player>");
                    return true;
                }
                String targetName = args[1];
                return handleAdminHatchCommand(player, targetName);
            }
        }
        // Default action is summon
        return handleSummonCommand(player);
    }

    // --- Reimplemented Logic from DragonPlugin ---

    private boolean handleBuyCommand(Player player) {
        String playerUUID = player.getUniqueId().toString();
        int storedEggCount = databaseManager.getPlayerStoredEggCount(player.getUniqueId());
        boolean hasActiveDragon = databaseManager.playerHasDragon(playerUUID);
        int totalEggCount = storedEggCount + (hasActiveDragon ? 1 : 0);
        int maxEggs = plugin.getConfig().getInt("gui.storage_size", 5); // Use config

        if (totalEggCount >= maxEggs) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.maxEggs", maxEggs));
            return true;
        }

        // Show the purchase menu using the new form-based GUI
        dragonGUI.openDragonPurchaseMenu(player);
        return true;
    }

    private boolean handleLostCommand(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (!databaseManager.playerHasDragon(playerUUID)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.noDragon"));
            return true;
        }

        String eggId = databaseManager.getDragonEggId(playerUUID);
         if (eggId == null) {
             player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.generic"));
             return true;
        }

        if (databaseManager.isEggHatched(eggId)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.alreadyHatched"));
            return true;
        }

        // Give the player a new egg item (does not affect DB state)
        String dragonType = databaseManager.getDragonType(eggId);
        String dragonName = databaseManager.getDragonName(eggId);
        Item eggItem = eggManager.createDragonEgg(dragonType, dragonName, UUID.fromString(eggId));
        player.getInventory().addItem(eggItem);
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.success.eggReplaced"));
        return true;
    }

    private boolean handleAdminHatchCommand(Player admin, String targetName) {
        if (!admin.hasPermission("rokidragon.admin")) {
            admin.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.admin.permissionDenied"));
            return true;
        }

        Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
        if (targetPlayer == null) {
            admin.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.admin.playerNotFound", targetName));
            return true;
        }

        String targetUUID = targetPlayer.getUniqueId().toString();
        if (!databaseManager.playerHasDragon(targetUUID)) {
            admin.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.admin.noDragonEgg", targetName));
            return true;
        }

        String eggId = databaseManager.getDragonEggId(targetUUID);
         if (eggId == null) {
             admin.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.generic"));
             return true;
        }

        if (databaseManager.isEggHatched(eggId)) {
            admin.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.admin.alreadyHatched", targetName));
            return true;
        }

        // Hatch the egg
        databaseManager.setEggHatched(eggId, true);

        // Give initial shards
        String dragonType = databaseManager.getDragonType(eggId);
        shardManager.giveInitialShards(dragonType, targetPlayer);

        admin.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.admin.eggHatched", targetName));
        targetPlayer.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.admin.eggHatchedNotification"));
        return true;
    }

    private boolean handleSummonCommand(Player player) {
        String playerUUID = player.getUniqueId().toString();

        if (!databaseManager.playerHasDragon(playerUUID)) {
             player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.noDragon"));
            return true;
        }

        String eggId = databaseManager.getDragonEggId(playerUUID);
         if (eggId == null) {
             player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.generic"));
             return true;
        }

        if (!databaseManager.isEggHatched(eggId)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.notHatched"));
            return true;
        }

        // Use the new method to check for active dragons
        if (plugin.hasActiveDragon(player)) {
            player.sendMessage(TextFormat.YELLOW + plugin.getLanguageString("messages.errors.alreadySummoned"));
            return true;
        }

        // Check cooldown (using the main plugin's cooldown map)
        if (plugin.isOnCooldown(player.getName())) {
            long remainingSeconds = plugin.getCooldownTime(player.getName());
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.cooldown", remainingSeconds));
            return true;
        }

        // Check if player is in an allowed world
        if (plugin.areWorldRestrictionsEnabled() && !plugin.isWorldAllowed(player.getLevel().getName())) {
            player.sendMessage(TextFormat.RED + "Dragons cannot be summoned in this world.");
            return true;
        }

        plugin.getLogger().info("Attempting to summon dragon for " + player.getName());
        
        // Summon dragon using the manager
        com.youssgm3o8.rokidragon.dragon.DragonEntity dragon = dragonManager.spawnDragon(player);
        if (dragon != null) {
            // Register the dragon using the new method
            plugin.registerActiveDragon(player, dragon);

            // Set cooldown using the main plugin's method
            int summonCooldown = plugin.getConfig().getInt("timing.cooldowns.summon_seconds", 30);
            plugin.setCooldown(player.getName(), summonCooldown);
        } else {
            plugin.getLogger().warning("Failed to summon dragon for " + player.getName());
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.summonFailed"));
            
            // Suggest a solution to the player
            player.sendMessage(TextFormat.YELLOW + "Try moving to a different location or relogging.");
        }
        return true;
    }
}
