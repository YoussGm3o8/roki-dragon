package com.youssgm3o8.rokidragon.util;

import com.youssgm3o8.rokidragon.DragonPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import cn.nukkit.Server;
import java.util.UUID;

/**
 * Utility class for managing and generating the plugin's resource pack
 */
public class ResourcePackManager {
    private final DragonPlugin plugin;
    private static final String RESOURCE_PACK_NAME = "roki_dragon_pack.zip";
    
    /**
     * Creates a new ResourcePackManager
     * 
     * @param plugin The main plugin instance
     */
    public ResourcePackManager(DragonPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Generates and exports the plugin's resource pack into a zip file in Nukkit's resource_packs folder
     * 
     * @return true if successful, false otherwise
     */
    public boolean exportResourcePack() {
        try {
            plugin.getLogger().info("Generating RokiDragon resource pack...");
            
            // Get the server's resource_packs directory
            String serverDirPath = plugin.getServer().getDataPath();
            File serverDir = new File(serverDirPath);
            File resourcePacksDir = new File(serverDir, "resource_packs");
            
            // Create the directory if it doesn't exist
            if (!resourcePacksDir.exists()) {
                resourcePacksDir.mkdirs();
                plugin.getLogger().info("Created resource_packs directory");
            }
            
            // Create the output zip file
            File outputZip = new File(resourcePacksDir, RESOURCE_PACK_NAME);
            
            // Create a temporary directory to build the resource pack structure
            File tempDir = new File(plugin.getDataFolder(), "temp_resource_pack");
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
            }
            tempDir.mkdirs();
            
            // Generate manifest.json file (required by Nukkit)
            File manifestJson = new File(tempDir, "manifest.json");
            generateManifestJson(manifestJson);
            
            // Create directories for textures, models, etc.
            File texturesDir = new File(tempDir, "textures/entity");
            texturesDir.mkdirs();
            
            File modelsDir = new File(tempDir, "models/entity");
            modelsDir.mkdirs();
            
            // Create render_controllers directory
            File renderControllersDir = new File(tempDir, "render_controllers");
            renderControllersDir.mkdirs();
            
            // Generate render controller
            generateRenderController(new File(renderControllersDir, "dragon.render_controllers.json"));
            
            // Generate a single entity definition for ender_dragon that will be used for all dragon types
            File entityDir = new File(tempDir, "entity");
            entityDir.mkdirs();
            generateEntityDefinition(new File(entityDir, "ender_dragon.json"), "fire_dragon"); // Use fire_dragon as base
            
            // Generate texture files for each dragon type
            String[] dragonTypes = {"fire_dragon", "ice_dragon", "lightning_dragon", "water_dragon", "earth_dragon"};
            
            for (String dragonType : dragonTypes) {
                // Generate texture files
                generateTexture(new File(texturesDir, dragonType + ".png"), dragonType);
                
                // Generate model files
                generateModel(new File(modelsDir, dragonType + ".json"), dragonType);
            }
            
            // Create a pack_manifest.json file (for our own reference)
            generatePackManifest(new File(tempDir, "pack_manifest.json"));
            
            // Create the zip file
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
                zipDirectory(tempDir, tempDir.getPath(), zos);
                plugin.getLogger().info("Successfully created resource pack: " + outputZip.getAbsolutePath());
            }
            
            // Clean up temporary directory
            deleteDirectory(tempDir);
            
            // Register the resource pack with the server
            plugin.getServer().getResourcePackManager().reloadPacks();
            plugin.getLogger().info("Successfully registered resource pack with the server");
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().error("Failed to generate resource pack: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generates the manifest.json file (required by Nukkit)
     * 
     * @param file The file to write to
     * @throws IOException If an I/O error occurs
     */
    private void generateManifestJson(File file) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String json = "{\n" +
                      "  \"format_version\": 2,\n" +
                      "  \"header\": {\n" +
                      "    \"description\": \"RokiDragon Resource Pack\",\n" +
                      "    \"name\": \"RokiDragon\",\n" +
                      "    \"uuid\": \"" + uuid + "\",\n" +
                      "    \"version\": [1, 0, 0],\n" +
                      "    \"min_engine_version\": [1, 16, 0]\n" +
                      "  },\n" +
                      "  \"modules\": [\n" +
                      "    {\n" +
                      "      \"description\": \"RokiDragon Resources\",\n" +
                      "      \"type\": \"resources\",\n" +
                      "      \"uuid\": \"" + UUID.randomUUID().toString() + "\",\n" +
                      "      \"version\": [1, 0, 0]\n" +
                      "    }\n" +
                      "  ]\n" +
                      "}";
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        }
        
        plugin.getLogger().info("Generated manifest.json");
    }
    
    /**
     * Generates a texture file for a dragon
     * 
     * @param file The file to write to
     * @param dragonType The type of dragon
     * @throws IOException If an I/O error occurs
     */
    private void generateTexture(File file, String dragonType) throws IOException {
        // Create a basic colored texture based on dragon type
        // This is just a placeholder - in a real implementation, you'd have proper textures
        int width = 64;
        int height = 32;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Fill with color based on dragon type
        java.awt.Graphics2D g = image.createGraphics();
        switch (dragonType) {
            case "fire_dragon":
                g.setColor(new java.awt.Color(255, 100, 0, 255)); // Orange-red
                break;
            case "ice_dragon":
                g.setColor(new java.awt.Color(100, 200, 255, 255)); // Light blue
                break;
            case "lightning_dragon":
                g.setColor(new java.awt.Color(255, 255, 0, 255)); // Yellow
                break;
            case "water_dragon":
                g.setColor(new java.awt.Color(0, 100, 255, 255)); // Blue
                break;
            case "earth_dragon":
                g.setColor(new java.awt.Color(0, 150, 0, 255)); // Green
                break;
            default:
                g.setColor(new java.awt.Color(200, 200, 200, 255)); // Gray default
        }
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        // Save the image
        javax.imageio.ImageIO.write(image, "png", file);
        plugin.getLogger().info("Generated texture for " + dragonType);
    }
    
    /**
     * Generates a model JSON file for a dragon
     * 
     * @param file The file to write to
     * @param dragonType The type of dragon
     * @throws IOException If an I/O error occurs
     */
    private void generateModel(File file, String dragonType) throws IOException {
        String modelJson = "{\n" +
                           "  \"format_version\": \"1.12.0\",\n" +
                           "  \"minecraft:geometry\": [\n" +
                           "    {\n" +
                           "      \"description\": {\n" +
                           "        \"identifier\": \"geometry.roki:" + dragonType + "\",\n" +
                           "        \"texture_width\": 64,\n" +
                           "        \"texture_height\": 32,\n" +
                           "        \"visible_bounds_width\": 8,\n" +
                           "        \"visible_bounds_height\": 4,\n" +
                           "        \"visible_bounds_offset\": [0, 1, 0]\n" +
                           "      },\n" +
                           "      \"bones\": [\n" +
                           "        {\n" +
                           "          \"name\": \"body\",\n" +
                           "          \"pivot\": [0, 24, 0],\n" +
                           "          \"cubes\": [\n" +
                           "            {\"origin\": [-6, 20, -6], \"size\": [12, 8, 12], \"uv\": [0, 0]}\n" +
                           "          ]\n" +
                           "        },\n" +
                           "        {\n" +
                           "          \"name\": \"head\",\n" +
                           "          \"pivot\": [0, 24, -6],\n" +
                           "          \"parent\": \"body\",\n" +
                           "          \"cubes\": [\n" +
                           "            {\"origin\": [-4, 22, -14], \"size\": [8, 6, 8], \"uv\": [0, 20]}\n" +
                           "          ]\n" +
                           "        },\n" +
                           "        {\n" +
                           "          \"name\": \"wing_left\",\n" +
                           "          \"pivot\": [6, 24, 0],\n" +
                           "          \"parent\": \"body\",\n" +
                           "          \"cubes\": [\n" +
                           "            {\"origin\": [6, 20, -4], \"size\": [8, 4, 8], \"uv\": [36, 0]}\n" +
                           "          ]\n" +
                           "        },\n" +
                           "        {\n" +
                           "          \"name\": \"wing_right\",\n" +
                           "          \"pivot\": [-6, 24, 0],\n" +
                           "          \"parent\": \"body\",\n" +
                           "          \"cubes\": [\n" +
                           "            {\"origin\": [-14, 20, -4], \"size\": [8, 4, 8], \"uv\": [36, 12]}\n" +
                           "          ]\n" +
                           "        },\n" +
                           "        {\n" +
                           "          \"name\": \"tail\",\n" +
                           "          \"pivot\": [0, 24, 6],\n" +
                           "          \"parent\": \"body\",\n" +
                           "          \"cubes\": [\n" +
                           "            {\"origin\": [-2, 22, 6], \"size\": [4, 4, 8], \"uv\": [24, 20]}\n" +
                           "          ]\n" +
                           "        }\n" +
                           "      ]\n" +
                           "    }\n" +
                           "  ]\n" +
                           "}";
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(modelJson);
        }
        
        plugin.getLogger().info("Generated model for " + dragonType);
    }
    
    /**
     * Generates an entity definition JSON file for a dragon
     * 
     * @param file The file to write to
     * @param dragonType The type of dragon
     * @throws IOException If an I/O error occurs
     */
    private void generateEntityDefinition(File file, String dragonType) throws IOException {
        // Make sure parent directories exist
        file.getParentFile().mkdirs();
        
        // Much simpler entity definition - just override the texture
        // This approach is more likely to work with Nukkit's resource pack system
        String entityJson = "{\n" +
                           "  \"format_version\": \"1.10.0\",\n" +
                           "  \"minecraft:client_entity\": {\n" +
                           "    \"description\": {\n" +
                           "      \"identifier\": \"minecraft:ender_dragon\",\n" +
                           "      \"materials\": { \"default\": \"entity_alphatest\" },\n" +
                           "      \"textures\": { \"default\": \"textures/entity/" + dragonType + "\" }\n" +
                           "    }\n" +
                           "  }\n" +
                           "}";
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(entityJson);
        }
        
        plugin.getLogger().info("Generated entity definition for ender_dragon using " + dragonType + " texture");
    }
    
    /**
     * Generates a pack manifest file
     * 
     * @param file The file to write to
     * @throws IOException If an I/O error occurs
     */
    private void generatePackManifest(File file) throws IOException {
        String manifestJson = "{\n" +
                             "  \"name\": \"RokiDragon Resource Pack\",\n" +
                             "  \"description\": \"Resource pack for RokiDragon plugin\",\n" +
                             "  \"version\": \"1.0.0\",\n" +
                             "  \"dragons\": [\"fire_dragon\", \"ice_dragon\", \"lightning_dragon\", \"water_dragon\", \"earth_dragon\"]\n" +
                             "}";
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(manifestJson);
        }
        
        plugin.getLogger().info("Generated pack manifest");
    }
    
    /**
     * Get color hex code for a dragon type
     * 
     * @param dragonType The type of dragon
     * @return Hex color code
     */
    private String getColorForDragonType(String dragonType) {
        switch (dragonType) {
            case "fire_dragon":
                return "#FF5500";
            case "ice_dragon":
                return "#88CCFF";
            case "lightning_dragon":
                return "#FFFF00";
            case "water_dragon":
                return "#0066FF";
            case "earth_dragon":
                return "#009900";
            default:
                return "#CCCCCC";
        }
    }
    
    /**
     * Recursively zip a directory
     * 
     * @param folder The folder to zip
     * @param parentFolder The parent folder path for relative paths
     * @param zos The ZipOutputStream to write to
     * @throws IOException If an I/O error occurs
     */
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder, zos);
                continue;
            }
            
            String entryPath = file.getPath().substring(parentFolder.length() + 1).replace('\\', '/');
            ZipEntry ze = new ZipEntry(entryPath);
            zos.putNextEntry(ze);
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            
            zos.closeEntry();
        }
    }
    
    /**
     * Recursively delete a directory
     * 
     * @param directory The directory to delete
     * @return true if successful, false otherwise
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * Generates the render controller file
     * 
     * @param file The file to write to
     * @throws IOException If an I/O error occurs
     */
    private void generateRenderController(File file) throws IOException {
        // Make sure parent directories exist
        file.getParentFile().mkdirs();
        
        String renderControllerJson = "{\n" +
                                    "  \"format_version\": \"1.10.0\",\n" +
                                    "  \"render_controllers\": {\n" +
                                    "    \"controller.render.dragon\": {\n" +
                                    "      \"geometry\": \"Geometry.default\",\n" +
                                    "      \"materials\": [ { \"*\": \"Material.default\" } ],\n" +
                                    "      \"textures\": [ \"Texture.default\" ]\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}";
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(renderControllerJson);
        }
        
        plugin.getLogger().info("Generated render controller");
    }
} 