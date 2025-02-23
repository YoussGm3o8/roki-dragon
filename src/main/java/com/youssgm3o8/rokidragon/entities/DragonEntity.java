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
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemFireCharge;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.scheduler.TaskHandler;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import nukkitcoders.mobplugin.entities.HorseBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.entities.EntityBedFireBall;

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

    public DragonEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setMaxHealth(100);
        this.setHealth(100);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, 100f));
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(100);
        super.initEntity();
        this.setHealth(100);

        this.fireProof = true;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_FIRE_IMMUNE, true);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, 100f));

        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SADDLED, true);

        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_WIDTH, this.getWidth()));
        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_HEIGHT, this.getHeight()));
        
        this.setSaddled(true);
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
        return super.onUpdate(currentTick);
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
        if (source.getDamage() >= this.getHealth()) {
            dismountAllPassengers();
        }
        return super.attack(source);
    }

    public void dismountAllPassengers() {
        for (Entity passenger : new ArrayList<>(this.passengers)) {
            dismountEntity(passenger);
        }
    }

    @Override
    public void kill() {
        dismountAllPassengers();
        super.kill();
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

    public void shootFireball(Player rider) {
        int cooldown = DragonPlugin.getInstance().getConfig().getInt("dragon-fireball-cooldown", 500);
        long now = System.currentTimeMillis();
        if (now - lastFireballTime < cooldown) {
            return;
        }

        // Check and consume fire charge first
        if (!consumeFireCharge(rider)) {
            rider.sendMessage("§cYou need a fire charge to shoot fireballs!");
            return;
        }
        
        lastFireballTime = now;
        
        // Store original XP values before setting to 0
        UUID uuid = rider.getUniqueId();
        if (!originalExperience.containsKey(uuid)) {
            originalExperience.put(uuid, rider.getExperience());
            originalXpLevel.put(uuid, rider.getExperienceLevel());
        }
        
        // Force XP bar to 0 and keep level
        int currentLevel = rider.getExperienceLevel();
        rider.setExperience(0, currentLevel); // Set both total XP and level
        rider.sendExperience(0); // Force client-side update
        
        // Schedule repeating task for smooth XP regeneration
        final int totalTicks = cooldown / 50; // Convert ms to ticks
        final int xpPerTick = 1000 / totalTicks; // Divide total XP by number of ticks
        final int[] currentTick = {0};
        
        // Store TaskHandler instead of int
        final TaskHandler[] task = {null};
        if (task[0] != null) {
            task[0].cancel(); // Cancel any existing task
        }
        task[0] = DragonPlugin.getInstance().getServer().getScheduler().scheduleRepeatingTask(DragonPlugin.getInstance(), () -> {
            if (!rider.isOnline() || rider.riding != this) {
                task[0].cancel();
                return;
            }
            currentTick[0]++;
            int newXp = Math.min(1000, currentTick[0] * xpPerTick);
            rider.setExperience(newXp, currentLevel);
            rider.sendExperience(newXp);
            
            if (currentTick[0] >= totalTicks) {
                rider.setExperience(1000, currentLevel);
                rider.sendExperience(1000);
                task[0].cancel();
                return;
            }
        }, 1);
        
        // Compute the forward vector using yaw and pitch
        double yawRad = Math.toRadians(this.yaw + 180);
        double pitchRad = Math.toRadians(this.pitch);
        double motX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double motY = -Math.sin(pitchRad);
        double motZ = Math.cos(yawRad) * Math.cos(pitchRad);
        
        // Spawn position in front of the dragon's head
        Vector3 pos = new Vector3(
            this.x + (-Math.sin(yawRad) * FIREBALL_OFFSET),
            this.y + 1.5,
            this.z + (Math.cos(yawRad) * FIREBALL_OFFSET)
        );
        
        CompoundTag fireballNBT = Entity.getDefaultNBT(pos)
            .putList(new ListTag<DoubleTag>("Motion")
                .add(new DoubleTag("", motX * FIREBALL_SPEED))
                .add(new DoubleTag("", motY * FIREBALL_SPEED))
                .add(new DoubleTag("", motZ * FIREBALL_SPEED)))
            .putList(new ListTag<DoubleTag>("Pos")
                .add(new DoubleTag("", pos.x))
                .add(new DoubleTag("", pos.y))
                .add(new DoubleTag("", pos.z)))
            .putLong("DragonID", this.getId());
        
        EntityBedFireBall fireball = new EntityBedFireBall(this.getChunk(), fireballNBT, this);
        fireball.setExplode(true);
        fireball.setMotion(new Vector3(motX * FIREBALL_SPEED, motY * FIREBALL_SPEED, motZ * FIREBALL_SPEED));
        fireball.spawnToAll();

        // Check if fire trail is enabled in config
        boolean fireTrailEnabled = DragonPlugin.getInstance().getConfig().getBoolean("fireball.fire-trail", true);
        
        if (fireTrailEnabled) {
            // Add multiple spiral particle trails
            DragonPlugin.getInstance().getServer().getScheduler().scheduleRepeatingTask(DragonPlugin.getInstance(), () -> {
                if (fireball.isClosed()) {
                    return; // Stop if fireball is gone
                }
                
                // Create 3 spiral trails with different radii and speeds
                double time = (System.currentTimeMillis() - now) / 100.0; // Time factor for spiral
                for (int i = 0; i < 3; i++) {
                    double radius = 0.3 + (i * 0.2); // Different radius for each spiral
                    double speed = 2.0 + (i * 0.5); // Different speed for each spiral
                    
                    // Calculate spiral positions
                    double spiralX = Math.cos(time * speed) * radius;
                    double spiralY = Math.sin(time * speed) * radius;
                    double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                    
                    Vector3 particlePos = fireball.getPosition().add(spiralX, spiralY, spiralZ);
                    
                    // Add randomized flame and smoke particles
                    if (Math.random() < 0.7) { // 70% chance for flame
                        fireball.level.addParticle(new cn.nukkit.level.particle.FlameParticle(particlePos));
                    }
                    if (Math.random() < 0.3) { // 30% chance for smoke
                        fireball.level.addParticle(new cn.nukkit.level.particle.SmokeParticle(particlePos));
                    }
                }
                
                // Add random sparks around the fireball
                for (int i = 0; i < 2; i++) {
                    double offsetX = (Math.random() - 0.5) * 0.5;
                    double offsetY = (Math.random() - 0.5) * 0.5;
                    double offsetZ = (Math.random() - 0.5) * 0.5;
                    Vector3 sparkPos = fireball.getPosition().add(offsetX, offsetY, offsetZ);
                    fireball.level.addParticle(new cn.nukkit.level.particle.FlameParticle(sparkPos));
                }
            }, 1); // Run every tick
        }
        
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_IMITATE_ENDER_DRAGON);
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_IMITATE_GHAST);
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
}
