package com.youssgm3o8.rokidragon.data.models;

// ORMLite imports removed

// ORMLite annotation removed
public class StoredEgg {

    // ORMLite annotation removed
    private int id;

    // ORMLite annotation removed
    private String playerUUID;

    // ORMLite annotation removed
    private int slotNumber;

    // ORMLite annotation removed
    private String eggId;
    
    // ORMLite annotation removed
    private String nbtData;

    // Empty constructor required by ORMLite
    public StoredEgg() {
    }

    public StoredEgg(String playerUUID, int slotNumber, String eggId, String nbtData) {
        this.playerUUID = playerUUID;
        this.slotNumber = slotNumber;
        this.eggId = eggId;
        this.nbtData = nbtData;
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

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getEggId() {
        return eggId;
    }

    public void setEggId(String eggId) {
        this.eggId = eggId;
    }

    public String getNbtData() {
        return nbtData;
    }

    public void setNbtData(String nbtData) {
        this.nbtData = nbtData;
    }
} 