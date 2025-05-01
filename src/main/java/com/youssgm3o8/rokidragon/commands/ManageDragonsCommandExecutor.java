package com.youssgm3o8.rokidragon.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;

public class ManageDragonsCommandExecutor implements CommandExecutor {

    private final DragonPlugin plugin;
    private final FormBasedDragonGUI dragonGUI;

    public ManageDragonsCommandExecutor(DragonPlugin plugin, FormBasedDragonGUI dragonGUI) {
        this.plugin = plugin;
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

        try {
            // Logic moved from DragonPlugin.onCommand
            plugin.getLogger().info("Player " + player.getName() + " is opening the dragon management GUI via command.");
            
            // Open the main form menu
            dragonGUI.openMainMenu(player);
            
        } catch (Exception e) {
            plugin.getLogger().error("Error opening GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(TextFormat.RED + "There was an error opening the GUI. Please check the server logs.");
        }

        return true;
    }
}