package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.level.particle.WaterDripParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import com.youssgm3o8.rokidragon.DragonPlugin;

import java.util.concurrent.ThreadLocalRandom;

public class EntityWaterBall extends EntityProjectile {
    private Entity shootingEntity;
    private DragonPlugin plugin;

    public EntityWaterBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.plugin = plugin;
    }
    
    public EntityWaterBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt);
        this.plugin = plugin;
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
        return 0.025f; // Slightly less gravity than other projectiles
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
        return Item.PRISMARINE_CRYSTALS; // Use prismarine crystals model
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        // Add trailing particles
        if (ThreadLocalRandom.current().nextDouble() < 0.7) {
            this.level.addParticle(new WaterDripParticle(this));
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            this.level.addParticle(new BubbleParticle(this));
        }

        return super.onUpdate(currentTick);
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        if (entity != this.shootingEntity) {
            this.onHit(); // Call onHit when colliding with an entity
        }
    }
    
    // Helper method to handle collision with any position
    private void handleCollision(Position position) {
        this.onHit(); // Delegate to onHit method
    }

    @Override
    protected void onHit() {
        if (!this.closed) {
            this.close();
            
            // Get splash radius from config or use default
            int radius = plugin.getConfig().getInt("abilities.waterball.splash_radius", 3);
            
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 0.5); // Small visual effect only
            this.server.getPluginManager().callEvent(ev);
            
            if (!ev.isCancelled()) {
                // Create water splash effect
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + z * z <= radius * radius) {
                            // Random splash effect with water particles
                            if (ThreadLocalRandom.current().nextDouble() < 0.7) {
                                Vector3 splashPos = new Vector3(
                                    this.x + x + ThreadLocalRandom.current().nextDouble() - 0.5,
                                    this.y + ThreadLocalRandom.current().nextDouble(),
                                    this.z + z + ThreadLocalRandom.current().nextDouble() - 0.5
                                );
                                this.level.addParticle(new WaterDripParticle(splashPos));
                                
                                // Add bubbles occasionally
                                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                                    this.level.addParticle(new BubbleParticle(splashPos));
                                }
                            }
                        }
                    }
                }
                
                // Apply water-based effects to nearby entities
                for (Entity entity : this.level.getNearbyEntities(this.getBoundingBox().grow(radius, radius, radius))) {
                    if (entity != this.shootingEntity) {
                        // Extinguish if entity is on fire
                        entity.extinguish();
                        
                        // Apply slowness effect (like swimming in water)
                        int duration = plugin.getConfig().getInt("abilities.waterball.slowness_duration_ticks", 80); // 4 seconds
                        int amplifier = plugin.getConfig().getInt("abilities.waterball.slowness_amplifier", 1); // Slowness II
                        
                        Effect slowness = Effect.getEffect(Effect.SLOWNESS)
                            .setDuration(duration)
                            .setAmplifier(amplifier);
                        entity.addEffect(slowness);
                    }
                }
                
                // Replace some air blocks with temporary water blocks
                int waterChance = plugin.getConfig().getInt("abilities.waterball.water_chance", 50); // Percentage
                boolean createWater = plugin.getConfig().getBoolean("abilities.waterball.create_water", true);
                
                if (createWater) {
                    // Create a small puddle of water that will auto-disappear
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x * x + z * z <= 1) {
                                Vector3 waterPos = new Vector3(
                                    this.x + x,
                                    this.y,
                                    this.z + z
                                );
                                
                                // Only place water in air blocks
                                Block block = this.level.getBlock(waterPos);
                                if (block.getId() == Block.AIR && ThreadLocalRandom.current().nextInt(100) < waterChance) {
                                    this.level.setBlock(waterPos, Block.get(Block.WATER), true, false);
                                    
                                    // Schedule water removal after a few seconds
                                    this.server.getScheduler().scheduleDelayedTask(plugin, () -> {
                                        if (this.level.getBlock(waterPos).getId() == Block.WATER) {
                                            this.level.setBlock(waterPos, Block.get(Block.AIR));
                                        }
                                    }, 60); // 3 seconds (60 ticks)
                                }
                            }
                        }
                    }
                }
                
                // Add final splash sound effect
                this.level.addLevelSoundEvent(this, cn.nukkit.network.protocol.LevelSoundEventPacket.SOUND_SPLASH);
            }
        }
    }
} 