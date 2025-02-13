package com.youssgm3o8.rokidragon.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DragonEggManager {

    private final Map<String, String> playerToEggId = new HashMap<>();

    public String purchaseEgg(String playerUUID) {
        String eggId = UUID.randomUUID().toString();
        playerToEggId.put(playerUUID, eggId);
        return eggId;
    }

    public boolean hasPurchasedEgg(String playerUUID) {
        return playerToEggId.containsKey(playerUUID);
    }

    public String getEggId(String playerUUID) {
        return playerToEggId.get(playerUUID);
    }

    public void removeEgg(String playerUUID) {
        playerToEggId.remove(playerUUID);
    }
}
