package com.youssgm3o8.rokidragon.commands;

import java.text.MessageFormat;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.DatabaseManager;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;
import com.youssgm3o8.rokidragon.manager.DragonEggManager;
import com.youssgm3o8.rokidragon.manager.DragonShardManager;
import com.youssgm3o8.rokidragon.util.DragonUtils; // Import the utility class

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
public class DragonCommandExecutor implements CommandExecutor {

    private final DragonPlugin plugin;
    private final DatabaseManager databaseManager;
    private final DragonEggManager eggManager;
    private final DragonShardManager shardManager;
    private final FormBasedDragonGUI dragonGUI; // Updated to form-based GUI

    public DragonCommandExecutor(DragonPlugin plugin, DatabaseManager databaseManager, DragonEggManager eggManager, DragonShardManager shardManager, FormBasedDragonGUI dragonGUI) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.eggManager = eggManager;
        this.shardManager = shardManager;
        this.dragonGUI = dragonGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.playerOnly"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("rokidragon.use")) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.noPermission"));
            return true;
        }
        
        // Check if the player is in an allowed world
        if (!plugin.isPlayerInAllowedWorld(player)) {
            return true; // Message already sent by isPlayerInAllowedWorld
        }

        if (args.length == 0) {
            // Open the dragon management GUI instead of showing help
            dragonGUI.openMainMenu(player);
            return true;
        }

        String playerUUID = player.getUniqueId().toString();

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(player);
                break;
            case "info":
                handleInfoCommand(player, playerUUID);
                break;
            case "lost":
                // Open the lost eggs menu with the form-based GUI
                dragonGUI.openLostEggsMenu(player);
                break;
            case "name":
                handleNameCommand(player, playerUUID, args);
                break;
            case "recipes":
                shardManager.showRecipesToPlayer(player);
                break;
            default:
                sendHelp(player);
        }
        return true;
    }

    private void handleInfoCommand(Player player, String playerUUID) {
        // Use the form-based dragon info instead of sending messages
        dragonGUI.showDragonInfo(player);
    }

    private void handleNameCommand(Player player, String playerUUID, String[] args) {
        if (args.length < 2) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.commandUsage.dragonName"));
            return;
        }

        if (!databaseManager.playerHasDragon(playerUUID)) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.noDragon"));
            return;
        }

        // Reconstruct name with spaces
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        String newName = nameBuilder.toString();

        // Validate name length (consider making limits configurable)
        int minLen = plugin.getConfig().getInt("naming.min_length", 3); // Updated key
        int maxLen = plugin.getConfig().getInt("naming.max_length", 16); // Updated key
        if (newName.length() < minLen || newName.length() > maxLen) {
            player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.nameLength", minLen, maxLen));
            return;
        }

        // Update name in database
        String eggID = databaseManager.getDragonEggId(playerUUID);
         if (eggID == null) {
             player.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.generic"));
             return;
        }
        databaseManager.setDragonName(eggID, newName);

        player.sendMessage(TextFormat.GREEN + MessageFormat.format(
            plugin.getLanguageString("messages.success.dragonRenamed"),
            newName
        ));
        
        // If dragon is active, update its nameplate
        if (plugin.hasActiveDragon(player)) {
            plugin.getActiveDragons().get(player.getUniqueId()).setNameTag(newName);
        }
    }


    private void sendHelp(Player player) {
        player.sendMessage(TextFormat.GREEN + plugin.getLanguageString("messages.help.title"));
        player.sendMessage(TextFormat.YELLOW + "/dragon - " + TextFormat.WHITE + "Open the dragon management GUI");
        player.sendMessage(plugin.getLanguageString("messages.help.help"));
        player.sendMessage(plugin.getLanguageString("messages.help.info"));
        player.sendMessage(plugin.getLanguageString("messages.help.name"));
        player.sendMessage(plugin.getLanguageString("messages.help.recipes"));
        player.sendMessage(plugin.getLanguageString("messages.help.lost"));
    }

    // Removed duplicate getColorByType method
}