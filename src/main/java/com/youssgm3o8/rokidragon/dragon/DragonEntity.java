package com.youssgm3o8.rokidragon.dragon;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityInteractable;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.data.FloatEntityData;
import cn.nukkit.entity.data.Vector3fEntityData;
import cn.nukkit.entity.mob.EntityEnderDragon;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.network.protocol.types.EntityLink;
import cn.nukkit.utils.TextFormat;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.entities.projectile.EntityGhastFireBall;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.particle.ElectricSparkParticle;
import cn.nukkit.block.Block;

import com.youssgm3o8.rokidragon.DragonPlugin;
import com.youssgm3o8.rokidragon.entities.EntityBedFireBall;
import com.youssgm3o8.rokidragon.entities.EntityIceBall;
import com.youssgm3o8.rokidragon.entities.EntityLightningBall;
import com.youssgm3o8.rokidragon.manager.DragonShardManager; // Ensure this is the correct import

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the custom Dragon entity that players can own, ride, and customize.
 * Extends HorseBase for riding mechanics and implements CustomEntity for Nukkit integration.
 */
public class DragonEntity extends HorseBase implements CustomEntity, EntityInteractable {
    // Replaced generic identifier with type-specific identifiers
    public static final String FIRE_DRAGON_IDENTIFIER = "roki:fire_dragon";
    public static final String ICE_DRAGON_IDENTIFIER = "roki:ice_dragon";
    public static final String LIGHTNING_DRAGON_IDENTIFIER = "roki:lightning_dragon";
    public static final String WATER_DRAGON_IDENTIFIER = "roki:water_dragon";
    public static final String EARTH_DRAGON_IDENTIFIER = "roki:earth_dragon";
    
    // Default identifier (used as fallback)
    public static final String DEFAULT_IDENTIFIER = FIRE_DRAGON_IDENTIFIER;
    
    // The actual definition will be set dynamically based on dragon type
    private static EntityDefinition DEFINITION;
    
    // Get the correct identifier based on dragon type
    public static String getIdentifierForType(String dragonType) {
        if (dragonType == null) {
            return DEFAULT_IDENTIFIER;
        }
        
        switch (dragonType) {
            case "Fire Dragon":
                return FIRE_DRAGON_IDENTIFIER;
            case "Ice Dragon":
                return ICE_DRAGON_IDENTIFIER;
            case "Lightning Dragon":
                return LIGHTNING_DRAGON_IDENTIFIER;
            case "Water Dragon":
                return WATER_DRAGON_IDENTIFIER;
            case "Earth Dragon":
                return EARTH_DRAGON_IDENTIFIER;
            default:
                return DEFAULT_IDENTIFIER;
        }
    }
    
    // Get the entity definition with the appropriate identifier
    public static EntityDefinition getDefinitionForType(String dragonType) {
        String identifier = getIdentifierForType(dragonType);
        return EntityDefinition.builder()
            .identifier(identifier)
            .implementation(DragonEntity.class)
            .build();
    }

    // Removed DEFAULT_MOVE_SPEED constant, will use config value
    private static final float PASSENGER_HEIGHT_OFFSET = 2.5f;
    private static final float FIREBALL_SPEED = 2f;
    private static final float FIREBALL_OFFSET = 8.0f;
    private long lastFireballTime = 0;

    // NBT Keys Constants
    private static final String NBT_KEY_DRAGON_TYPE = "DragonType";
    private static final String NBT_KEY_PARTICLE_EFFECT = "ParticleEffect";
    private static final String NBT_KEY_DRAGON_COLOR = "DragonColor";
    private static final String NBT_KEY_IS_ADMIN_DRAGON = "IsAdminDragon";
    // Note: "DragonID" used in canCollideWith seems to refer to the projectile's NBT, not this entity's.
    private DragonPlugin plugin; // Keep field, make non-final

    // Constants for Magic Numbers
    private static final Vector3f RIDER_SEAT_POSITION = new Vector3f(-0.8f, 4f, -1f);
    private static final int DISMOUNT_RESISTANCE_DURATION_TICKS = 5 * 20;
    private static final int DISMOUNT_RESISTANCE_AMPLIFIER = 254; // Max amplifier
    private static final long EMPTY_DESPAWN_DELAY_MILLIS = 5 * 60 * 1000; // 5 minutes
    private static final float PITCH_THRESHOLD_UP = -20f;
    private static final float PITCH_THRESHOLD_DOWN = 20f;
    private static final double STRAFE_MULTIPLIER = 0.4;
    private static final double FRICTION = 0.91;
    private static final double FORWARD_SLOWDOWN_MULTIPLIER = 0.3;
    private static final float ICE_DRAGON_FIRE_VULNERABILITY = 1.5f;
    private static final int DEATH_PARTICLE_COUNT = 20;
    private static final int PARTICLE_EFFECT_INTERVAL_TICKS = 5;
    private static final int ACTION_BAR_UPDATE_INTERVAL_TICKS = 10;
    private static final double MILLIS_PER_SECOND = 1000.0;

    // --- Flight Physics Constants ---
    private static final double ACCELERATION_RATE = 0.08; // How quickly the dragon gains speed
    private static final double DRAG_COEFFICIENT = 0.025; // How quickly the dragon slows down naturally
    private static final double GRAVITY_ACCEL = 0.04;     // Constant downward pull
    private static final double DIVE_ACCEL_MULTIPLIER = 1.2; // Extra downward acceleration when diving steeply
    private static final double LIFT_COEFFICIENT = 0.05; // Upward force generated by forward motion/pitch (counteracts gravity)
    private static final float MAX_VERTICAL_SPEED_UP = 1.0f; // Maximum upward speed cap
    private static final float MAX_VERTICAL_SPEED_DOWN = -2.0f; // Maximum downward speed cap (negative)
    private static final float MIN_LIFT_SPEED = 0.2f; // Minimum forward speed required to generate significant lift
    private static final float FLAT_FLIGHT_SPEED_MULTIPLIER = 1.5f; // Speed multiplier for level flight
    // --- End Flight Physics Constants ---

    private boolean isTeleporting = false;
    private long dismountStart = 0;
    private boolean shouldDespawn = false;
    protected ArrayList<Entity> passengers = new ArrayList<>();
    protected float moveSpeed; // Initialize in initialize() method
    private Player owner;

    private String dragonId;

    // --- Plugin Health Tracking ---
    private float pluginHealth;
    private float pluginMaxHealth;
    // --- End Plugin Health Tracking ---

    // --- Flight Physics State ---
    private double currentFlightSpeed = 0.0; // Tracks the magnitude of actual speed
    private double targetSpeed = 0.0;        // Tracks the speed the player wants based on input
    private double lastStrafeInput = 0.0;    // Last strafe input from player (-1 to 1)
    private double lastForwardInput = 0.0;   // Last forward input from player (-1 to 1)
    private double currentMaxSpeed = MAX_SPEED; // Tracks the current maximum speed considering factors like flat flight
    // --- End Flight Physics State ---

    // Removed fields for storing original XP values

    private String dragonType = "Fire Dragon";
    private String particleEffect = "Flame Trail";
    private String dragonColor = "Red";

    private DragonShardManager shardManager;

    private static int DEFAULT_MAX_HEALTH = 100;
    private static double DEFAULT_BASE_DAMAGE = 15.0;
    private static double DEFAULT_DAMAGE_REDUCTION = 0.25;

    // Flight speed constants
    private static final float MAX_SPEED = 0.92f;

    /**
     * Sets the static default stats for all DragonEntity instances, typically loaded from config.
     * @param maxHealth Default maximum health.
     * @param baseDamage Default base damage (currently unused).
     * @param damageReduction Default damage reduction multiplier.
     */
    public static void setDefaultStats(int maxHealth, double baseDamage, double damageReduction) {
        DEFAULT_MAX_HEALTH = maxHealth;
        DEFAULT_BASE_DAMAGE = baseDamage; // Note: Base damage is not currently used in attack logic
        DEFAULT_DAMAGE_REDUCTION = damageReduction;
    }

    /**
     * Constructs a new DragonEntity.
     * @param chunk The chunk the entity is in.
     * @param nbt The NBT data for the entity.
     */
    public DragonEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        // Plugin instance and other dependencies are set via initialize() after creation
    }

    /**
     * Initializes the entity after creation with necessary dependencies.
     * @param plugin The main plugin instance.
     */
    public void initialize(DragonPlugin plugin) {
        this.plugin = plugin;
        // Initialize other dependencies if needed, e.g., shardManager
        if (plugin != null) {
            this.setShardManager(plugin.getShardManager());
            // Initialize moveSpeed from config
            this.moveSpeed = (float) plugin.getConfig().getDouble("dragon_stats.move_speed", 1.8);
        }
    }

    /**
     * Initializes the entity's properties, flags, and loads customization from NBT.
     * Called after the entity is created.
     */
    @Override
    public void initEntity() {
        // --- Set Plugin Health ---
        this.pluginMaxHealth = DEFAULT_MAX_HEALTH; // Use the static default loaded from config
        this.pluginHealth = this.pluginMaxHealth; // Start at full plugin health
        // --- End Set Plugin Health ---

        // --- Set Nukkit Health to Low But Survivable Value ---
        this.setMaxHealth(10); // Set Nukkit max health to something survivable
        super.initEntity(); // Call super *after* setting Nukkit max health
        this.setHealth(10f); // Set Nukkit current health to survive environmental effects
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, 10f)); // Ensure data property reflects health
        
        // Make name tag visible but health bar minimal
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        // --- End Set Nukkit Health ---

        this.fireProof = true;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_FIRE_IMMUNE, true);

        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SADDLED, true);

        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_WIDTH, this.getWidth()));
        this.setDataProperty(new FloatEntityData(DATA_BOUNDING_BOX_HEIGHT, this.getHeight()));
        
        this.setSaddled(true);

        // Load customization from NBT if exists
        if (this.namedTag.contains(NBT_KEY_DRAGON_TYPE)) {
            this.dragonType = this.namedTag.getString(NBT_KEY_DRAGON_TYPE);
            // *** ADDED LOGGING ***
            if (plugin != null) { // Check if plugin instance is available
                plugin.getLogger().info("[DragonEntity Init Debug] Loaded DragonType '" + this.dragonType + "' from NBT for entity ID " + this.getId());
            } else {
                System.out.println("[DragonEntity Init Debug - No Plugin] Loaded DragonType '" + this.namedTag.getString(NBT_KEY_DRAGON_TYPE) + "' from NBT for entity ID " + this.getId());
            }
            // *** END LOGGING ***
        }
        if (this.namedTag.contains(NBT_KEY_PARTICLE_EFFECT)) {
            this.particleEffect = this.namedTag.getString(NBT_KEY_PARTICLE_EFFECT);
        }
        if (this.namedTag.contains(NBT_KEY_DRAGON_COLOR)) {
            this.dragonColor = this.namedTag.getString(NBT_KEY_DRAGON_COLOR);
        }

        // Make dragon damageable
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_NO_AI, false);
        
        // Log the entity identifier being used
        String identifier = getIdentifierForType(this.dragonType);
        if (plugin != null) {
            plugin.getLogger().info("[DragonEntity] Using entity identifier: " + identifier + " for dragon type: " + this.dragonType);
        } else {
            System.out.println("[DragonEntity - No Plugin] Using entity identifier: " + identifier + " for dragon type: " + this.dragonType);
        }
        
        // Apply visual customizations immediately on initialization
        applyTypeSpecificProperties();
    }

    /**
     * Applies specific properties based on the dragon type
     */
    private void applyTypeSpecificProperties() {
        switch (this.dragonType) {
            case "Fire Dragon":
                this.fireProof = true;
                // Set any Fire Dragon specific properties
                break;
            case "Ice Dragon":
                this.fireProof = false;
                // Set any Ice Dragon specific properties
                break;
            case "Lightning Dragon":
                this.fireProof = true;
                // Set any Lightning Dragon specific properties
                break;
            case "Water Dragon":
                this.fireProof = false;
                // Set Water Dragon specific properties
                break;
            case "Earth Dragon":
                this.fireProof = false;
                // Set Earth Dragon specific properties
                break;
            default:
                this.fireProof = true; // Default behavior
                break;
        }
        
        // Set the entity identifier specifically to match this dragon type
        // This will be used by clients with the resource pack to select the proper model
        String identifier = getIdentifierForType(this.dragonType);
        if (plugin != null) {
            plugin.getLogger().info("[DragonEntity] Setting identifier to: " + identifier + " for dragon: " + this.getName());
        } else {
            System.out.println("[DragonEntity - No Plugin] Setting identifier to: " + identifier + " for dragon: " + this.getName());
        }
        
        // This ensures that when the entity is first created
        // the visual appearance will match the dragon type
        if (this.level != null) {
            applyCustomizations();
        }
    }

    /**
     * Gets the EntityDefinition for this custom entity.
     * @return The entity definition.
     */
    @Override
    public EntityDefinition getEntityDefinition() {
        String currentType = this.dragonType != null ? this.dragonType : "Fire Dragon";
        return getDefinitionForType(currentType);
    }

    /**
     * Gets the network ID for this entity, using the Ender Dragon ID for appearance.
     * @return The network ID.
     */
    @Override
    public int getNetworkId() {
        return EntityEnderDragon.NETWORK_ID;
    }

    /**
     * Spawns the entity to a specific player, sending the necessary packets.
     * @param player The player to spawn the entity for.
     */
    @Override
    public void spawnTo(Player player) {
        // Debug the dragon type to see if it's correct
        if (plugin != null) {
            plugin.getLogger().info("[DragonEntity SpawnTo Debug] dragonType = '" + this.dragonType + "'");
        }
        
        // Get the correct identifier for this dragon type
        String identifier = getIdentifierForType(this.dragonType);
        
        // Log the entity identifier being used
        if (plugin != null) {
            plugin.getLogger().info("[DragonEntity SpawnTo] Using identifier: " + identifier + " for dragon: " + this.getName() + ", dragonType = " + this.dragonType);
        }
        
        // Create the packet for spawning the entity
        AddEntityPacket pk = new AddEntityPacket();
        pk.type = this.getNetworkId(); // All use the Ender Dragon network ID for rendering
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
                // Set minimal health attributes to keep entity alive but minimize health bar
                cn.nukkit.entity.Attribute.getAttribute(cn.nukkit.entity.Attribute.MAX_HEALTH).setMaxValue(10f).setValue(10f),
                cn.nukkit.entity.Attribute.getAttribute(cn.nukkit.entity.Attribute.MOVEMENT_SPEED).setValue(this.moveSpeed)
        };

        // Set specific metadata for dragon type (add a special tag to identify the dragon type)
        if (identifier.equals(FIRE_DRAGON_IDENTIFIER)) {
            this.setNameTag(TextFormat.RED + this.getNameTag());
        } else if (identifier.equals(ICE_DRAGON_IDENTIFIER)) {
            this.setNameTag(TextFormat.AQUA + this.getNameTag());
        } else if (identifier.equals(LIGHTNING_DRAGON_IDENTIFIER)) {
            this.setNameTag(TextFormat.YELLOW + this.getNameTag());
        } else if (identifier.equals(WATER_DRAGON_IDENTIFIER)) {
            this.setNameTag(TextFormat.BLUE + this.getNameTag());
        } else if (identifier.equals(EARTH_DRAGON_IDENTIFIER)) {
            this.setNameTag(TextFormat.GREEN + this.getNameTag());
        }
        
        // Ensure nametag is always visible
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);

        pk.metadata = this.dataProperties;
        player.dataPacket(pk);

        // Apply visual customizations like particles
        applyCustomizations();

        super.spawnTo(player);
    }

    /**
     * Handles an entity attempting to mount this dragon.
     * Only allows the owner (or anyone if it's an admin dragon) to mount.
     * @param entity The entity attempting to mount.
     * @param mode The mounting mode (unused).
     * @return {@code true} if mounting was successful, {@code false} otherwise.
     */
    @Override
    public boolean mountEntity(Entity entity, byte mode) {
        // If this dragon is an admin dragon allow any player to mount it
        // Check for a dragonId starting with "admin" like in the original code
        if (this.dragonId != null && this.dragonId.startsWith("admin")) {
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
                entity.setDataProperty(new Vector3fEntityData(DATA_RIDER_SEAT_POSITION, RIDER_SEAT_POSITION));
                passengers.add(entity);

                // Removed XP storing/setting logic
            }

            return true;
        } else {
            ((Player) entity).sendMessage("§cOnly the owner can ride this dragon.");
            return false;
        }
    }
    
    /**
     * Handles an entity dismounting this dragon.
     * Teleports the rider safely and applies resistance. Starts the despawn timer if empty.
     * @param entity The entity dismounting.
     * @return {@code true} if dismounting was successful, {@code false} otherwise.
     */
    @Override
    public boolean dismountEntity(Entity entity) {
        // Removed call to restorePlayerXP
        
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
                .setDuration(DISMOUNT_RESISTANCE_DURATION_TICKS)
                .setAmplifier(DISMOUNT_RESISTANCE_AMPLIFIER));
        }

        this.dismountStart = System.currentTimeMillis();
        this.shouldDespawn = true;

        isTeleporting = false;
        return true;
    }
    
    /**
     * Called every tick to update the entity's state, movement, passengers, and effects.
     * Handles the despawn timer when empty.
     * @param currentTick The current server tick.
     * @return {@code true} if the update was successful, {@code false} if the entity should be closed.
     */
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

            // --- Simulate Flight Physics ---
            
            // 1. Adjust current speed towards target speed (Acceleration/Deceleration & Drag)
            if (this.currentFlightSpeed < this.targetSpeed) {
                this.currentFlightSpeed += ACCELERATION_RATE * (this.targetSpeed - this.currentFlightSpeed);
                this.currentFlightSpeed = Math.min(this.currentFlightSpeed, this.targetSpeed); // Clamp to target
            } else if (this.currentFlightSpeed > this.targetSpeed) {
                // Slightly faster deceleration towards 0
                this.currentFlightSpeed -= ACCELERATION_RATE * (this.currentFlightSpeed - this.targetSpeed) * 1.5; 
                this.currentFlightSpeed = Math.max(this.currentFlightSpeed, this.targetSpeed); // Clamp to target
            }
            // Apply Drag (Natural Deceleration)
            this.currentFlightSpeed -= this.currentFlightSpeed * DRAG_COEFFICIENT;
            if (this.currentFlightSpeed < 0.01) {
                 this.currentFlightSpeed = 0;
            }

            // 2. Calculate Desired Horizontal Motion Vector based on Input & Yaw
            double yawRad = Math.toRadians(this.yaw); // Use current yaw
            // Dragon's local forward vector (where its nose points along Z axis relative to yaw)
            double forwardX_dragon = -Math.sin(yawRad);
            double forwardZ_dragon = Math.cos(yawRad);
            // Dragon's local right vector (where its right wing points along X axis relative to yaw)
            double rightX_dragon = -Math.sin(yawRad + Math.PI / 2.0);
            double rightZ_dragon = Math.cos(yawRad + Math.PI / 2.0);

            // Combine player input with dragon's local axes:
            // Player forward input (inputY) is negative for forward, so multiply by -1
            // Player strafe input (inputX) is positive for right, which matches the right vector
            double combinedX = forwardX_dragon * (-this.lastForwardInput) + rightX_dragon * this.lastStrafeInput;
            double combinedZ = forwardZ_dragon * (-this.lastForwardInput) + rightZ_dragon * this.lastStrafeInput;

            double horizontalInputMagnitude = Math.sqrt(combinedX * combinedX + combinedZ * combinedZ);
            double normalizedX = 0;
            double normalizedZ = 0;

            if (horizontalInputMagnitude > 1.0E-4) {
                // If there's input, normalize the combined direction
                 normalizedX = combinedX / horizontalInputMagnitude;
                 normalizedZ = combinedZ / horizontalInputMagnitude;
            } else if (currentFlightSpeed > 0) {
                 // If no input but still moving, use the dragon's facing direction for deceleration
                 // This prevents snapping direction during slowdown.
                 double facingYawRad = Math.toRadians(this.yaw + 180); // Use the corrected facing direction
                 normalizedX = -Math.sin(facingYawRad);
                 normalizedZ = Math.cos(facingYawRad);
            }
            // If horizontalInputMagnitude is near zero AND currentFlightSpeed is near zero, normalized vectors remain 0.

            // 3. Apply Horizontal Speed based on calculated direction and current flight speed
            this.motionX = normalizedX * this.currentFlightSpeed;
            this.motionZ = normalizedZ * this.currentFlightSpeed;
            
            // 4. Calculate Initial Vertical Motion based on Pitch & Speed
            double pitchRad = Math.toRadians(this.pitch);
            this.motionY = -Math.sin(pitchRad) * this.currentFlightSpeed;
            
            // 5. Apply Vertical Physics Adjustments (Gravity, Lift, Dive) to the initial motionY
            this.motionY -= GRAVITY_ACCEL; // Apply gravity

            double horizontalSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ); // Use actual horizontal motion magnitude
            
            if (horizontalSpeed > MIN_LIFT_SPEED && this.pitch < 0) { // Lift generation
                // Lift opposes gravity, scaled by horizontal speed and how much looking up (cos(pitch)) 
                double liftForce = horizontalSpeed * LIFT_COEFFICIENT * Math.cos(pitchRad); 
                this.motionY += liftForce;
            }
            
            if (this.pitch > 20) { // Dive bonus acceleration
                 double diveBonus = Math.sin(Math.toRadians(this.pitch - 20)) * GRAVITY_ACCEL * DIVE_ACCEL_MULTIPLIER;
                 this.motionY -= diveBonus; 
                 // --- Momentum Boost during Dive --- 
                 // Increase current speed slightly when diving steeply
                 double diveMomentumFactor = Math.min(0.1, Math.abs(Math.sin(pitchRad)) * 0.15); 
                 this.currentFlightSpeed += diveMomentumFactor;
            }
            
            // 6. Clamp final vertical speed
            this.motionY = Math.max(MAX_VERTICAL_SPEED_DOWN, Math.min(MAX_VERTICAL_SPEED_UP, this.motionY));
            // --- End Flight Physics Simulation ---

            // 7. Apply final calculated motion if moving or has vertical motion
            if (this.motionX != 0 || this.motionY != 0 || this.motionZ != 0 || this.currentFlightSpeed > 0) {
                 this.move(this.motionX, this.motionY, this.motionZ); 
                 this.updateMovement(); // Update movement on the server
                 this.updatePassengers(); // <--- Explicitly update passenger positions AFTER dragon moves
            } else {
                 // Ensure motion is truly zero if speed is zero and no other forces are acting
                 this.motionX = 0;
                 // Let motionY decay naturally due to gravity/drag unless needed to clamp
                 this.motionZ = 0;
            }
            
            // Action Bar Display Logic - Update every tick now
            if (!this.passengers.isEmpty() && this.passengers.get(0) instanceof Player) {
                Player rider = (Player) this.passengers.get(0);
                sendRidingActionBar(rider);
            }
            
        } else {
            // No passengers, become immobile and stop motion
            this.setImmobile(true);
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            this.currentFlightSpeed = 0; // Reset speed when dismounted
            this.targetSpeed = 0;        // Reset target speed
            this.lastForwardInput = 0;   // Reset input state
            this.lastStrafeInput = 0;
        }

        // Despawn timer logic
        if (this.passengers.isEmpty() && shouldDespawn) {
            long elapsed = System.currentTimeMillis() - dismountStart;
            if (elapsed >= EMPTY_DESPAWN_DELAY_MILLIS) {
                this.close();
                return false;
            }
        }

        // Standard entity update and particle effects
        if (super.onUpdate(currentTick)) {
            if (currentTick % PARTICLE_EFFECT_INTERVAL_TICKS == 0 && !this.getViewers().isEmpty()) {
                applyCustomizations();
            }
            return true;
        }
        return false;
    }

    /**
     * Handles player input while riding the dragon to control movement and direction.
     * Uses pitch and yaw directly from the PlayerAuthInputPacket to avoid potential state issues.
     * 
     * @param player The player providing input.
     * @param strafe Strafe input (-1.0 to 1.0).
     * @param forward Forward input (-1.0 to 1.0).
     * @param packetPitch Pitch value from the client packet.
     * @param packetYaw Yaw value from the client packet.
     */
    public void onPlayerInput(Player player, double strafe, double forward, float packetPitch, float packetYaw) {
        this.stayTime = 0;
        this.moveTime = 20;
        this.route = null;
        this.target = null;
    
        double playerYaw = (packetYaw + 180) % 360;
        double playerPitch = packetPitch;

        // Set the dragon's visual rotation first
        this.setRotation(playerYaw, playerPitch);

        // Store the raw input values directly
        this.lastStrafeInput = strafe; // Raw inputX (-1 left, +1 right)
        this.lastForwardInput = forward; // Raw inputY (-1 forward, +1 backward)

        // --- Determine Target Speed based on Input and Flight Angle ---
        double horizontalInputMagnitude = Math.sqrt(this.lastStrafeInput * this.lastStrafeInput + this.lastForwardInput * this.lastForwardInput);
        boolean hasHorizontalInput = horizontalInputMagnitude >= 1.0E-4;

        // Calculate adjusted max speed based on pitch
        this.currentMaxSpeed = MAX_SPEED; // Reset to base max speed
        if (Math.abs(playerPitch) < 15) { // Check if pitch is relatively level
            this.currentMaxSpeed *= FLAT_FLIGHT_SPEED_MULTIPLIER;
        }

        if (hasHorizontalInput) {
            // Player wants to move, set target speed towards current max
            this.targetSpeed = this.currentMaxSpeed;
        } else {
            // Player has no input, target speed is 0
            this.targetSpeed = 0;
        }

        // NOTE: Actual motion calculation (acceleration, drag, physics) is now handled in onUpdate
        // We only set the rotation, store inputs, and target speed here based on immediate input.
    }

    /**
     * Creates the AddEntityPacket specific to this dragon entity.
     * @return The AddEntityPacket.
     */
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
        
        // Include minimal health attributes to keep entity alive
        addEntity.attributes = new Attribute[]{
            Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(10f).setValue(10f),
            Attribute.getAttribute(Attribute.MOVEMENT_SPEED).setValue(this.moveSpeed)
        };

        addEntity.links = new EntityLink[this.passengers.size()];

        for(int i = 0; i < addEntity.links.length; ++i) {
           addEntity.links[i] = new EntityLink(this.id, ((Entity)this.passengers.get(i)).getId(), (byte)(i == 0 ? 1 : 2), false, false, 0.0F);
        }

        return addEntity;
    }

    /**
     * Gets the width of the entity's bounding box.
     * @return The width.
     */
    @Override
    public float getWidth() {
        return 4.0f;
    }

    /**
     * Gets the height of the entity's bounding box.
     * @return The height.
     */
    @Override
    public float getHeight() {
        return 4.0f;
    }

    /**
     * Gets the length of the entity's bounding box.
     * @return The length.
     */
    @Override
    public float getLength() {
        return 6.0f;
    }
   
    /**
     * Gets the display name of the entity.
     * @return The custom name tag if set, otherwise "Dragon".
     */
    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Dragon";
    }

    /**
     * Handles the entity being attacked. Applies damage reduction based on type and source.
     * Keeps visual damage feedback while managing health through the plugin system.
     * @param source The damage event source.
     * @return {@code true} if the attack was successful (damage applied), {@code false} otherwise.
     */
    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.closed || this.pluginHealth <= 0) { // Check if already dead/closing
            return false;
        }

        if (source.getCause() == EntityDamageEvent.DamageCause.VOID) {
            // Void damage should still kill instantly
            this.pluginHealth = 0;
            this.handleDeath(); 
            return true;
        }

        // Apply damage reduction based on dragon type to our plugin health
        float damage = source.getDamage();
        boolean isImmune = false;
        switch (this.dragonType) {
            case "Fire Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                    source.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    isImmune = true;
                } else {
                    damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                }
                break;
            case "Ice Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                    isImmune = true;
                } else if (source.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                           source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                           source.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    damage *= plugin.getConfig().getDouble("dragon_stats.ice_dragon_fire_vulnerability", 1.5);
                } else {
                    damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                }
                break;
            case "Lightning Dragon":
                if (source.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
                    isImmune = true;
                } else {
                     damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                }
                break;
             default:
                 damage *= (1.0 - DEFAULT_DAMAGE_REDUCTION);
                 break;
        }

        if (isImmune) {
            source.setCancelled(true);
            return false; // Damage type ignored
        }

        // --- Apply damage to plugin health --- 
        this.pluginHealth -= damage;
        // --- End Apply damage --- 

        // --- Trigger custom death logic if plugin health is zero or less --- 
        if (this.pluginHealth <= 0) {
             this.pluginHealth = 0; // Ensure health doesn't go negative visually
             // Allow the normal damage to occur for visual feedback, but handle death our way
             super.attack(source); 
             handleDeath();
             return true; // Attack resulted in death
        }
        
        // Let vanilla damage processing occur for hit effects, but cap it to never kill
        // the entity (by ensuring it always has at least 2 health after damage)
        float currentHealth = this.getHealth();
        if (currentHealth - source.getFinalDamage() < 2) {
            source.setDamage(Math.max(0, currentHealth - 2));
        }
        
        // Actually apply the damage for visual effect
        super.attack(source);
        
        // Make sure the entity doesn't die from this damage
        if (this.getHealth() < 2) {
            this.setHealth(2);
        }
        
        // Force action bar update for rider to see new health immediately
        if (!this.passengers.isEmpty() && this.passengers.get(0) instanceof Player) {
            Player rider = (Player) this.passengers.get(0);
            sendRidingActionBar(rider);
        }
        
        return true; // Attack was processed (damage applied to plugin health)
    }

    private void handleDeath() {
        plugin.getLogger().info("Dragon " + this.getName() + " owned by " + (owner != null ? owner.getName() : "N/A") + " is dying.");

        // Get information about what killed the dragon
        String killerName = "Unknown";
        String location = this.level.getName() + " (" + (int)this.x + "," + (int)this.y + "," + (int)this.z + ")";
        
        // Try to determine the killer from the last damage cause if available
        EntityDamageEvent lastDamage = this.getLastDamageCause();
        if (lastDamage != null) {
            if (lastDamage instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
                if (damager instanceof Player) {
                    killerName = ((Player) damager).getName();
                } else {
                    killerName = damager.getName();
                }
            } else {
                // Get the damage cause name (e.g., VOID, FIRE, etc.)
                killerName = lastDamage.getCause().name();
            }
        }
        
        // Mark dragon as dead in the database
        String eggId = this.getDragonId();
        if (eggId != null && plugin.getDatabaseManager() != null) {
            boolean marked = plugin.getDatabaseManager().markDragonAsDead(eggId, killerName, location);
            if (marked) {
                plugin.getLogger().info("Marked dragon " + eggId + " as dead in the database");
            } else {
                plugin.getLogger().warning("Failed to mark dragon " + eggId + " as dead in the database");
            }
        }

        // Dismount all passengers
        dismountAllPassengers();

        // Notify owner and start cooldown (this also handles removing from activeDragons map)
        if (owner != null) {
            plugin.onDragonDeath(owner);
        }

        // We now mark the dragon as dead in the database instead of removing it
        // This replaces the original database removal code

        // Drop items or create effects
        if (this.level != null) {
            // Add death particles
            for (int i = 0; i < DEATH_PARTICLE_COUNT; i++) {
                this.level.addParticle(new cn.nukkit.level.particle.ExplodeParticle(this.add(
                    Math.random() * 2 - 1,
                    Math.random() * 2,
                    Math.random() * 2 - 1
                )));
            }

            // Play death sound
            this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_EXPLODE);
        }

        // Force close the entity to fully remove it from the world
        this.close();
        
        plugin.getLogger().info("Dragon " + this.getName() + " death handling complete.");
    }

    /**
     * Overrides the default kill method to ensure death handling logic is executed.
     */
    @Override
    public void kill() {
        handleDeath();
        super.kill();
    }

    /**
     * Dismounts all current passengers from this dragon.
     */
    public void dismountAllPassengers() {
        for (Entity passenger : new ArrayList<>(this.passengers)) {
            dismountEntity(passenger);
        }
    }

    /**
     * Updates the position of all passengers to match the dragon's position.
     */
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

    /**
     * Gets the amount of experience dropped when this entity is killed.
     * Dragons do not drop experience.
     * @return Always 0.
     */
    @Override
    public int getKillExperience() {
        return 0;
    }

    /**
     * Sets the owner of this dragon.
     * @param owner The player who owns this dragon.
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * Gets the owner of this dragon.
     * @return The owning player, or null if none.
     */
    public Player getOwner() {
        return this.owner;
    }

    /**
     * Gets the unique ID associated with this dragon (usually the egg ID).
     * @return The dragon ID string.
     */
    public String getDragonId() {
        return this.dragonId;
    }

    /**
     * Sets the unique ID for this dragon.
     * @param dragonId The dragon ID string.
     */
    public void setDragonId(String dragonId) {
        this.dragonId = dragonId;
    }

    /**
     * Gets the text displayed on the interact button (e.g., "Mount").
     * @return The interaction text key.
     */
    @Override
    public String getInteractButtonText() {
        return "action.interact.mount";
    }

    /**
     * Checks if interaction (mounting) is currently possible.
     * @return {@code true} if there are no passengers, {@code false} otherwise.
     */
    @Override
    public boolean canDoInteraction() {
        return passengers.isEmpty();
    }

    /**
     * Initiates the dragon's shooting ability based on its type,
     * checking cooldowns and consuming the required item from the rider (owner).
     */
    public void shoot() {
        // Check cooldown
        int cooldownMillis = plugin.getConfig().getInt("timing.cooldowns.ability_milliseconds", 500);
        long currentTime = System.currentTimeMillis(); // Get current time once
        long timeSinceLastShot = currentTime - lastFireballTime;

        // --- DEBUG LOGGING ---
        plugin.getLogger().info("Shoot attempt: currentTime=" + currentTime + 
                               ", lastFireballTime=" + lastFireballTime + 
                               ", timeSinceLastShot=" + timeSinceLastShot + 
                               ", cooldownMillis=" + cooldownMillis);
        // --- END DEBUG LOGGING ---

        if (timeSinceLastShot < cooldownMillis) {
            plugin.getLogger().info("Shoot blocked: Cooldown active."); // Debug log
            
            // Play note sound to the rider as feedback
            if (!this.passengers.isEmpty() && this.passengers.get(0) instanceof Player) {
                Player rider = (Player) this.passengers.get(0);
                // Play a low bass note sound
                rider.getLevel().addLevelSoundEvent(rider, LevelSoundEventPacket.SOUND_NOTE, 0); // Data 0 for bass
            }
            
            return; // Still on cooldown
        }

        // Check if rider is owner
        if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof Player) || !this.passengers.get(0).equals(this.owner)) {
             return; // Only owner can shoot while riding
        }
        Player rider = (Player) this.passengers.get(0);

        // Get required item and shoot action based on dragon type
        Item requiredItem;
        Runnable shootAction;
        String missingItemMessageKey;
        
        switch (this.dragonType) {
            case "Fire Dragon":
                requiredItem = Item.get(Item.FIRE_CHARGE); 
                shootAction = this::shootFireball;
                missingItemMessageKey = "messages.errors.missingShootItem";
                break;
            case "Ice Dragon":
                requiredItem = Item.get(Item.SNOWBALL); // Default item
                shootAction = this::shootIceball;
                missingItemMessageKey = "messages.errors.missingShootItem"; // Use generic message
                break;
            case "Lightning Dragon":
                requiredItem = Item.get(Item.GLOWSTONE_DUST); // Default item
                shootAction = this::shootLightningBall;
                missingItemMessageKey = "messages.errors.missingShootItem"; // Use generic message
                break;
            case "Water Dragon":
                requiredItem = Item.get(Item.PRISMARINE_CRYSTALS); // Water-themed item
                shootAction = this::shootWaterball;
                missingItemMessageKey = "messages.errors.missingShootItem"; // Use generic message
                break;
            case "Earth Dragon":
                requiredItem = Item.get(Item.CLAY_BALL); // Earth-themed item
                shootAction = this::shootEarthball;
                missingItemMessageKey = "messages.errors.missingShootItem"; // Use generic message
                break;
            default:
                 plugin.getLogger().warning("Attempted to shoot with unknown dragon type: " + this.dragonType);
                 // Default to fire dragon behavior for safety
                 requiredItem = Item.get(Item.FIRE_CHARGE);
                 shootAction = this::shootFireball;
                 missingItemMessageKey = "messages.errors.missingShootItem";
                 break;
        }

        // Check and consume the required item
        if (consumeRequiredItem(rider, requiredItem.getId())) {
            plugin.getLogger().info("Item consumed. Updating lastFireballTime and shooting."); // Debug log
            // Update cooldown time *before* shooting
            lastFireballTime = currentTime;
            // Execute the appropriate shoot method
            shootAction.run();
        } else {
             plugin.getLogger().info("Shoot failed: Required item not found."); // Debug log
            // Send message if item not found (using the specific item name)
            rider.sendMessage(TextFormat.RED + plugin.getLanguageString(missingItemMessageKey, requiredItem.getName()));
        }
    }

    /**
     * Consumes one of the required items from the player's inventory.
     * @param player The player whose inventory to check.
     * @param requiredItemId The ID of the item to consume.
     * @return true if the item was found and consumed, false otherwise.
     */
    private boolean consumeRequiredItem(Player player, int requiredItemId) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item item = player.getInventory().getItem(slot);
            if (item.getId() == requiredItemId) {
                if (item.getCount() > 1) {
                    item.setCount(item.getCount() - 1);
                    player.getInventory().setItem(slot, item);
                } else {
                    player.getInventory().setItem(slot, Item.get(Item.AIR));
                }
                player.getInventory().sendContents(player); // Update client inventory
                return true;
            }
        }
        return false; // Item not found
    }

    // Removed restorePlayerXP method

    /**
     * Overrides the close method to ensure passengers are dismounted first.
     */
    @Override
    public void close() {
        // Removed XP restoration logic before closing
        super.close();
    }

    /**
     * Saves custom NBT data for the entity, including type, particle effect, and color.
     */
    @Override
    public void saveNBT() {
        super.saveNBT();
        
        // Save customization to NBT
        this.namedTag.putString(NBT_KEY_DRAGON_TYPE, this.dragonType);
        this.namedTag.putString(NBT_KEY_PARTICLE_EFFECT, this.particleEffect);
        this.namedTag.putString(NBT_KEY_DRAGON_COLOR, this.dragonColor);
    }

    /**
     * Sets the type of this dragon (e.g., "Fire Dragon").
     * Applies type-specific properties like fire immunity.
     * @param type The dragon type string.
     */
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
            case "Water Dragon":
                this.fireProof = false;
                // Water dragons are resistant to drowning
                break;
            case "Earth Dragon":
                this.fireProof = false;
                // Earth dragons are resistant to fall damage
                break;
            default:
                this.fireProof = true; // Default behavior
                break;
        }
    }

    /**
     * Gets the type of this dragon.
     * @return The dragon type string.
     */
    public String getDragonType() {
        return this.dragonType;
    }

    /**
     * Sets the particle effect customization for this dragon.
     * @param effect The name of the particle effect.
     */
    public void setParticleEffect(String effect) {
        this.particleEffect = effect;
    }

    /**
     * Gets the particle effect customization for this dragon.
     * @return The particle effect name.
     */
    public String getParticleEffect() {
        return this.particleEffect;
    }

    /**
     * Sets the color customization for this dragon.
     * Note: Actual visual change requires texture/model adjustments.
     * @param color The color name.
     */
    public void setDragonColor(String color) {
        this.dragonColor = color;
        // Apply color changes
        // This would typically involve changing the dragon's texture or particle colors
    }

    /**
     * Gets the color customization for this dragon.
     * @return The color name.
     */
    public String getDragonColor() {
        return this.dragonColor;
    }

    // --- Plugin Health Getters ---
    public float getPluginHealth() {
        return pluginHealth;
    }

    public float getPluginMaxHealth() {
        return pluginMaxHealth;
    }
    // --- End Plugin Health Getters ---

    private static final double PARTICLE_RENDER_DISTANCE_SQUARED = 32.0 * 32.0; // Use squared distance for efficiency

    private void applyCustomizations() {
        if (this.level == null || this.getViewers().isEmpty()) {
            return; // No level or no viewers, skip particles
        }

        // Apply particle effects based on type and customization, only for nearby players
        if (this.dragonType == null) {
            this.dragonType = "Fire Dragon"; // Default to Fire Dragon if null
        }
        if (this.particleEffect == null) {
            this.particleEffect = "Flame Trail"; // Default particle effect
        }

        Vector3 particlePos = this.add(0, 1.2, 0);
        Vector3 particlePosWings = this.add(0, 1.5, 0);

        for (Player player : this.getViewers().values()) {
            if (player.distanceSquared(this) <= PARTICLE_RENDER_DISTANCE_SQUARED) {
                switch (this.dragonType) {
                    case "Fire Dragon":
                        // Multiple flame particles for fire dragons
                        for (int i = 0; i < 3; i++) {
                            Vector3 randPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            this.level.addParticle(new cn.nukkit.level.particle.FlameParticle(randPos), player);
                        }
                        
                        // Add smoke particles too
                        if (Math.random() < 0.3) {
                            this.level.addParticle(new cn.nukkit.level.particle.SmokeParticle(
                                particlePos.add((Math.random() - 0.5), 0.2, (Math.random() - 0.5))
                            ), player);
                        }
                        break;
                        
                    case "Ice Dragon":
                        // Custom ice particles for ice dragons
                        for (int i = 0; i < 3; i++) {
                            Vector3 randPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            // Use water drip particles instead for ice effects
                            this.level.addParticle(new cn.nukkit.level.particle.WaterDripParticle(randPos), player);
                        }
                        
                        // Add extra particles on wings
                        for (int i = 0; i < 2; i++) {
                            Vector3 wingPos = particlePosWings.add(
                                (Math.random() - 0.5) * 3, // wider spread for wings
                                Math.random() * 0.2,
                                (Math.random() - 0.5) * 3
                            );
                            this.level.addParticle(new cn.nukkit.level.particle.WaterDripParticle(wingPos), player);
                        }
                        break;
                        
                    case "Lightning Dragon":
                        // Electric spark particles for lightning dragons
                        for (int i = 0; i < 3; i++) {
                            Vector3 randPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            this.level.addParticle(new ElectricSparkParticle(randPos), player);
                        }
                        
                        // Occasional larger spark burst
                        if (Math.random() < 0.1) {
                            for (int i = 0; i < 8; i++) {
                                Vector3 burstPos = particlePos.add(
                                    (Math.random() - 0.5) * 3,
                                    Math.random() * 1.0,
                                    (Math.random() - 0.5) * 3
                                );
                                this.level.addParticle(new ElectricSparkParticle(burstPos), player);
                            }
                        }
                        break;
                        
                    case "Water Dragon":
                        // Water bubble particles for water dragons
                        for (int i = 0; i < 3; i++) {
                            Vector3 randPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            this.level.addParticle(new cn.nukkit.level.particle.BubbleParticle(randPos), player);
                        }
                        
                        // Add splashing water particles occasionally
                        if (Math.random() < 0.2) {
                            Vector3 splashPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            this.level.addParticle(new cn.nukkit.level.particle.SplashParticle(splashPos), player);
                        }
                        break;
                        
                    case "Earth Dragon":
                        // Add earthy particles for earth dragons
                        for (int i = 0; i < 3; i++) {
                            Vector3 randPos = particlePos.add(
                                (Math.random() - 0.5) * 2,
                                Math.random() * 0.5,
                                (Math.random() - 0.5) * 2
                            );
                            // Use terrain particles for earth effect
                            if (Math.random() < 0.5) {
                                this.level.addParticle(new cn.nukkit.level.particle.DustParticle(randPos, 139, 69, 19), player);
                            } else {
                                this.level.addParticle(new cn.nukkit.level.particle.DustParticle(randPos, 100, 100, 100), player);
                            }
                        }
                        break;
                        
                    default:
                        // Default to fire particles
                        this.level.addParticle(new cn.nukkit.level.particle.FlameParticle(particlePos), player);
                        break;
                }
            }
        }
    }

    /**
     * Sets the DragonShardManager instance for this entity.
     * Called during initialization.
     * @param manager The DragonShardManager instance.
     */
    public void setShardManager(com.youssgm3o8.rokidragon.manager.DragonShardManager manager) {
        this.shardManager = manager;
    }

    /**
     * Overrides setHealth to also update the entity data property for client sync.
     * @param health The new health value.
     */
    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        this.setDataProperty(new FloatEntityData(DATA_HEALTH, health));
    }

    /**
     * Heals the dragon by the specified amount, capped at pluginMaxHealth.
     * @param amount The amount to heal.
     */
    public void healPluginHealth(float amount) {
        if (amount <= 0) return;
        this.pluginHealth = Math.min(this.pluginMaxHealth, this.pluginHealth + amount);
    }

    private void shootFireball() {
        // Calculate projectile spawn position with adjusted yaw (180 degrees)
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        
        // Calculate direction vector with 180 degree yaw adjustment
        double adjustedYaw = this.yaw + 180;
        double yawRadians = Math.toRadians(adjustedYaw);
        double pitchRadians = Math.toRadians(this.pitch);
        
        // Calculate direction vector with adjusted yaw
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vector3 directionVector = new Vector3(x, y, z);
        
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Get cooldown from config
        int cooldown = plugin.getConfig().getInt("timing.cooldowns.ability_milliseconds", 500);
        long now = System.currentTimeMillis();

        // Check if rider is player
        if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof Player)) {
            return;
        }
        Player rider = (Player) this.passengers.get(0);

        // Create and spawn fireball
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        nbt.putLong("DragonID", this.getId());
        EntityBedFireBall fireball = new EntityBedFireBall(plugin, this.getChunk(), nbt, this);
        fireball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        fireball.spawnToAll();
        
        // Add particle trails
        Server.getInstance().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (fireball.isClosed()) {
                return; // Stop if fireball is gone
            }
            
            // Create spiral trails with different radii and speeds
            double time = (System.currentTimeMillis() - now) / 100.0; // Time factor for spiral
            for (int i = 0; i < 3; i++) {
                double radius = 0.3 + (i * 0.2); // Different radius for each spiral
                double speed = 2.0 + (i * 0.5); // Different speed for each spiral
                
                // Calculate spiral positions
                double spiralX = Math.cos(time * speed) * radius;
                double spiralY = Math.sin(time * speed) * radius;
                double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                
                Vector3 particlePos = fireball.add(spiralX, spiralY, spiralZ);
                
                // Add randomized flame and smoke particles
                if (Math.random() < 0.7) { // 70% chance for flame
                    fireball.level.addParticle(new cn.nukkit.level.particle.FlameParticle(particlePos));
                }
                if (Math.random() < 0.3) { // 30% chance for smoke
                    fireball.level.addParticle(new cn.nukkit.level.particle.SmokeParticle(particlePos));
                }
            }
        }, 1, false);

        // Play sound effects
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_IMITATE_GHAST);
        
        // Update cooldown time
        lastFireballTime = now;
    }

    private void shootIceball() {
        // Calculate projectile spawn position with adjusted yaw (180 degrees)
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        
        // Calculate direction vector with 180 degree yaw adjustment
        double adjustedYaw = this.yaw + 180;
        double yawRadians = Math.toRadians(adjustedYaw);
        double pitchRadians = Math.toRadians(this.pitch);
        
        // Calculate direction vector with adjusted yaw
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vector3 directionVector = new Vector3(x, y, z);
        
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Create and spawn iceball
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        nbt.putLong("DragonID", this.getId());
        EntityIceBall iceball = new EntityIceBall(this.getChunk(), nbt, this); 
        iceball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        iceball.spawnToAll();
        
        // Add ice particle effects
        long now = System.currentTimeMillis();
        Server.getInstance().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (iceball.isClosed()) {
                return; // Stop if iceball is gone
            }
            
            // Create ice particle trails
            double time = (System.currentTimeMillis() - now) / 100.0;
            for (int i = 0; i < 3; i++) {
                double radius = 0.3 + (i * 0.2);
                double speed = 2.0 + (i * 0.5);
                
                double spiralX = Math.cos(time * speed) * radius;
                double spiralY = Math.sin(time * speed) * radius;
                double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                
                Vector3 particlePos = iceball.add(spiralX, spiralY, spiralZ);
                
                // Add ice/snow particles
                if (Math.random() < 0.8) {
                    iceball.level.addParticleEffect(particlePos, ParticleEffect.FALLING_DUST_TOP_SNOW, 0);
                }
            }
        }, 1, false);
        
        // Play appropriate sound effect
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_GLASS);
    }

    private void shootLightningBall() {
        // Calculate projectile spawn position with adjusted yaw (180 degrees)
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        
        // Calculate direction vector with 180 degree yaw adjustment
        double adjustedYaw = this.yaw + 180;
        double yawRadians = Math.toRadians(adjustedYaw);
        double pitchRadians = Math.toRadians(this.pitch);
        
        // Calculate direction vector with adjusted yaw
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vector3 directionVector = new Vector3(x, y, z);
        
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Create and spawn lightning ball - pass plugin instance
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        nbt.putLong("DragonID", this.getId());
        EntityLightningBall lightningBall = new EntityLightningBall(plugin, this.getChunk(), nbt, this); 
        lightningBall.setMotion(directionVector.multiply(FIREBALL_SPEED));
        lightningBall.spawnToAll();
        
        // Add lightning particle effects
        long now = System.currentTimeMillis();
        Server.getInstance().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (lightningBall.isClosed()) {
                return; // Stop if lightning ball is gone
            }
            
            // Create lightning particle trails
            double time = (System.currentTimeMillis() - now) / 100.0;
            for (int i = 0; i < 3; i++) {
                double radius = 0.3 + (i * 0.2);
                double speed = 2.0 + (i * 0.5);
                
                double spiralX = Math.cos(time * speed) * radius;
                double spiralY = Math.sin(time * speed) * radius;
                double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                
                Vector3 particlePos = lightningBall.add(spiralX, spiralY, spiralZ);
                
                // Add electric spark particles
                if (Math.random() < 0.8) {
                    lightningBall.level.addParticle(new ElectricSparkParticle(particlePos));
                }
            }
        }, 1, false);
        
        // Play appropriate sound effect
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_THUNDER);
    }

    private void shootWaterball() {
        // Calculate projectile spawn position with adjusted yaw (180 degrees)
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        
        // Calculate direction vector with 180 degree yaw adjustment
        double adjustedYaw = this.yaw + 180;
        double yawRadians = Math.toRadians(adjustedYaw);
        double pitchRadians = Math.toRadians(this.pitch);
        
        // Calculate direction vector with adjusted yaw
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vector3 directionVector = new Vector3(x, y, z);
        
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Get cooldown from config
        int cooldown = plugin.getConfig().getInt("timing.cooldowns.ability_milliseconds", 500);
        long now = System.currentTimeMillis();

        // Check if rider is player
        if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof Player)) {
            return;
        }
        Player rider = (Player) this.passengers.get(0);

        // Create a waterball using IceBall for now, since waterball doesn't exist
        // In a complete implementation, you'd create a new EntityWaterBall class
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        nbt.putLong("DragonID", this.getId());
        EntityIceBall waterball = new EntityIceBall(this.getChunk(), nbt, this);
        waterball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        waterball.spawnToAll();
        
        // Add water particle effects
        Server.getInstance().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (waterball.isClosed()) {
                return; // Stop if waterball is gone
            }
            
            // Create water particle trails
            double time = (System.currentTimeMillis() - now) / 100.0;
            for (int i = 0; i < 3; i++) {
                double radius = 0.3 + (i * 0.2);
                double speed = 2.0 + (i * 0.5);
                
                double spiralX = Math.cos(time * speed) * radius;
                double spiralY = Math.sin(time * speed) * radius;
                double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                
                Vector3 particlePos = waterball.add(spiralX, spiralY, spiralZ);
                
                // Add water drip and bubble particles
                if (Math.random() < 0.7) {
                    waterball.level.addParticle(new cn.nukkit.level.particle.WaterDripParticle(particlePos));
                }
                if (Math.random() < 0.3) {
                    waterball.level.addParticle(new cn.nukkit.level.particle.BubbleParticle(particlePos));
                }
            }
        }, 1, false);
        
        // Play appropriate sound effect
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_SPLASH);
    }

    private void shootEarthball() {
        // Calculate projectile spawn position with adjusted yaw (180 degrees)
        Vector3 pos = this.add(0, this.getEyeHeight(), 0);
        
        // Calculate direction vector with 180 degree yaw adjustment
        double adjustedYaw = this.yaw + 180;
        double yawRadians = Math.toRadians(adjustedYaw);
        double pitchRadians = Math.toRadians(this.pitch);
        
        // Calculate direction vector with adjusted yaw
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vector3 directionVector = new Vector3(x, y, z);
        
        Vector3 spawnPos = pos.add(directionVector.multiply(FIREBALL_OFFSET));

        // Get cooldown from config
        int cooldown = plugin.getConfig().getInt("timing.cooldowns.ability_milliseconds", 500);
        long now = System.currentTimeMillis();

        // Check if rider is player
        if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof Player)) {
            return;
        }
        Player rider = (Player) this.passengers.get(0);

        // Create an earthball using standard fireball for now
        // In a complete implementation, you'd create a new EntityEarthBall class
        CompoundTag nbt = Entity.getDefaultNBT(spawnPos);
        nbt.putLong("DragonID", this.getId());
        EntityBedFireBall earthball = new EntityBedFireBall(plugin, this.getChunk(), nbt, this);
        earthball.setMotion(directionVector.multiply(FIREBALL_SPEED));
        earthball.spawnToAll();
        
        // Add earth particle effects
        Server.getInstance().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (earthball.isClosed()) {
                return; // Stop if earthball is gone
            }
            
            // Create spiral trails
            double time = (System.currentTimeMillis() - now) / 100.0;
            for (int i = 0; i < 3; i++) {
                double radius = 0.3 + (i * 0.2);
                double speed = 2.0 + (i * 0.5);
                
                double spiralX = Math.cos(time * speed) * radius;
                double spiralY = Math.sin(time * speed) * radius;
                double spiralZ = Math.cos(time * speed + Math.PI/2) * radius;
                
                Vector3 particlePos = earthball.add(spiralX, spiralY, spiralZ);
                
                // Add dust particles with earth colors
                if (Math.random() < 0.5) {
                    // Brown dust (dirt color)
                    earthball.level.addParticle(new cn.nukkit.level.particle.DustParticle(particlePos, 139, 69, 19));
                } else {
                    // Gray dust (stone color)
                    earthball.level.addParticle(new cn.nukkit.level.particle.DustParticle(particlePos, 100, 100, 100));
                }
            }
        }, 1, false);
        
        // Play appropriate sound effect
        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_BREAK);
    }

    // Placeholder methods for special abilities
    private void applyIceDragonEffects(Vector3 position) {
        // Placeholder for potential future ice dragon effects (e.g., freeze water, slow entities)
        // Example: Add snowflake particles
        if (this.level != null && !this.getViewers().isEmpty()) {
            for (Player viewer : this.getViewers().values()) {
                if (viewer.distanceSquared(this) <= PARTICLE_RENDER_DISTANCE_SQUARED) {
                    this.level.addParticleEffect(position, ParticleEffect.SNOWFLAKE, 0, 5, viewer);
                }
            }
        }
    }

    private void applyLightningDragonEffects(Vector3 position) {
        // Placeholder for potential future lightning dragon effects (e.g., chain lightning, thunder sounds)
        // Example: Add electric spark particles
        if (this.level != null && !this.getViewers().isEmpty()) {
            for (Player viewer : this.getViewers().values()) {
                if (viewer.distanceSquared(this) <= PARTICLE_RENDER_DISTANCE_SQUARED) {
                    this.level.addParticle(new ElectricSparkParticle(position), viewer);
                }
            }
        }
    }


    /**
     * Sends riding information (Health, Cooldown) to the rider via Action Bar.
     * @param rider The player riding the dragon.
     */
    private void sendRidingActionBar(Player rider) {
        // Health Display - Use pluginHealth and pluginMaxHealth
        float health = this.pluginHealth;
        float maxHealth = this.pluginMaxHealth;
        int healthPercent = (maxHealth > 0) ? (int) ((health / maxHealth) * 100) : 0; // Prevent division by zero
        TextFormat healthColor;
        if (healthPercent > 70) healthColor = TextFormat.GREEN;
        else if (healthPercent > 30) healthColor = TextFormat.YELLOW;
        else healthColor = TextFormat.RED;
        String healthDisplay = healthColor + "HP: " + (int)health + " / " + (int)maxHealth + " (" + healthPercent + "%)";

        // Create a visual cube-based cooldown bar
        long currentTime = System.currentTimeMillis();
        int cooldownMillis = plugin.getConfig().getInt("timing.cooldowns.ability_milliseconds", 500); 
        long remainingMillis = Math.max(0, (lastFireballTime + cooldownMillis) - currentTime);
        
        final int MAX_CUBES = 10; // Number of cube blocks to show
        String cooldownDisplay;
        
        if (remainingMillis > 0) {
            double cooldownFraction = 1.0 - (remainingMillis / (double)cooldownMillis);
            int filledCubes = (int)(cooldownFraction * MAX_CUBES);
            
            StringBuilder cooldownBar = new StringBuilder();
            cooldownBar.append(TextFormat.RED).append(" Ability: ");
            
            // Add filled cubes (green)
            for (int i = 0; i < filledCubes; i++) {
                cooldownBar.append(TextFormat.GREEN).append("■");
            }
            
            // Add empty cubes (gray)
            for (int i = filledCubes; i < MAX_CUBES; i++) {
                cooldownBar.append(TextFormat.GRAY).append("■");
            }
            
            cooldownDisplay = cooldownBar.toString();
        } else {
            // Ready - all green cubes
            StringBuilder cooldownBar = new StringBuilder();
            cooldownBar.append(TextFormat.AQUA).append(" Ability: ");
            for (int i = 0; i < MAX_CUBES; i++) {
                cooldownBar.append(TextFormat.GREEN).append("■");
            }
            cooldownDisplay = cooldownBar.toString();
        }

        // Combine and send
        String actionBarMessage = healthDisplay + TextFormat.GRAY + " |" + cooldownDisplay;
        rider.sendActionBar(actionBarMessage);
    }

    /**
     * Determines if this entity can collide with another entity.
     * Prevents collision with its own projectiles.
     * @param entity The other entity.
     * @return {@code true} if collision should occur, {@code false} otherwise.
     */
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

}
