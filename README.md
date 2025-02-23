# RokiDragonPlugin

RokiDragonPlugin is a Nukkit plugin that introduces customizable rideable dragons to your Minecraft server. Players can purchase, hatch, and summon their own dragons with unique types, abilities, and visual effects.

## Features

- **Custom Dragon Entity**: Fully rideable dragon entity with smooth flying controls
- **Dragon Types**: Three unique dragon types with different abilities:
  - Fire Dragon: Shoots powerful fireballs with fire trails
  - Ice Dragon: Launches ice projectiles with frost effects
  - Lightning Dragon: Unleashes lightning strikes with electric trails
- **Custom Dragon Shards**: 
  - Fire Shard: Used by Fire Dragons
  - Ice Shard: Used by Ice Dragons
  - Lightning Shard: Used by Lightning Dragons
- **Dragon Customization System**: 
  - Change dragon appearance and colors
  - Customize particle effects and trails
  - Name your dragons
- **Dragon Egg Management**: 
  - Purchase and incubate dragon eggs
  - Different incubation conditions determine dragon type
  - Secure ownership verification system
- **Visual Effects**:
  - Customizable particle trails for each dragon type
  - Dynamic projectile effects with configurable trails
  - Unique visual effects for different dragon abilities
- **Admin Controls**: 
  - Force hatch eggs for players
  - Manage dragon ownership
  - Configure all aspects of the plugin

## Installation

1. Download the latest release from the [releases page](https://github.com/YoussGm3o8/roki-dragon/releases)
2. Place the JAR file in your server's `plugins` folder
3. Install the required dependencies (see below)
4. Start your server to generate configuration files
5. Configure the plugin in `plugins/RokiDragon/config.yml`

## Dependencies

Required plugins:
1. MobPlugin: [Download](https://cloudburstmc.org/resources/mobplugin.3/)
2. EconomyAPI: [Download](https://cloudburstmc.org/resources/economyapi.14/)

## Configuration

The `config.yml` file allows extensive customization:

```yaml
# Economy Settings
dragon-egg-price: 128000      # Cost to purchase a dragon egg
dragon-buy-cooldown: 60       # Cooldown between purchases (seconds)

# Dragon Hatching
dragon-hatching-time: 3600    # Time to hatch (seconds)

# Projectile Settings
dragon-projectile-cooldown: 500 # Cooldown between projectiles (ms)
projectile:
  explosion-radius: 3.0       # Size of projectile explosion
  set-fire: true             # Whether projectiles set blocks on fire
  fire-trail: true           # Enable particle trail effects
  fire-chance: 0.3333        # Chance for blocks to catch fire (0-1)
```

## Dragon Types and Incubation

Dragons hatch into different types based on incubation conditions:

### Fire Dragon
- **Incubation**: Keep egg in hot environments
  - Nether
  - Near lava
  - Near furnaces
  - Near torches
- **Abilities**: 
  - Powerful fireballs
  - Fire resistance
  - Flame particle trails
- **Starting Items**:
  - 10x Fire Shards on hatching

### Ice Dragon
- **Incubation**: Keep egg in cold environments
  - Cold biomes
  - Underwater
  - Near ice
- **Abilities**:
  - Ice projectiles
  - Water breathing
  - Frost particle trails
- **Starting Items**:
  - 10x Ice Shards on hatching

### Lightning Dragon
- **Incubation**: Keep egg in dark environments
  - Nighttime
  - Dark areas
  - Underground
- **Abilities**:
  - Lightning strikes
  - Night vision
  - Electric particle trails
- **Starting Items**:
  - 10x Lightning Shards on hatching

## Dragon Shards

Special items used to power dragon abilities. Each dragon type requires its corresponding shard type.

### Obtaining Shards
1. **Initial Shards**: 
   - Receive 10 shards when your dragon egg hatches
   - Type matches your dragon's type

2. **Crafting Recipes**:

#### Fire Shard
```
[TNT] [Fire Charge]
```
- Ingredients:
  - 1x TNT
  - 1x Fire Charge
- Output: 1x Fire Shard

#### Ice Shard
```
[TNT] [Ice Block]
```
- Ingredients:
  - 1x TNT
  - 1x Ice Block
- Output: 1x Ice Shard

#### Lightning Shard
```
[TNT] [Lightning Rod]
```
- Ingredients:
  - 1x TNT
  - 1x Lightning Rod
- Output: 1x Lightning Shard

## Commands

### Basic Commands
- `/summondragon`: Main command to manage your dragon
  - `/summondragon buy`: Purchase a dragon egg
  - `/summondragon lost`: Repurchase a lost egg (half price)
  - `/summondragon`: Summon/despawn your dragon

### Management Commands
- `/dragonmanage`: Open dragon management interface
  - Customize appearance
  - Change particle effects
  - Rename dragon
  - View dragon information

### Admin Commands
- `/summondragon admin <player>`: Force hatch a player's egg

## Permissions

### Player Permissions
- `rokidragon.command.buy`: Purchase dragon eggs
- `rokidragon.command.summon`: Summon dragons
- `rokidragon.command.manage`: Access management interface

### Admin Permissions
- `rokidragon.admin`: Full admin access
  - Includes all player permissions
  - Access to admin commands
  - Override ownership checks

## Usage Guide

### Getting Started
1. Purchase a dragon egg (`/summondragon buy`)
2. Choose your desired dragon type:
   - Place egg in appropriate environment
   - Wait for incubation (default: 1 hour)
3. Once hatched, summon your dragon (`/summondragon`)
4. Receive your initial 10 dragon shards

### Riding Controls
- **Mount**: Right-click the dragon
- **Movement**: 
  - W/S: Forward/Backward
  - A/D: Turn left/right
  - Space: Ascend
  - Shift: Descend
- **Abilities**: Right-click with corresponding shard type:
  - Fire Dragon: Fire Shard
  - Ice Dragon: Ice Shard
  - Lightning Dragon: Lightning Shard

### Customization
1. Open management interface (`/dragonmanage`)
2. Select from available options:
   - Dragon appearance
   - Particle effects
   - Color schemes
   - Custom name

## Development

### Building from Source
1. Clone the repository
2. Install Maven
3. Run `mvn clean package`
4. Find the built JAR in `target/`

### API Usage
The plugin provides an API for developers:
```java
DragonPlugin.getInstance().getDatabaseManager()
DragonPlugin.getInstance().getEggManager()
```

### Contributing
1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Submit pull request

## Troubleshooting

### Common Issues
1. **Dragon won't spawn**: 
   - Verify egg is hatched
   - Check ownership
   - Ensure proper permissions

2. **Can't purchase egg**:
   - Check economy balance
   - Verify cooldown period
   - Confirm permissions

3. **Wrong dragon type**:
   - Review incubation conditions
   - Check environment requirements
   - Monitor incubation time

4. **Can't use abilities**:
   - Verify you have the correct shard type
   - Check shard count
   - Ensure cooldown period has passed

### Support
- Report issues on GitHub
- Join our Discord for help
- Check wiki for guides

## License

This project is licensed under the MIT License. See LICENSE file for details.
