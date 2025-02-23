package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.particle.GenericParticle;
import cn.nukkit.block.Block;
import cn.nukkit.entity.projectile.EntitySnowball;
import cn.nukkit.potion.Effect;
import cn.nukkit.math.Vector3;
import java.util.concurrent.ThreadLocalRandom;

public class EntityIceBall extends EntitySnowball {
    private Entity shootingEntity;

    public EntityIceBall(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }
    
    public EntityIceBall(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt);
        this.shootingEntity = shootingEntity;
    }

    @Override
    public void onHit() {
        if (!this.closed) {
            this.close();
            
            // Create explosion effect
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 1.2);
            this.server.getPluginManager().callEvent(ev);
            
            if (!ev.isCancelled()) {
                // Create snow layers in a radius
                int radius = 3;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + z * z <= radius * radius) {
                            Vector3 pos = new Vector3(
                                this.x + x,
                                this.y,
                                this.z + z
                            );
                            
                            // Only place snow on solid blocks
                            Block block = this.level.getBlock(pos.subtract(0, 1, 0));
                            if (block.isSolid()) {
                                // Random chance to place snow layer
                                if (ThreadLocalRandom.current().nextDouble() < 0.7) {
                                    this.level.setBlock(pos, Block.get(Block.SNOW_LAYER));
                                }
                            }
                        }
                    }
                }
                
                // Apply slowness effect to nearby entities
                for (Entity entity : this.level.getNearbyEntities(this.getBoundingBox().grow(radius, radius, radius))) {
                    if (entity != this.shootingEntity) {
                        Effect slowness = Effect.getEffect(Effect.SLOWNESS)
                            .setDuration(100)  // 5 seconds
                            .setAmplifier(1);  // Slowness II
                        entity.addEffect(slowness);
                    }
                }
                
                // Add particle effects
                for (int i = 0; i < 20; i++) {
                    this.level.addParticle(new GenericParticle(this, GenericParticle.TYPE_SNOWBALL_POOF));
                }
            }
        }
    }
} 