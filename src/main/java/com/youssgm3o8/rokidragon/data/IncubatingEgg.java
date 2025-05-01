package com.youssgm3o8.rokidragon.data;

/**
 * Represents an egg that is currently being incubated
 */
public class IncubatingEgg {
    private final String eggId;
    private final String playerUUID;
    private final String dragonType;
    private final String dragonName;
    private final long incubationStartTime;
    private int incubationSeconds;
    
    /**
     * Creates a new IncubatingEgg instance
     * 
     * @param eggId The unique ID of the egg
     * @param playerUUID The UUID of the player incubating the egg
     * @param dragonType The type of dragon that will hatch
     * @param dragonName The name of the dragon that will hatch
     * @param incubationStartTime The timestamp when incubation started
     * @param incubationSeconds The total seconds the egg has been incubated
     */
    public IncubatingEgg(String eggId, String playerUUID, String dragonType, String dragonName, 
                         long incubationStartTime, int incubationSeconds) {
        this.eggId = eggId;
        this.playerUUID = playerUUID;
        this.dragonType = dragonType;
        this.dragonName = dragonName;
        this.incubationStartTime = incubationStartTime;
        this.incubationSeconds = incubationSeconds;
    }
    
    /**
     * Gets the unique ID of the egg
     * 
     * @return The egg ID
     */
    public String getEggId() {
        return eggId;
    }
    
    /**
     * Gets the UUID of the player incubating the egg
     * 
     * @return The player UUID
     */
    public String getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Gets the type of dragon that will hatch
     * 
     * @return The dragon type
     */
    public String getDragonType() {
        return dragonType;
    }
    
    /**
     * Gets the name of the dragon that will hatch
     * 
     * @return The dragon name
     */
    public String getDragonName() {
        return dragonName;
    }
    
    /**
     * Gets the timestamp when incubation started
     * 
     * @return The incubation start time
     */
    public long getIncubationStartTime() {
        return incubationStartTime;
    }
    
    /**
     * Gets the total seconds the egg has been incubated
     * 
     * @return The incubation seconds
     */
    public int getIncubationSeconds() {
        return incubationSeconds;
    }
    
    /**
     * Sets the total seconds the egg has been incubated
     * 
     * @param incubationSeconds The new incubation seconds value
     */
    public void setIncubationSeconds(int incubationSeconds) {
        this.incubationSeconds = incubationSeconds;
    }
    
    /**
     * Calculates the current incubation progress as a percentage
     * 
     * @param requiredSeconds The total seconds required for full incubation
     * @return The progress percentage (0-100)
     */
    public int getProgressPercentage(int requiredSeconds) {
        if (requiredSeconds <= 0) {
            return 100;
        }
        
        int progress = (int) (((double) incubationSeconds / requiredSeconds) * 100);
        return Math.min(100, Math.max(0, progress)); // Ensure between 0-100
    }
    
    /**
     * Checks if the egg is ready to hatch
     * 
     * @param requiredSeconds The total seconds required for full incubation
     * @return true if the egg is ready to hatch, false otherwise
     */
    public boolean isReadyToHatch(int requiredSeconds) {
        return incubationSeconds >= requiredSeconds;
    }
    
    /**
     * Updates the incubation progress
     * 
     * @param additionalSeconds The additional seconds to add to the incubation
     */
    public void addIncubationTime(int additionalSeconds) {
        this.incubationSeconds += additionalSeconds;
    }
} 