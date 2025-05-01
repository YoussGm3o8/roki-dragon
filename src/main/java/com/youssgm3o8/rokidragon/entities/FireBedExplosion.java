package com.youssgm3o8.rokidragon.entities;

import com.youssgm3o8.rokidragon.DragonPlugin;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.utils.FireBallExplosion;

public class FireBedExplosion extends FireBallExplosion {

    private final DragonPlugin plugin; // Added plugin instance
    private final Entity sourceEntity; // Keep source entity
    
    public FireBedExplosion(DragonPlugin plugin, Entity entity, double force, Entity shooter) { // Added plugin parameter
        super(entity, force, shooter); // Keep super call
        this.plugin = plugin; // Assign plugin instance
        this.sourceEntity = entity; // Keep source entity assignment
    }
    
    @Override
    public boolean explodeB() {
        // Get explosion settings from config
        double explosionRadius = plugin.getConfig().getDouble("abilities.fireball.explosion_radius", 3.0); // Updated key
        boolean setFire = plugin.getConfig().getBoolean("abilities.fireball.set_fire", true); // Updated key
        float fireChance = (float) plugin.getConfig().getDouble("abilities.fireball.fire_chance", 0.3333); // Updated key
        
        // New explosion implementation using Explosion class
        Position pos = sourceEntity.getPosition().add(0.5, 0, 0.5);
        Explosion explosion = new Explosion(pos, explosionRadius, sourceEntity);
        
        if (setFire) {
            explosion.setFireSpawnChance(fireChance);
        } else {
            explosion.setFireSpawnChance(0);
        }
        
        explosion.explodeA();
        explosion.explodeB();
        return true;
    }
}
