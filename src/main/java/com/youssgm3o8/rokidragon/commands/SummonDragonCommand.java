package com.youssgm3o8.rokidragon.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.youssgm3o8.rokidragon.DragonPlugin;

public class SummonDragonCommand extends Command {

    private final DragonPlugin plugin;

    public SummonDragonCommand(String name, String description, String usageMessage, DragonPlugin plugin) {
        super(name, description, usageMessage);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("buy")) {
            return plugin.handleBuyDragonCommand(player);
        } else {
            return plugin.handleSummonDragonCommand(player);
        }
    }
}
