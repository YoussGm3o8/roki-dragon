package com.youssgm3o8.rokidragon.data.models;

// ORMLite imports removed

import java.util.Date;

// ORMLite annotation removed
public class DragonEgg {

    // ORMLite annotation removed
    private String eggId;

    // ORMLite annotation removed
    private String playerUUID;

    // ORMLite annotation removed
    private String dragonType;

    // ORMLite annotation removed
    private String dragonName;

    // ORMLite annotation removed
    private boolean hatched;

    // ORMLite annotation removed
    private boolean incubating;

    // ORMLite annotation removed
    private int incubationProgress;

    // ORMLite annotation removed
    private Date purchaseDate;

    // Empty constructor required by ORMLite
    public DragonEgg() {
    }

    public DragonEgg(String eggId, String playerUUID, String dragonType, String dragonName) {
        this.eggId = eggId;
        this.playerUUID = playerUUID;
        this.dragonType = dragonType;
        this.dragonName = dragonName;
        this.hatched = false;
        this.incubating = false;
        this.incubationProgress = 0;
        this.purchaseDate = new Date();
    }

    public String getEggId() {
        return eggId;
    }

    public void setEggId(String eggId) {
        this.eggId = eggId;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getDragonType() {
        return dragonType;
    }

    public void setDragonType(String dragonType) {
        this.dragonType = dragonType;
    }

    public String getDragonName() {
        return dragonName;
    }

    public void setDragonName(String dragonName) {
        this.dragonName = dragonName;
    }

    public boolean isHatched() {
        return hatched;
    }

    public void setHatched(boolean hatched) {
        this.hatched = hatched;
    }

    public boolean isIncubating() {
        return incubating;
    }

    public void setIncubating(boolean incubating) {
        this.incubating = incubating;
    }

    public int getIncubationProgress() {
        return incubationProgress;
    }

    public void setIncubationProgress(int incubationProgress) {
        this.incubationProgress = incubationProgress;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
} 