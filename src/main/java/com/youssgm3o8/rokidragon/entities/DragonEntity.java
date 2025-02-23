package com.youssgm3o8.rokidragon.entities;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityInteractable;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.data.FloatEntityData;
import cn.nukkit.entity.data.Vector3fEntityData;
import cn.nukkit.entity.item.EntityVehicle;
import cn.nukkit.entity.mob.EntityEnderDragon;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemFireCharge;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.TextFormat;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import cn.nukkit.level.particle.FlameParticle;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.particle.ElectricSparkParticle;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.entities.EntityBedFireBall;
import com.youssgm3o8.rokidragon.items.DragonShardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;

public class DragonEntity extends HorseBase implements CustomEntity, EntityInteractable {
    public static final String IDENTIFIER = "custom:dragon";
    public static final EntityDefinition DEFINITION =
            EntityDefinition.builder().identifier(DragonEntity.IDENTIFIER).implementation(DragonEntity.class).build();

    private static final float DEFAULT_MOVE_SPEED =1.8f;
    private static final float VERTICAL_MOTION_UP = 0.2f;
    private static final float VERTICAL_MOTION_DOWN = -0.2f;
    private static final float PASSENGER_HEIGHT_OFFSET = 2.5f;
    private static final float FIREBALL_SPEED = 2f;
    private static final float FIREBALL_OFFSET = 8.0f;
    private long lastFireballTime = 0;

    private boolean isTeleporting = false;
    private long dismountStart = 0;
    private boolean shouldDespawn = false;

    protected ArrayList<Entity> passengers = new ArrayList<>();
    protected float moveSpeed = DEFAULT_MOVE_SPEED;
    private Player owner;

    private String dragonId;

    // Add fields to store original XP values
    private final Map<UUID, Integer> originalExperience = new HashMap<>();
    private final Map<UUID, Integer> originalXpLevel = new HashMap<>();

    private String dragonType = "Fire Dragon";
    private String particleEffect = "Flame Trail";
    private String dragonColor = "Red";

    private DragonShardManager shardManager;

    private static int DEFAULT_MAX_HEALTH = 100;
    private static double DEFAULT_BASE_DAMAGE = 15.0;
    private static double DEFAULT_DAMAGE_REDUCTION = 0.25;

    public static void setDefaultStats(int maxHealth, double baseDamage, double damageReduction) {
        DEFAULT_MAX_HEALTH = maxHealth;
        DEFAULT_BASE_DAMAGE = baseDamage;
        DEFAULT_DAMAGE_REDUCTION = damageReduction;
    }

    public DragonEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setMaxHealth(100);
        this.setHealth(100);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, 100f));
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(DEFAULT_MAX_HEALTH);
        super.initEntity();
        this.setHealth((float)DEFAULT_MAX_HEALTH);

        this.fireProof = true;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_FIRE_IMMUNE, true);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, (float)DEFAULT_MAX_HEALTH));

        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SADDLED, true);

        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_WIDTH, this.getWidth()));
        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_HEIGHT, this.getHeight()));
        
        this.setSaddled(true);

        // Load customization from NBT if exists
        if (this.namedTag.contains("DragonType")) {
            this.dragonType = this.namedTag.getString("DragonType");
        }
        if (this.namedTag.contains("ParticleEffect")) {
            this.particleEffect = this.namedTag.getString("ParticleEffect");
        }
        if (this.namedTag.contains("DragonColor")) {
            this.dragonColor = this.namedTag.getString("DragonColor");
        }

        // Make dragon damageable
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_NO_AI, false);
    }

    @Override
    public EntityDefinition getEntityDefinition() {
        return DEFINITION;
    }

    @Override
    public int getNetworkId() {
        return EntityEnderDragon.NETWORK_ID;
    }

    @Override
    public void spawnTo(Player player) {
        AddEntityPacket pk = new AddEntityPacket();
        pk.type = this.getNetworkId();
        pk.entityUniqueId = this.getId();
        pk.entityRuntimeId = this.getId();
        pk.x = (float) this.x;
        pk.y = (float) this.y;
        pk.z = (float) this.z;
        pk.speedX = (float) this.motionX;
        pk.speedY = (float) this.motionY;
        pk.speedZ = (float) this.motionZ;
        pk.yaw = (float) this.yaw;
        pk.pitch = (float) this.pitch;

        pk.attributes = new cn.nukkit.entity.Attribute[]{
                cn.nukkit.entity.Attribute.getAttribute(cn.nukkit.entity.Attribute.MAX_HEALTH).setMaxValue(100.0f).setValue(100.0f),
                cn.nukkit.entity.Attribute.getAttribute(cn.nukkit.entity.Attribute.MOVEMENT_SPEED).setValue(moveSpeed)
        };

        pk.metadata = this.dataProperties;
        player.dataPacket(pk);

        // Apply visual customizations
        applyCustomizations();

        super.spawnTo(player);
    }

    @Override
    public boolean mountEntity(Entity entity, byte mode) {
        // If this dragon is an admin dragon allow any player to mount it
        if (this.getDragonId() != null && this.getDragonId().startsWith("admin")) {
            return super.mountEntity(entity, mode);
        }
        // Instead of using equals(), compare UUIDs to check for owner access.
        if (entity instanceof Player && owner != null &&
            ((Player) entity).getUniqueId().equals(owner.getUniqueId())) {
            Objects.requireNonNull(entity, "The target of the mounting entity can't be null");

            if (entity.riding != null) {
                dismountEntity(entity);
                entity.resetFallDistance();
                this.motionX = 0;
                this.motionZ = 0;
                this.stayTime = 20;
            } else {
                if (entity instanceof Player && ((Player) entity).isSleeping()) {
                    return false;
                }

                if (isPassenger(entity)) {
                    return false;
                }

                broadcastLinkPacket(entity, SetEntityLinkPacket.TYPE_RIDE);

                entity.riding = this;
                entity.setDataFlag(DATA_FLAGS, DATA_FLAG_RIDING, true);
                entity.setDataProperty(new Vector3fEntityData(DATA_RIDER_SEAT_POSITION, new Vector3f(-0.5f, 4f, -1f)));
                passengers.add(entity);

                // Store original XP values when mounting
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    originalExperience.put(player.getUniqueId(), player.getExperience());
                    originalXpLevel.put(player.getUniqueId(), player.getExperienceLevel());
                    // Set XP bar to full (1000 is max experience per level)
                    player.setExperience(1000, player.getExperienceLevel());
                }
            }

            return true;
        } else {
            ((Player) entity).sendMessage("§cOnly the owner can ride this dragon.");
            return false;
        }
    }
    
    @Override
    public boolean dismountEntity(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            // Restore original XP values before dismounting
            restorePlayerXP(player);
        }
        
        if (entity.riding == null || !this.passengers.contains(entity)) {
            return false;
        }

        if (isTeleporting) {
            return false;
        }

        isTeleporting = true;

        SetEntityLinkPacket pk = new SetEntityLinkPacket();
        pk.vehicleUniqueId = this.getId();
        pk.riderUniqueId = entity.getId();
        pk.type = SetEntityLinkPacket.TYPE_REMOVE;
        Server.broadcastPacket(this.getViewers().values(), pk);

        entity.riding = null;
        entity.setDataFlag(DATA_FLAGS, DATA_FLAG_RIDING, false);
        this.passengers.remove(entity);

        if (entity instanceof Player) {
            Player player = (Player) entity;
            Vector3 currentPosition = this.getPosition();
            player.teleport(currentPosition);
            
            player.addEffect(cn.nukkit.potion.Effect.getEffect(cn.nukkit.potion.Effect.RESISTANCE)
                .setDuration(5 * 20)
                .setAmplifier(254));
        }

        this.dismountStart = System.currentTimeMillis();
        this.shouldDespawn = true;

        isTeleporting = false;
        return true;
    }
    
    @Override
    public boolean onUpdate(int currentTick) {
        Iterator<Entity> linkedIterator = this.passengers.iterator();
  
        while (linkedIterator.hasNext()) {
            Entity linked = (Entity) linkedIterator.next();
            if (!linked.isAlive()) {
                if (linked.riding == this) {
                    linked.riding = null;
                }
                linkedIterator.remove();
            }
        }

        if (!this.passengers.isEmpty()) {
            this.setImmobile(false);
            this.move(this.motionX, 0, this.motionZ);
            this.updateMovement();
        } else {
            this.setImmobile(true);
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
        }

        if (this.passengers.isEmpty() && shouldDespawn) {
            long elapsed = System.currentTimeMillis() - dismountStart;
            if (elapsed >= 5 * 60_000) {
                this.close();
                return false;
            }
        }

        if (super.onUpdate(currentTick)) {
            // Apply particle effects every few ticks
            if (currentTick % 5 == 0) {
                applyCustomizations();
            }
            return true;
        }
        return false;
    }

    private static final float MAX_SPEED = 0.92f;
    @Override
    public void onPlayerInput(Player player, double strafe, double forward) {
        this.stayTime = 0;
        this.moveTime = 20;
        this.route = null;
        this.target = null;
    
        double playerYaw = (player.getYaw() + 180) % 360;
        double playerPitch = player.getPitch();

        this.setRotation(playerYaw, playerPitch);

        forward = -forward;
        strafe = -strafe;
    
        strafe *= 0.4;
    
        double f = strafe * strafe + forward * forward;
        double friction = 0.91;
    
        this.yaw = playerYaw;
    
        if (playerPitch < -20) {
            this.pitch = playerPitch;
            this.motionY = VERTICAL_MOTION_UP;
        } 
        else if (playerPitch > 20) {
            this.pitch = playerPitch;
            this.motionY = VERTICAL_MOTION_DOWN;
        } 
        else {
            this.motionY = 0;
            this.pitch = playerPitch;
        }
    
        if (f >= 1.0E-4) {
            f = Math.sqrt(f);
    
            if (f < 1) {
                f = 1;
            }
    
            f = friction / f;
            strafe *= f;
            forward *= f;
    
            double yawRadians = Math.toRadians(-playerYaw);
            double sinYaw = Math.sin(yawRadians);             
            double cosYaw = Math.cos(yawRadians);
        
            this.motionX = (strafe * cosYaw + forward * sinYaw) * this.moveSpeed;
            this.motionZ = (forward * cosYaw - strafe * sinYaw) * this.moveSpeed;

            double speed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            if (speed > MAX_SPEED ) {
                double scale = MAX_SPEED / speed;
                this.motionX *= scale;
                this.motionZ *= scale;
            }
            if (forward > 0) {
                this.motionX *= 0.3;
                this.motionZ *= 0.3;
            }
        } else {
            this.motionX = 0;
            this.motionZ = 0;
        }
    }

     @Override
    protected DataPacket createAddEntityPacket() {
        AddEntityPacket addEntity = new AddEntityPacket();
        addEntity.type = this.getNetworkId();
        addEntity.entityUniqueId = this.getId();
        addEntity.entityRuntimeId = this.getId();
        addEntity.yaw = (float) this.yaw;
        addEntity.headYaw = (float) this.yaw;
        addEntity.pitch = (float) this.pitch;
        addEntity.x = (float) this.x;
        addEntity.y = (float) this.y;
        addEntity.z = (float) this.z;
        addEntity.speedX = (float) this.motionX;
        addEntity.y = (float) this.y + this.getBaseOffset();
        addEntity.speedZ = (float) this.motionZ;
        addEntity.metadata = this.dataProperties.clone();
        addEntity.attributes = new Attribute[]{Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(100).setValue(100)};

        addEntity.links = new EntityLink[this.passengers.size()];

        for(int i = 0; i < addEntity.links.length; ++i) {
           addEntity.links[i] = new EntityLink(this.id, ((Entity)this.passengers.get(i)).getId(), (byte)(i == 0 ? 1 : 2), false, false, 0.0F);
        }

        return addEntity;
    }

    @Override
    public float getWidth() {
        return 4.0f;
    }

    @Override
    public float getHeight() {
        return 4.0f;
    }

    @Override
    public float getLength() {
        return 6.0f;
    }
   
    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Dragon";
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source.getCause() == EntityDamageEvent.DamageCause.VOID) {
            // Always allow void damage
            return super.attack(source);
        }

        // Prevent damage from owner
        if (source instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
            if (damager instanceof Player && damager.equals(this.owner)) {
                return false;
            }
        }

        // Apply damage reduction based on dragon type
        float damage = source.getDamage();
        switch (this.dragonType) {
            case "Fire Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                    source.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    return false; // Immune to fire damage
                }
                damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                break;
            case "Ice Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                    return false; // Immune to drowning
                }
                if (source.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                    source.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    damage *= 1.5f; // Takes more damage from fire
                } else {
                    damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                }
                break;
            case "Lightning Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
                    return false; // Immune to lightning
                }
                damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                break;
        }

        source.setDamage(damage);
        boolean result = super.attack(source);

        // Update health display
        if (result) {
            this.setDataProperty(new FloatEntityData(DATA_HEALTH, (float) this.getHealth()));
            
            // Check if dragon died from this damage
            if (this.getHealth() <= 0) {
                handleDeath();
            }
        }

        return result;
    }

    private void handleDeath() {
        // Dismount all passengers
        dismountAllPassengers();

        // Restore XP for all passengers
        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (passenger instanceof Player) {
                restorePlayerXP((Player) passenger);
            }
        }

        // Notify owner and start cooldown
        if (owner != null) {
            DragonPlugin.getInstance().onDragonDeath(owner);
        }

        // Drop items or create effects
        if (this.level != null) {
            // Add death particles
            for (int i = 0; i < 20; i++) {
                this.level.addParticle(new cn.nukkit.level.particle.ExplodeParticle(this.add(
                    Math.random() * 2 - 1,
                    Math.random() * 2,
                    Math.random() * 2 - 1
                )));
            }

            // Play death sound
            this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_EXPLODE);
        }

        // Remove from database
        if (owner != null) {
            DragonPlugin.getInstance().getDatabaseManager().removeDragon(owner.getUniqueId().toString());
        }

        // Finally, close the entity
        this.close();
    }

    @Override
    public void kill() {
        handleDeath();
        super.kill();
    }

    public void dismountAllPassengers() {
        for (Entity passenger : new ArrayList<>(this.passengers)) {
            dismountEntity(passenger);
        }
    }

    @Override
    public void updatePassengers() {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (!passenger.isAlive() || passenger.riding != this) {
                dismountEntity(passenger);
                continue;
            }

            passenger.setPosition(new Vector3(
                    this.x,
                    this.y + PASSENGER_HEIGHT_OFFSET,
                    this.z
            ));
        }
    }

    @Override
    public int getKillExperience() {
        return 0;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return this.owner;
    }

    public String getDragonId() {
        return this.dragonId;
    }

    public void setDragonId(String dragonId) {
        this.dragonId = dragonId;
    }

    @Override
    public String getInteractButtonText() {
        return "action.interact.mount";
    }

    @Override
    public boolean canDoInteraction() {
        return passengers.isEmpty();
    }

    public void shoot() {
        if (System.currentTimeMillis() - lastFireballTime < DragonPlugin.getInstance().getConfig().getInt("dragon-fireball-cooldown", 500)) {
            return;
        }
        lastFireballTime = System.currentTimeMillis();

        String dragonType = DragonPlugin.getInstance().getDatabaseManager().getDragonType(this.dragonId);
        switch (dragonType) {
            case "Fire Dragon":
                shootFireball();
                break;
            case "Ice Dragon":
                shootIceball();
                break;
            case "Lightning Dragon":
                shootLightningBall();
                break;
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof EntityGhastFireBall) {
            CompoundTag nbt = entity.namedTag;
            if (nbt != null && nbt.contains("DragonID") && nbt.getLong("DragonID") == this.getId()) {
                return false;
            }
        }
        return super.canCollideWith(entity);
    }

    private boolean consumeFireCharge(Player player) {
        int fireChargeSlot = -1;
        
        // Find fire charge in player's inventory
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            if (item.getId() == Item.FIRE_CHARGE) {
                fireChargeSlot = slot;
                break;
            }
        }

        if (fireChargeSlot == -1) {
            return false;
        }

        // Remove one fire charge
        Item fireCharge = player.getInventory().getItem(fireChargeSlot);
        fireCharge.setCount(fireCharge.getCount() - 1);
        player.getInventory().setItem(fireChargeSlot, fireCharge);
        
        return true;
    }

    public void restorePlayerXP(Player player) {
        UUID uuid = player.getUniqueId();
        if (originalExperience.containsKey(uuid) && originalXpLevel.containsKey(uuid)) {
            player.setExperience(originalExperience.get(uuid), originalXpLevel.get(uuid));
            originalExperience.remove(uuid);
            originalXpLevel.remove(uuid);
        }
    }

    @Override
    public void close() {
        // Restore XP for all passengers before closing
        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (passenger instanceof Player) {
                restorePlayerXP((Player) passenger);
            }
        }
        super.close();
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        
        // Save customization to NBT
        this.namedTag.putString("DragonType", this.dragonType);
        this.namedTag.putString("ParticleEffect", this.particleEffect);
        this.namedTag.putString("DragonColor", this.dragonColor);
    }

    public void setDragonType(String type) {
        if (type == null) {
            type = "Fire Dragon"; // Default to Fire Dragon if null
        }
        this.dragonType = type;
        // Apply visual changes based on type
        switch (type) {
            case "Fire Dragon":
                this.fireProof = true;
                break;
            case "Ice Dragon":
                this.fireProof = false;
                // Add ice resistance
                break;
            case "Lightning Dragon":
                this.fireProof = true;
                // Add lightning effects
                break;
            default:
                this.fireProof = true; // Default behavior
                break;
        }
    }

    public String getDragonType() {
        return this.dragonType;
    }

    public void setParticleEffect(String effect) {
        this.particleEffect = effect;
    }

    public String getParticleEffect() {
        return this.particleEffect;
    }

    public void setDragonColor(String color) {
        this.dragonColor = color;
        // Apply color changes
        // This would typically involve changing the dragon's texture or particle colors
    }

    public String getDragonColor() {
        return this.dragonColor;
    }

    private void applyCustomizations() {
        // Apply particle effects based on type and customization
        if (this.dragonType == null) {
            this.dragonType = "Fire Dragon"; // Default to Fire Dragon if null
        }
        if (this.particleEffect == null) {
            this.particleEffect = "Flame Trail"; // Default particle effect
        }
        
        switch (this.dragonType) {
            case "Fire Dragon":
                if (this.particleEffect.equals("Flame Trail")) {
                    this.level.addParticle(new cn.nukkit.level.particle.FlameParticle(this.add(0, 1.2, 0)));
                }
                break;
            case "Ice Dragon":
                if (this.particleEffect.equals("Ice Trail")) {
                    // Add ice particles
                    this.level.addParticleEffect(this.add(0, 1.2, 0), ParticleEffect.FALLING_DUST_TOP_SNOW);
                }
                break;
            case "Lightning Dragon":
                if (this.particleEffect.equals("Lightning Trail")) {
                    // Add lightning particles
                    this.level.addParticle(new cn.nukkit.level.particle.ElectricSparkParticle(this.add(0, 1.2, 0)));
                }
                break;
            default:
                // Default to fire particles
                this.level.addParticle(new cn.nukkit.level.particle.FlameParticle(this.add(0, 1.2, 0)));
                break;
        }
    }

    public void setShardManager(DragonShardManager manager) {
        this.shardManager = manager;
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, health));
    }

    private void shootFireball() {
        // Calculate projectile spawn position
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        Vector3 directionVector = this.getDirectionVector();
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Create and spawn fireball
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        EntityBedFireBall fireball = new EntityBedFireBall(this.getChunk(), nbt, this);
        fireball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        fireball.spawnToAll();
    }

    private void shootIceball() {
        // Calculate projectile spawn position
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        Vector3 directionVector = this.getDirectionVector();
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Create and spawn iceball
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        EntityIceBall iceball = new EntityIceBall(this.getChunk(), nbt, this);
        iceball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        iceball.spawnToAll();
    }

    private void shootLightningBall() {
        // Calculate projectile spawn position
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        Vector3 directionVector = this.getDirectionVector();
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Create and spawn lightning ball
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        EntityLightningBall lightningBall = new EntityLightningBall(this.getChunk(), nbt, this);
        lightningBall.setMotion(directionVector.multiply(FIREBALL_SPEED));
        lightningBall.spawnToAll();
    }
}
