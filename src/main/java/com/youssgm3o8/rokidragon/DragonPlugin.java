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
import com.youssgm3o8.rokidragon.commands.DragonManageCommand;
import com.youssgm3o8.rokidragon.commands.SummonDragonCommand;
import com.youssgm3o8.rokidragon.entities.EventListenerEdit;
import com.youssgm3o8.rokidragon.gui.DragonManagementGUI;
import com.youssgm3o8.rokidragon.items.DragonShardManager;
import com.youssgm3o8.rokidragon.listeners.DragonGUIListener;
import com.youssgm3o8.rokidragon.gui.inventory.DragonInventoryListener;

import cn.nukkit.level.format.FullChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import java.io.File;
import cn.nukkit.utils.Config;
import java.util.UUID;
import java.util.stream.Collectors;

public class DragonPlugin extends PluginBase implements Listener {
    private DragonEggManager eggManager;
    private DatabaseManager dbManager;
    private double dragonEggPrice;
    private int dragonHatchingTime;
    private int dragonBuyCooldown;
    private Map<String, Long> lastPurchaseTime = new HashMap<>();
    public static DragonPlugin instance;
    private DragonShardManager shardManager;
    private Config langConfig;
    private String currentLanguage;
    private Map<String, Long> dragonDeathCooldowns = new HashMap<>();
    private static final long DRAGON_RESPAWN_COOLDOWN = 15 * 60 * 1000; // 15 minutes in milliseconds
    private DragonManagementGUI managementGUI;

    @Override
    public void onLoad() {
        // Load SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            getLogger().error("Failed to load SQLite JDBC driver", e);
        }

        // Register the dragon entity
        EntityManager.get().registerDefinition(DragonEntity.DEFINITION);
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
        
        // Load language configuration
        loadLanguageConfig();
        
        // Initialize managers
        this.eggManager = new DragonEggManager(this);
        this.dbManager = new DatabaseManager(this);
        this.shardManager = new DragonShardManager(this);

        // Initialize GUI system and register command
        this.managementGUI = new DragonManagementGUI(this);
        getServer().getPluginManager().registerEvents(new DragonGUIListener(this, managementGUI), this);
        getServer().getPluginManager().registerEvents(new DragonInventoryListener(), this);
        getServer().getCommandMap().register("dragonmanage", new DragonManageCommand(this, managementGUI));

        // Register other commands
        getServer().getCommandMap().register("summondragon", new SummonDragonCommand("summondragon", "Summon or buy a dragon", "/summondragon [buy]", this));

        // Register permissions
        registerPermissions();

        // Schedule task to check incubation conditions
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            for (Player p : getServer().getOnlinePlayers().values()) {
                String playerUUID = p.getUniqueId().toString();
                String eggId = dbManager.getDragonEggId(playerUUID);

                if (eggId != null && !dbManager.isEggHatched(eggId)) {
                    // Process all items in inventory
                    Item dragonEgg = null;
                    for (Item item : p.getInventory().getContents().values()) {
                        if (item.getId() == BlockID.DRAGON_EGG && 
                            item.hasCompoundTag() && 
                            item.getNamedTag().getString("eggId").equals(eggId)) {
                            dragonEgg = item;
                            break;
                        }
                    }

                    if (dragonEgg != null) {
                        // Update incubation conditions
                        eggManager.updateIncubationConditions(p, eggId);
                        
                        int onlineTime = dbManager.getEggOnlineTime(eggId);
                        if (onlineTime >= dragonHatchingTime / 60) {
                            // When egg hatches, determine type based on incubation conditions
                            String dragonType = eggManager.determineDragonType(eggId);
                            getLogger().info("Determined dragon type: " + dragonType + " for egg " + eggId);
                            dbManager.setEggHatched(eggId);
                            dbManager.setDragonType(eggId, dragonType);
                            getLogger().info("Saved dragon type to database for egg " + eggId);
                            p.sendMessage("§aYour dragon egg has hatched into a " + dragonType + "!");
                            updateEggLore(dragonEgg, 0);
                            onDragonHatch(p, dragonType);
                        } else {
                            dbManager.incrementEggOnlineTime(eggId);
                            onlineTime = dbManager.getEggOnlineTime(eggId);
                            updateEggLore(dragonEgg, (dragonHatchingTime / 60) - onlineTime);
                        }
                    }
                }
            }
        }, 1200, true); // Check every minute

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
            return handleSummonDragonCommand(player, args);
        } else if (command.getName().equalsIgnoreCase("dragonmanage")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            if (!sender.hasPermission("rokidragon.command.manage")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            
            managementGUI.openMainMenu((Player) sender);
            return true;
        }
        return false;
    }

    // Tab completion for commands
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("summondragon") || 
            command.getName().equalsIgnoreCase("dragon") || 
            command.getName().equalsIgnoreCase("drg")) {
            
            if (args.length == 1) {
                completions.add("buy");
                completions.add("lost");
                if (sender.hasPermission("rokidragon.admin")) {
                    completions.add("admin");
                }
                return filterCompletions(completions, args[0]);
            }
            
            if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("rokidragon.admin")) {
                for (Player player : getServer().getOnlinePlayers().values()) {
                    completions.add(player.getName());
                }
                return filterCompletions(completions, args[1]);
            }
        }
        
        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String partial) {
        String lowercasePartial = partial.toLowerCase();
        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(lowercasePartial))
            .collect(Collectors.toList());
    }

    private void loadConfig() {
        saveDefaultConfig();
        dragonEggPrice = getConfig().getDouble("dragon-egg-price", 128000);
        dragonHatchingTime = getConfig().getInt("dragon-hatching-time", 3600);
        dragonBuyCooldown = getConfig().getInt("dragon-buy-cooldown", 60);
        // Load dragon stats from config
        int maxHealth = getConfig().getInt("dragon.max-health", 100);
        double baseDamage = getConfig().getDouble("dragon.base-damage", 15.0);
        double damageReduction = getConfig().getDouble("dragon.damage-reduction", 0.25);
        DragonEntity.setDefaultStats(maxHealth, baseDamage, damageReduction);
    }

    private void loadLanguageConfig() {
        // Get language settings from config
        String defaultLang = getConfig().getString("language.default", "en_US");
        String fallbackLang = getConfig().getString("language.fallback", "en_US");
        
        // Save default language files
        saveResource("lang/en_US.yml", false);
        saveResource("lang/es_ES.yml", false); // Add other language files as needed
        
        // Try to load the default language
        File langFile = new File(getDataFolder(), "lang/" + defaultLang + ".yml");
        if (langFile.exists()) {
            langConfig = new Config(langFile, Config.YAML);
            currentLanguage = defaultLang;
        } else {
            // If default language file doesn't exist, use fallback
            File fallbackFile = new File(getDataFolder(), "lang/" + fallbackLang + ".yml");
            if (fallbackFile.exists()) {
                langConfig = new Config(fallbackFile, Config.YAML);
                currentLanguage = fallbackLang;
                getLogger().warning("Default language file not found. Using fallback language: " + fallbackLang);
            } else {
                // If fallback doesn't exist, create it with default values
                langConfig = new Config(fallbackFile, Config.YAML);
                saveResource("lang/" + fallbackLang + ".yml", true);
                currentLanguage = fallbackLang;
                getLogger().error("No language files found. Created default " + fallbackLang + " file.");
            }
        }
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

    public boolean handleSummonDragonCommand(Player player, String[] args) {
        String playerUUID = player.getUniqueId().toString();

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "buy":
                    return handleBuyDragonCommand(player);
                case "lost":
                    return handleLostEggCommand(player);
                case "admin":
                    if (!player.hasPermission("rokidragon.admin")) {
                        player.sendMessage(TextFormat.RED + "You don't have permission to use admin commands.");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(TextFormat.RED + "Usage: /dragon admin <player>");
                        return true;
                    }
                    return handleAdminHatchEggCommand(player, args[1]);
                default:
                    player.sendMessage(TextFormat.RED + "Unknown subcommand. Usage: /dragon [buy|lost|admin <player>]");
                    return true;
            }
        }

        // Check if player has a dragon egg
        String eggId = dbManager.getDragonEggId(playerUUID);
        if (eggId == null) {
            player.sendMessage(TextFormat.RED + "You don't have a dragon egg! Use " + 
                TextFormat.WHITE + "/dragon buy" + TextFormat.RED + " to purchase one.");
            return true;
        }

        // Check if egg is hatched
        if (!dbManager.isEggHatched(eggId)) {
            player.sendMessage(TextFormat.RED + "Your dragon egg hasn't hatched yet! " + 
                "You need to incubate it in the right environment.");
            player.sendMessage(TextFormat.GRAY + "Open your inventory and right-click the egg to start incubation.");
            return true;
        }

        // Check if dragon is already spawned
        if (dbManager.playerHasDragon(playerUUID)) {
            player.sendMessage(TextFormat.RED + "Your dragon is already spawned! " + 
                "If you can't find it, try relogging or using /dragon lost.");
            return true;
        }

        // Check if player is holding the dragon egg
        Item heldItem = player.getInventory().getItemInHand();
        if (heldItem.getId() != BlockID.DRAGON_EGG || !heldItem.hasCompoundTag() || 
            !heldItem.getNamedTag().contains("eggId") || 
            !heldItem.getNamedTag().getString("eggId").equals(eggId)) {
            player.sendMessage(TextFormat.RED + "You must be holding your dragon egg to summon your dragon!");
            return true;
        }

        // Get dragon type
        String dragonType = dbManager.getDragonType(eggId);
        if (dragonType == null || dragonType.isEmpty()) {
            dragonType = "Fire Dragon"; // Default type
        }

        // Create and spawn the dragon
        DragonEntity dragon = new DragonEntity(player.getChunk(), DragonEntity.getDefaultNBT(player.getPosition()));
        dragon.setOwner(player);
        dragon.setDragonType(dragonType);
        dragon.spawnToAll();

        // Register the dragon in the database
        String dragonId = UUID.randomUUID().toString();
        dbManager.insertDragon(playerUUID, dragonId);

        player.sendMessage(TextFormat.GREEN + "Your " + getColorForDragonType(dragonType) + 
            dragonType + TextFormat.GREEN + " has been summoned!");
        return true;
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
        
        // Check if player is holding the dragon egg (except for player quit event)
        if (player.isOnline()) {
            String eggId = dbManager.getDragonEggId(playerUUID);
            Item heldItem = player.getInventory().getItemInHand();
            if (eggId != null && (heldItem.getId() != BlockID.DRAGON_EGG || !heldItem.hasCompoundTag() || 
                !heldItem.getNamedTag().contains("eggId") || 
                !heldItem.getNamedTag().getString("eggId").equals(eggId))) {
                player.sendMessage(TextFormat.RED + "You must be holding your dragon egg to despawn your dragon!");
                return;
            }
        }
        
        // Loop over all levels to find and despawn the player's dragon entity.
        for (cn.nukkit.level.Level level : getServer().getLevels().values()) {
            for (cn.nukkit.entity.Entity entity : level.getEntities()) {
                if (entity instanceof DragonEntity) {
                    DragonEntity dragon = (DragonEntity) entity;
                    if (dragon.getOwner() != null && dragon.getOwner().equals(player)) {
                        // Unmount all passengers before despawning
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
        if (dragonEgg == null || !dragonEgg.hasCompoundTag()) {
            return;
        }
        String eggId = dragonEgg.getNamedTag().getString("eggId");
        if (eggId == null || eggId.isEmpty()) {
            return;
        }
        eggManager.updateEggLore(dragonEgg, eggId);
    }

    private void onDragonHatch(Player player, String dragonType) {
        // Give initial shards
        shardManager.giveInitialShards(dragonType, player);
        player.sendMessage(TextFormat.GREEN + "Congratulations! Your dragon egg has hatched into a " + 
            getColorForDragonType(dragonType) + dragonType + TextFormat.GREEN + "!");
        player.sendMessage(TextFormat.YELLOW + "You received 10 matching dragon shards!");
        player.sendMessage(TextFormat.GRAY + "Use " + TextFormat.WHITE + "/dragon" + TextFormat.GRAY + 
            " to summon your dragon, or break the egg on the ground.");
    }

    private String getColorForDragonType(String dragonType) {
        switch (dragonType) {
            case "Fire Dragon":
                return TextFormat.RED.toString();
            case "Ice Dragon":
                return TextFormat.AQUA.toString();
            case "Lightning Dragon":
                return TextFormat.YELLOW.toString();
            default:
                return TextFormat.WHITE.toString();
        }
    }

    public DragonShardManager getShardManager() {
        return shardManager;
    }

    public Config getLanguageConfig() {
        return langConfig;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(String language) {
        File langFile = new File(getDataFolder(), "lang/" + language + ".yml");
        if (langFile.exists()) {
            langConfig = new Config(langFile, Config.YAML);
            currentLanguage = language;
            getLogger().info("Language changed to: " + language);
        } else {
            getLogger().warning("Language file not found: " + language);
        }
    }

    public void onDragonDeath(Player owner) {
        String playerUUID = owner.getUniqueId().toString();
        dragonDeathCooldowns.put(playerUUID, System.currentTimeMillis());
        int respawnTime = getConfig().getInt("dragon-respawn-time", 900);
        owner.sendMessage("§cYour dragon has been defeated! You must wait " + (respawnTime / 60) + " minutes before summoning it again.");
    }

    public void removeDragonDeathCooldown(String playerUUID) {
        dragonDeathCooldowns.remove(playerUUID);
    }

    public long getDragonRespawnCooldown() {
        return DRAGON_RESPAWN_COOLDOWN;
    }

    public int getDragonHatchingTime() {
        return dragonHatchingTime;
    }
}
