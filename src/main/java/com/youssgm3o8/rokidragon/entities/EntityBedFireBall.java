package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.entity.Entity;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.GameRule;
import com.youssgm3o8.rokidragon.DragonPlugin; // Import DragonPlugin

public class EntityBedFireBall extends EntityGhastFireBall {
    private final DragonPlugin plugin; // Add plugin field

    // Constructor 1: Requires plugin instance
    public EntityBedFireBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.plugin = plugin;
    }
    
    // Constructor 2: Requires plugin instance
    public EntityBedFireBall(DragonPlugin plugin, FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
        this.plugin = plugin;
    }
    
    @Override
    protected void onHit() {
        // Explicitly call explode when the projectile hits something (block or entity)
        this.explode();
        // We can add specific logic here if needed (e.g., different behavior for entity vs block hit)
        super.onHit(); // Call super method if necessary, though exploding usually stops further processing
    }
    
    @Override
    public void explode() {
        if (!this.closed) {
            this.close();
            // Use the config value for explosion radius, default to 1.2 if not found
            double explosionRadius = plugin.getConfig().getDouble("abilities.fireball.explosion_radius", 1.2); 
            EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, explosionRadius); 
            this.server.getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                // Pass the potentially modified force from the event
                FireBedExplosion explosion = new FireBedExplosion(plugin, this, ev.getForce(), this.shootingEntity); 
                if (this.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
                    explosion.explodeB(); // Call explodeB only once, respecting MOB_GRIEFING
                } else {
                    // If mob griefing is off, we might still want the explosion effect without block damage
                    // However, FireBallExplosion's explodeB handles block breaking based on config.
                    // Let FireBedExplosion handle the griefing check internally based on config.
                    // We call explodeB once here, and it will respect config settings.
                    explosion.explodeB(); 
                }
            }
        }
    }
}
