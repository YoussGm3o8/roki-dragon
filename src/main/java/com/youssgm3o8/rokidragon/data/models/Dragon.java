package com.youssgm3o8.rokidragon.data.models;

// ORMLite imports removed

// ORMLite annotation removed
public class Dragon {

    // ORMLite annotation removed
    private int id;

    // ORMLite annotation removed
    private String playerUUID;

    // ORMLite annotation removed (Note: Foreign key relationship is now handled by CREATE TABLE statement)
    private DragonEgg dragonEgg;

    // Empty constructor required by ORMLite
    public Dragon() {
    }

    public Dragon(String playerUUID, DragonEgg dragonEgg) {
        this.playerUUID = playerUUID;
        this.dragonEgg = dragonEgg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public DragonEgg getDragonEgg() {
        return dragonEgg;
    }

    public void setDragonEgg(DragonEgg dragonEgg) {
        this.dragonEgg = dragonEgg;
    }
} 