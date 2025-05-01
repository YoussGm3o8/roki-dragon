package com.youssgm3o8.rokidragon.language;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.youssgm3o8.rokidragon.DragonPlugin;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final DragonPlugin plugin;
    private Config languageConfig;
    private final Map<String, String> cachedStrings = new HashMap<>();
    private boolean debugMode = false;

    public LanguageManager(DragonPlugin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    /**
     * Loads language files based on config settings
     */
    private void loadLanguages() {
        // Load default language files if they don't exist
        plugin.saveResource("lang/en_US.yml", false);
        plugin.saveResource("lang/es_ES.yml", false);
        
        // Get default language from config
        String defaultLang = plugin.getConfig().getString("language.default", "en_US");
        File langFile = new File(plugin.getDataFolder() + "/lang", defaultLang + ".yml");
        
        // Check if the language file exists, use fallback if not
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + defaultLang + ".yml not found, using fallback language");
            String fallbackLang = plugin.getConfig().getString("language.fallback", "en_US");
            langFile = new File(plugin.getDataFolder() + "/lang", fallbackLang + ".yml");
        }
        
        this.languageConfig = new Config(langFile, Config.YAML);
        plugin.getLogger().info("Loaded language: " + langFile.getName());
    }

    /**
     * Gets a language string with formatted parameters
     * 
     * @param key The language key
     * @param params Parameters to format into the string
     * @return The formatted language string
     */
    public String get(String key, Object... params) {
        if (debugMode) {
            plugin.getLogger().info("Getting lang string for key: '" + key + "'");
        }

        // Try to get from cache first
        String value = cachedStrings.get(key);
        
        // If not in cache, get from config and cache it
        if (value == null) {
            value = languageConfig.getString(key, key);
            cachedStrings.put(key, value);
        }

        if (debugMode) {
            plugin.getLogger().info("Language result for '" + key + "': '" + value + "'");
        }
        
        // Format the string with the provided parameters
        if (params.length > 0) {
            try {
                value = MessageFormat.format(value, params);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Error formatting language string for key: " + key);
                plugin.getLogger().warning("Error: " + e.getMessage());
                // Fall back to simple replacement if MessageFormat fails
                for (int i = 0; i < params.length; i++) {
                    value = value.replace("{" + i + "}", String.valueOf(params[i]));
                }
            }
        }
        
        return TextFormat.colorize('&', value);
    }

    /**
     * Gets a raw language string without formatting
     * 
     * @param key The language key
     * @return The raw language string
     */
    public String getRaw(String key) {
        return languageConfig.getString(key, key);
    }

    /**
     * Gets a raw object from the language file
     * 
     * @param key The language key
     * @return The raw object (can be String, List, etc.)
     */
    public Object getRawObject(String key) {
        return languageConfig.get(key);
    }

    /**
     * Reloads the language configuration
     */
    public void reload() {
        loadLanguages();
        cachedStrings.clear();
    }

    /**
     * Sets debug mode for language string retrieval
     * 
     * @param debug Whether to enable debug mode
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * Checks if a language key exists
     * 
     * @param key The key to check
     * @return true if the key exists, false otherwise
     */
    public boolean hasKey(String key) {
        return languageConfig.exists(key);
    }
} 