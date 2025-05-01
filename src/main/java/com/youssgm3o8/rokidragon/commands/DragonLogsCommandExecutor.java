package com.youssgm3o8.rokidragon.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.data.DatabaseManager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;

public class DragonLogsCommandExecutor implements CommandExecutor {

    private final DragonPlugin plugin;
    private final DatabaseManager databaseManager;

    public DragonLogsCommandExecutor(DragonPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rokidragon.admin")) {
            sender.sendMessage(TextFormat.RED + plugin.getLanguageString("messages.errors.noPermissionAdmin"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendLogsHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                int limit = 10; // Default limit
                if (args.length > 1) {
                    try {
                        limit = Integer.parseInt(args[1]);
                        if (limit <= 0) {
                            sender.sendMessage(plugin.getLanguageString("messages.logs.invalidLimitPositive"));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getLanguageString("messages.logs.invalidLimitNumber", args[1]));
                        return true;
                    }
                }
                showRecentLogs(sender, limit);
                break;
            case "player":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getLanguageString("messages.logs.usagePlayer"));
                    return true;
                }
                showPlayerLogs(sender, args[1]);
                break;
            default:
                sendLogsHelp(sender);
        }

        return true;
    }

    private void sendLogsHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageString("messages.logs.helpTitle"));
        sender.sendMessage(plugin.getLanguageString("messages.logs.helpList"));
        sender.sendMessage(plugin.getLanguageString("messages.logs.helpPlayer"));
    }

    // Logic moved from DragonPlugin
    private void showRecentLogs(CommandSender sender, int limit) {
        List<Map<String, Object>> logs = databaseManager.getRecentLogs(limit);
        if (logs.isEmpty()) {
            sender.sendMessage(plugin.getLanguageString("messages.logs.noRecentLogs"));
            return;
        }

        sender.sendMessage(plugin.getLanguageString("messages.logs.headerRecent", limit));
        for (Map<String, Object> log : logs) {
            String playerName = getPlayerNameFromUUID((String) log.get("playerUUID"));
            sender.sendMessage(String.format("%s - Player: %s, EggID: %s, Location: %s",
                    log.get("timestamp"),
                    playerName,
                    log.get("eggId"),
                    log.get("lostLocation")));
        }
    }

    // Logic moved from DragonPlugin
    private void showPlayerLogs(CommandSender sender, String playerName) {
        Player targetPlayer = Server.getInstance().getPlayerExact(playerName);
        UUID targetUUID;

        if (targetPlayer != null) {
            targetUUID = targetPlayer.getUniqueId();
        } else {
            // Try offline player lookup if needed (requires appropriate API/method)
            // For now, assume online or exact name match required by DB method if it uses UUID
             try {
                 targetUUID = UUID.fromString(playerName); // Allow direct UUID input for offline players
             } catch (IllegalArgumentException e) {
                 // If not a valid UUID and player not online, try to get UUID from name if DB supports it
                 // This part depends heavily on how getPlayerLogs is implemented in DatabaseManager
                 // If getPlayerLogs expects a UUID string, we need to get it first.
                 // If it can handle a name, we can pass the name directly.
                 // Assuming getPlayerLogs needs UUID string for now:
                 sender.sendMessage(plugin.getLanguageString("messages.logs.playerNotFound", playerName));
                 // Alternatively, implement offline player UUID lookup if possible.
                 return; // Cannot proceed without UUID
             }
        }
        
        String playerUUIDString = targetUUID.toString();
        List<Map<String, Object>> logs = databaseManager.getPlayerLogs(playerUUIDString); // Assumes DB method takes UUID string

        if (logs.isEmpty()) {
            sender.sendMessage(plugin.getLanguageString("messages.logs.noPlayerLogs", playerName));
            return;
        }

        sender.sendMessage(plugin.getLanguageString("messages.logs.headerPlayer", playerName));
        for (Map<String, Object> log : logs) {
            sender.sendMessage(String.format("%s - EggID: %s, Location: %s",
                    log.get("timestamp"),
                    log.get("eggId"),
                    log.get("lostLocation")));
        }
    }

    // Helper to get player name (consider caching or using a better method if available)
    private String getPlayerNameFromUUID(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            Player player = Server.getInstance().getPlayer(uuid).orElse(null);
            if (player != null) {
                return player.getName();
            }
            // Add offline player lookup here if needed/possible
            return uuidString; // Fallback to UUID if name not found
        } catch (IllegalArgumentException e) {
            return uuidString; // Return original string if not a valid UUID
        }
    }
}