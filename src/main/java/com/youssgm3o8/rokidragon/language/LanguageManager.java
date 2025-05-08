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
    private final Map<String, String> cachedRawStrings = new HashMap<>();
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

        // Get the raw string from cache or config
        String rawValue = getRawStringFromCache(key);
        
        // Format the string with the provided parameters
        String formattedValue = rawValue;
        if (params.length > 0) {
            try {
                // Use our own simple replacement instead of MessageFormat to avoid formatting issues
                for (int i = 0; i < params.length; i++) {
                    formattedValue = formattedValue.replace("{" + i + "}", String.valueOf(params[i]));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error formatting language string for key: " + key);
                plugin.getLogger().warning("Error: " + e.getMessage());
            }
        }
        
        return TextFormat.colorize('&', formattedValue);
    }

    /**
     * Gets a raw string from cache or config
     * 
     * @param key The language key
     * @return The raw string
     */
    private String getRawStringFromCache(String key) {
        // Try to get from cache first
        String rawValue = cachedRawStrings.get(key);
        
        // If not in cache, get from config and cache it
        if (rawValue == null) {
            if (debugMode) {
                plugin.getLogger().info("[LangManager Debug] Key '" + key + "' not found in cache. Querying languageConfig.");
            }
            
            rawValue = languageConfig.getString(key, key);
            cachedRawStrings.put(key, rawValue);
            
            if (debugMode) {
                plugin.getLogger().info("[LangManager Debug] Value from languageConfig.getString for key '" + key + "': '" + rawValue + "'. Caching it.");
            }
        }
        
        return rawValue;
    }

    /**
     * Gets a raw language string without formatting
     * 
     * @param key The language key
     * @return The raw language string
     */
    public String getRaw(String key) {
        return getRawStringFromCache(key);
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
        cachedRawStrings.clear();
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