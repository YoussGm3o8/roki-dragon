package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import nukkitcoders.mobplugin.utils.FireBallExplosion;

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
        // New explosion implementation using Explosion class
        Position pos = sourceEntity.getPosition().add(0.5, 0, 0.5);
        Explosion explosion = new Explosion(pos, 3, sourceEntity);
        explosion.setFireSpawnChance(0.3333f);
        explosion.explodeA();
        explosion.explodeB();
        return true;
    }
}
