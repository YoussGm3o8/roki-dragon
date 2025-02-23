package com.youssgm3o8.rokidragon.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.gui.DragonManagementGUI;

public class DragonManageCommand extends Command {
    private final DragonPlugin plugin;
    private final DragonManagementGUI gui;

    public DragonManageCommand(DragonPlugin plugin, DragonManagementGUI gui) {
        super("dragonmanage", "Open the dragon management interface", "/dragonmanage", new String[]{"dm", "dragongui"});
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

        gui.openMainMenu((Player) sender);
        return true;
    }
} 