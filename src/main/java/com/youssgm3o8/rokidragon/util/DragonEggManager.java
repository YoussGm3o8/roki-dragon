package com.youssgm3o8.rokidragon.util;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;

import java.util.*;

public class DragonEggManager {
    private final Map<String, IncubationStats> eggStats = new HashMap<>();
    private final DragonPlugin plugin;

    public DragonEggManager(DragonPlugin plugin) {
        this.plugin = plugin;
    }

    public String purchaseEgg(String playerUUID) {
        String eggId = UUID.randomUUID().toString();
        eggStats.put(eggId, new IncubationStats());
        return eggId;
    }

    public void updateIncubationConditions(Player player, String eggId) {
        if (!eggStats.containsKey(eggId)) return;
        
        IncubationStats stats = eggStats.get(eggId);
        Level level = player.getLevel();
        Position pos = player.getPosition();
        
        // Check for heat sources
        if (isNearHeatSource(level, pos) || level.getName().toLowerCase().contains("nether")) {
            stats.incrementHeatTime();
        }
        
        // Check for cold conditions
        if (isInColdConditions(level, pos)) {
            stats.incrementColdTime();
        }
        
        // Check for darkness/night conditions
        if (isInDarkness(level, pos)) {
            stats.incrementDarkTime();
        }
    }

    private boolean isNearHeatSource(Level level, Position pos) {
        // Check nearby blocks for heat sources
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = level.getBlock(pos.add(x, y, z));
                    int blockId = block.getId();
                    if (blockId == Block.FURNACE || 
                        blockId == Block.BURNING_FURNACE ||
                        blockId == Block.LAVA ||
                        blockId == Block.STILL_LAVA ||
                        blockId == Block.TORCH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isInColdConditions(Level level, Position pos) {
        // Check if in water
        Block block = level.getBlock(pos);
        if (block.getId() == Block.WATER || block.getId() == Block.STILL_WATER) {
            return true;
        }
        
        // Check if near ice
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block nearbyBlock = level.getBlock(pos.add(x, y, z));
                    int blockId = nearbyBlock.getId();
                    if (blockId == Block.ICE || blockId == Block.PACKED_ICE || blockId == Block.SNOW_BLOCK) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private boolean isInDarkness(Level level, Position pos) {
        // Check if it's night time
        if (level.getTime() >= 13000 && level.getTime() <= 23000) {
            return true;
        }
        
        // Check light level
        int blockLight = level.getBlockLightAt((int)pos.x, (int)pos.y, (int)pos.z);
        return blockLight <= 7;
    }

    public String determineDragonType(String eggId) {
        if (!eggStats.containsKey(eggId)) return "Fire Dragon"; // Default type
        
        IncubationStats stats = eggStats.get(eggId);
        
        // Require a minimum threshold of conditions to be met
        int totalTime = stats.heatTime + stats.coldTime + stats.darkTime;
        if (totalTime == 0) return "Fire Dragon"; // Default if no conditions met
        
        // Calculate percentages
        double heatPercent = (double) stats.heatTime / totalTime;
        double coldPercent = (double) stats.coldTime / totalTime;
        double darkPercent = (double) stats.darkTime / totalTime;
        
        // Require at least 40% of total time in specific condition
        if (heatPercent >= 0.4) {
            return "Fire Dragon";
        } else if (coldPercent >= 0.4) {
            return "Ice Dragon";
        } else if (darkPercent >= 0.4) {
            return "Lightning Dragon";
        }
        
        // If no condition meets threshold, determine based on highest percentage
        if (stats.heatTime > stats.coldTime && stats.heatTime > stats.darkTime) {
            return "Fire Dragon";
        } else if (stats.coldTime > stats.heatTime && stats.coldTime > stats.darkTime) {
            return "Ice Dragon";
        } else if (stats.darkTime > stats.heatTime && stats.darkTime > stats.coldTime) {
            return "Lightning Dragon";
        }
        
        return "Fire Dragon"; // Default if no clear winner
    }

    public void removeEggStats(String eggId) {
        eggStats.remove(eggId);
    }

    public void updateEggLore(Item dragonEgg, String eggId) {
        List<String> lore = new ArrayList<>();
        lore.add(TextFormat.GRAY.toString() + "Egg ID: " + TextFormat.WHITE.toString() + eggId);

        if (plugin.getDatabaseManager().isEggHatched(eggId)) {
            lore.add(TextFormat.GREEN.toString() + "Egg is hatched! Right-click to summon or despawn your dragon.");
        } else {
            boolean isIncubating = plugin.getDatabaseManager().isEggIncubating(eggId);
            if (isIncubating) {
                lore.add(TextFormat.YELLOW.toString() + "Status: Incubating");
                lore.add(TextFormat.GRAY.toString() + "Shift-right-click to stop incubation");
            } else {
                lore.add(TextFormat.RED.toString() + "Status: Not Incubating");
                lore.add(TextFormat.GRAY.toString() + "Shift-right-click to start incubation");
            }

            // Add incubation instructions
            lore.add("");
            lore.add(TextFormat.WHITE.toString() + "Incubation Requirements:");
            lore.add(TextFormat.GRAY.toString() + "- Keep the egg in your inventory");
            lore.add(TextFormat.GRAY.toString() + "- Stay in the right environment");
            lore.add(TextFormat.GRAY.toString() + "- Remain online while incubating");
        }

        dragonEgg.setLore(lore.toArray(new String[0]));
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

    private static class IncubationStats {
        int heatTime = 0;
        int coldTime = 0;
        int darkTime = 0;

        void incrementHeatTime() { heatTime++; }
        void incrementColdTime() { coldTime++; }
        void incrementDarkTime() { darkTime++; }
    }
}

