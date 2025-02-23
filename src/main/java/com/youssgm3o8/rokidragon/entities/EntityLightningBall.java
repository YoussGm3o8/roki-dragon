package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.particle.DustParticle;
import cn.nukkit.entity.weather.EntityLightning;
import cn.nukkit.math.Vector3;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.level.Explosion;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;

public class EntityLightningBall extends EntityProjectile {
    private Entity shootingEntity;

    public EntityLightningBall(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }
    
    public EntityLightningBall(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt);
        this.shootingEntity = shootingEntity;
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.03f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setScale(1.5f);
    }

    @Override
    public int getNetworkId() {
        return Item.PRISMARINE_CRYSTALS; // Use prismarine crystal model
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        // Add trailing particles
        this.level.addParticle(new DustParticle(this, 255, 255, 0)); // Yellow dust

        return super.onUpdate(currentTick);
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        if (entity != this.shootingEntity) {
            this.handleCollision(entity.getPosition());
        }
    }

    @Override
    protected void onHit() {
        if (!this.closed) {
            this.close();
            
            // Create explosion effect
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 1.2);
            this.server.getPluginManager().callEvent(ev);
            
            if (!ev.isCancelled()) {
                // Create explosion using Explosion class
                Position explodePos = new Position(this.x, this.y, this.z, this.level);
                Explosion explosion = new Explosion(explodePos, ev.getForce(), this);
                explosion.explodeA();
                explosion.explodeB();
                
                // Summon lightning
                EntityLightning lightning = new EntityLightning(
                    this.getChunk(),
                    Entity.getDefaultNBT(this)
                );
                lightning.spawnToAll();
                
                // Add particle effects
                for (int i = 0; i < 30; i++) {
                    this.level.addParticle(new DustParticle(this, 255, 255, 0));
                }
            }
        }
    }

    private void handleCollision(Vector3 position) {
        if (!this.closed) {
            this.close();
            
            // Create explosion effect
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 1.2);
            this.server.getPluginManager().callEvent(ev);
            
            if (!ev.isCancelled()) {
                // Create explosion using Explosion class
                Position explodePos = new Position(position.x, position.y, position.z, this.level);
                Explosion explosion = new Explosion(explodePos, ev.getForce(), this);
                explosion.explodeA();
                explosion.explodeB();
                
                // Summon lightning
                EntityLightning lightning = new EntityLightning(
                    this.getChunk(),
                    Entity.getDefaultNBT(position)
                );
                lightning.spawnToAll();
                
                // Add particle effects
                for (int i = 0; i < 30; i++) {
                    this.level.addParticle(new DustParticle(position, 255, 255, 0));
                }
            }
        }
    }
} 