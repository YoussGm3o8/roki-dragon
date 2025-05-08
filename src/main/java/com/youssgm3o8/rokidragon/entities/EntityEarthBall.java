package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.level.particle.DustParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import com.youssgm3o8.rokidragon.DragonPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class EntityEarthBall extends EntityProjectile {
    private Entity shootingEntity;
    private DragonPlugin plugin;

    public EntityEarthBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.plugin = plugin;
    }
    
    public EntityEarthBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt);
        this.plugin = plugin;
        this.shootingEntity = shootingEntity;
    }

    @Override
    public float getWidth() {
        return 0.3f; // Slightly larger than other projectiles
    }

    @Override
    public float getLength() {
        return 0.3f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    public float getGravity() {
        return 0.04f; // More gravity than other projectiles
    }

    @Override
    public float getDrag() {
        return 0.015f; // More drag
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setScale(1.5f);
    }

    @Override
    public int getNetworkId() {
        return Item.CLAY_BALL; // Use clay ball model
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        // Add trailing particles - earth/dirt particles
        if (ThreadLocalRandom.current().nextDouble() < 0.5) {
            // Brown dust (dirt color)
            this.level.addParticle(new DustParticle(this, 139, 69, 19)); 
        } else {
            // Gray dust (stone color)
            this.level.addParticle(new DustParticle(this, 100, 100, 100));
        }

        return super.onUpdate(currentTick);
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        if (entity != this.shootingEntity) {
            this.onHit();
        }
    }

    @Override
    protected void onHit() {
        if (!this.closed) {
            this.close();
            
            // Get configuration values
            double explosionRadius = plugin.getConfig().getDouble("abilities.earthball.explosion_radius", 2.0);
            int slownessAmplifier = plugin.getConfig().getInt("abilities.earthball.slowness_amplifier", 2);
            int slownessDuration = plugin.getConfig().getInt("abilities.earthball.slowness_duration_ticks", 60);
            int stoneChance = plugin.getConfig().getInt("abilities.earthball.stone_chance", 30); // 30% chance
            boolean createStone = plugin.getConfig().getBoolean("abilities.earthball.create_stone", true);
            
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, explosionRadius);
            this.server.getPluginManager().callEvent(ev);
            
            if (!ev.isCancelled()) {
                // Create earth impact visuals
                for (int i = 0; i < 30; i++) {
                    Vector3 pos = new Vector3(
                        this.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * explosionRadius * 2,
                        this.y + ThreadLocalRandom.current().nextDouble() * explosionRadius,
                        this.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * explosionRadius * 2
                    );
                    
                    // Add dirt and stone particles
                    if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                        this.level.addParticle(new DustParticle(pos, 139, 69, 19)); // Brown (dirt)
                    } else {
                        this.level.addParticle(new DustParticle(pos, 100, 100, 100)); // Gray (stone)
                    }
                    
                    // Add block break particles occasionally
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        this.level.addParticle(new DestroyBlockParticle(pos, Block.get(Block.STONE)));
                    }
                }
                
                // Create a small explosion for effect
                Position explodePos = new Position(this.x, this.y, this.z, this.level);
                Explosion explosion = new Explosion(explodePos, ev.getForce(), this);
                // Only display explosion, don't actually break blocks
                explosion.explodeA();
                
                // Apply effects to nearby entities
                int radius = (int) Math.ceil(explosionRadius);
                for (Entity entity : this.level.getNearbyEntities(this.getBoundingBox().grow(radius, radius, radius))) {
                    if (entity != this.shootingEntity) {
                        // Calculate distance from explosion center to entity
                        double distance = entity.distance(this);
                        if (distance <= explosionRadius) {
                            // Apply slowness effect (as if trapped in earth/mud)
                            Effect slowness = Effect.getEffect(Effect.SLOWNESS)
                                .setDuration(slownessDuration)
                                .setAmplifier(slownessAmplifier);
                            entity.addEffect(slowness);
                            
                            // Deal damage based on distance
                            double damage = (1.0 - distance / explosionRadius) * 5.0;
                            if (damage > 0) {
                                EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                                    this, entity, EntityDamageEvent.DamageCause.PROJECTILE, (float)damage);
                                entity.attack(damageEvent);
                            }
                        }
                    }
                }
                
                // Create stone blocks for a short time
                if (createStone) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x * x + z * z <= 1) {
                                Vector3 stonePos = new Vector3(
                                    this.x + x,
                                    this.y,
                                    this.z + z
                                );
                                
                                // Only place stone in air blocks
                                Block block = this.level.getBlock(stonePos);
                                if (block.getId() == Block.AIR && ThreadLocalRandom.current().nextInt(100) < stoneChance) {
                                    // Randomly choose between regular stone or cobblestone
                                    int blockId = ThreadLocalRandom.current().nextBoolean() ? Block.STONE : Block.COBBLESTONE;
                                    this.level.setBlock(stonePos, Block.get(blockId), true, false);
                                    
                                    // Schedule stone removal after a few seconds
                                    this.server.getScheduler().scheduleDelayedTask(plugin, () -> {
                                        if (this.level.getBlock(stonePos).getId() == blockId) {
                                            this.level.setBlock(stonePos, Block.get(Block.AIR));
                                        }
                                    }, 100); // 5 seconds (100 ticks)
                                }
                            }
                        }
                    }
                }
                
                // Play earth impact sound
                this.level.addLevelSoundEvent(this, cn.nukkit.network.protocol.LevelSoundEventPacket.SOUND_BREAK);
            }
        }
    }
} 