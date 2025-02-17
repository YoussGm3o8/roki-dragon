package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.entity.Entity;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.GameRule;
import com.youssgm3o8.rokidragon.entities.FireBedExplosion;

public class EntityBedFireBall extends EntityGhastFireBall {

    public EntityBedFireBall(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }
    
    public EntityBedFireBall(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
    }
    
    @Override
    public void explode() {
        if (!this.closed) {
            this.close();
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 1.2);
            this.server.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                FireBedExplosion explosion = new FireBedExplosion(this, ev.getForce(), this.shootingEntity);
                if (this.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
                    explosion.explodeB();
                }
                explosion.explodeB();
            }
        }
    }
}
