package com.youssgm3o8.rokidragon.util;

import cn.nukkit.utils.TextFormat;

/**
 * Utility class for common dragon-related functions.
 */
public class DragonUtils {

    /**
     * Gets the appropriate TextFormat color string based on the dragon type.
     *
     * @param type The dragon type string (e.g., "Fire Dragon").
     * @return The corresponding TextFormat color string.
     */
    public static String getColorByType(String type) {
        if (type == null) {
            return TextFormat.WHITE.toString(); // Default color if type is null
        }
        switch (type) {
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

    // Add other utility methods here if needed in the future.
}