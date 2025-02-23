package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import nukkitcoders.mobplugin.utils.FireBallExplosion;
import com.youssgm3o8.rokidragon.DragonPlugin;

public class FireBedExplosion extends FireBallExplosion {

    // Added field to store the explosion source entity
    private Entity sourceEntity;
    
    public FireBedExplosion(Entity entity, double force, Entity shooter) {
        super(entity, force, shooter);
        // Initialize the explosion source
        this.sourceEntity = entity;
    }
    
    @Override
    public boolean explodeB() {
        // Get explosion settings from config
        double explosionRadius = DragonPlugin.getInstance().getConfig().getDouble("fireball.explosion-radius", 3.0);
        boolean setFire = DragonPlugin.getInstance().getConfig().getBoolean("fireball.set-fire", true);
        float fireChance = (float) DragonPlugin.getInstance().getConfig().getDouble("fireball.fire-chance", 0.3333);
        
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
