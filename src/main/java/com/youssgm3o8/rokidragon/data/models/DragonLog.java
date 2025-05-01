package com.youssgm3o8.rokidragon.data.models;

// ORMLite imports removed

import java.util.Date;

// ORMLite annotation removed
public class DragonLog {

    // ORMLite annotation removed
    private int id;

    // ORMLite annotation removed
    private String eggId;

    // ORMLite annotation removed
    private String playerUUID;

    // ORMLite annotation removed
    private String lostLocation;

    // ORMLite annotation removed
    private Date logDate;

    // Empty constructor required by ORMLite
    public DragonLog() {
    }

    public DragonLog(String eggId, String playerUUID, String lostLocation) {
        this.eggId = eggId;
        this.playerUUID = playerUUID;
        this.lostLocation = lostLocation;
        this.logDate = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getLostLocation() {
        return lostLocation;
    }

    public void setLostLocation(String lostLocation) {
        this.lostLocation = lostLocation;
    }

    public Date getLogDate() {
        return logDate;
    }

    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }
} 