package com.youssgm3o8.rokidragon.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.FormBasedDragonGUI;

public class DragonManageCommand extends Command {
    private final DragonPlugin plugin;
    private final FormBasedDragonGUI gui;

    public DragonManageCommand(DragonPlugin plugin, FormBasedDragonGUI gui) {
        super("dragonmanage", "Open the dragon management interface", "/dragonmanage [lost]", new String[]{"dm", "dragongui"});
        this.setPermission("rokidragon.command.manage");
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return false;
        }

        Player player = (Player) sender;
        
        try {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "lost":
                        // Handle the 'lost' subcommand by opening the lost eggs GUI
                        gui.openLostEggsMenu(player);
                        return true;
                    case "recipes":
                        // Show crafting recipes for dragon shards
                        plugin.getShardManager().showRecipesToPlayer(player);
                        return true;
                }
            }

            // Default behavior - open the main menu
            plugin.getLogger().info("Opening main dragon GUI for " + player.getName());
            gui.openMainMenu(player);
            
        } catch (Exception e) {
            plugin.getLogger().error("Error opening GUI for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cThere was an error opening the GUI. Please check the server logs.");
        }
        
        return true;
    }
} 