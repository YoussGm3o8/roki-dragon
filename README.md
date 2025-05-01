# RokiDragon

**Version:** 1.0.0
**Author:** YoussGm3o8
**Nukkit API:** 1.0.0+

RokiDragon is a Nukkit plugin that introduces customizable, rideable dragons to your Minecraft server. Players can acquire, hatch, manage, and summon their own dragons with unique types and abilities.

## Features

*   **Custom Dragon Entity**: Fully rideable dragon entity with smooth flying controls.
*   **Dragon Types**: Three unique dragon types hatch based on incubation conditions:
    *   **Fire Dragon**: Hatches in hot environments. Shoots explosive fireballs. Fire resistant.
    *   **Ice Dragon**: Hatches in cold environments. Shoots freezing ice balls. Breathes underwater. Vulnerable to fire.
    *   **Lightning Dragon**: Hatches in dark environments. Shoots small explosive lightning balls. Has night vision.
*   **Dragon Management GUI**: Access via `/managedragons` to:
    *   Purchase Dragon Eggs.
    *   View and manage stored eggs (up to configurable limit).
    *   Recover lost eggs (if applicable).
    *   Customize dragon appearance, colors, and particle effects (details may vary).
    *   Rename your dragon (also available via `/dragon name`).
*   **Egg Incubation**: Place eggs in the world; they hatch based on environment and configured time.
*   **Ability System**: Use specific items while riding to activate abilities (configurable):
    *   Fire Dragon: Fire Charge (default)
    *   Ice Dragon: Snowball (default)
    *   Lightning Dragon: Glowstone Dust (default)
*   **Dragon Loaf**: A craftable item (Wheat, Cooked Beef, Dragon Egg) likely used for healing or taming (exact function may vary).
*   **Permanent Death**: If your dragon dies, it is lost forever!
*   **Admin Logs**: Track lost dragon egg events via `/dragonlogs`.
*   **Database Integration**: Uses DbLib for robust data storage (SQLite default, MySQL supported).
*   **Localization**: Plugin messages can be translated via language files in the `lang` folder.

## Installation

1.  Download the latest `RokiDragon.jar` from the [releases page](https://github.com/YoussGm3o8/roki-dragon/releases) (link placeholder).
2.  Ensure your server is running a compatible version of Nukkit.
3.  Install the required dependencies (see below). Place their JAR files in your server's `plugins` folder.
4.  Place the `RokiDragon.jar` file in your server's `plugins` folder.
5.  Start your server. RokiDragon will generate its default configuration files (`config.yml`, `lang/en_US.yml`, etc.) inside `plugins/RokiDragon/`.
6.  Stop the server and configure `plugins/RokiDragon/config.yml` to your liking.
7.  Configure DbLib (if using MySQL) in `plugins/DbLib/config.yml`.
8.  Start your server again.

## Dependencies

*   **DbLib**: **Required** for database operations. Download from [Nukkit-coders/DbLib Releases](https://github.com/Nukkit-coders/DbLib/releases).
*   **MobPlugin**: **Required** (Likely) for custom entity functionality. Download from [CloudburstMC Resources](https://cloudburstmc.org/resources/mobplugin.3/).
*   **EconomyAPI**: **Required** (Likely) if using the egg purchase feature with economy integration. Download from [CloudburstMC Resources](https://cloudburstmc.org/resources/economyapi.14/).

*Note: While DbLib is explicitly checked, MobPlugin and EconomyAPI are inferred requirements based on plugin features. Ensure they are installed for full functionality.*

## Configuration (`plugins/RokiDragon/config.yml`)

```yaml
# RokiDragon Configuration File
# -----------------------------
# Configure various aspects of the RokiDragon plugin.

# Language settings for plugin messages
language:
  default: "en_US"  # Default language file to load from the 'lang' folder (e.g., en_US.yml)
  fallback: "en_US" # Fallback language if the default file is missing or invalid

# Economy related settings
economy:
  dragon_egg_price: 128000 # Cost to purchase a dragon egg via the GUI/command

# Timing and Cooldown settings
timing:
  # Egg incubation settings
  eggs:
    incubation_time_seconds: 3600 # Time in seconds for an egg to hatch while incubating (Default: 3600 = 1 hour)

  # Cooldowns for various actions
  cooldowns:
    buy_seconds: 60          # Cooldown in seconds between purchasing eggs (Default: 60)
    # respawn_seconds: 900     # Cooldown before summon after death (REMOVED - Dragons are lost forever)
    ability_milliseconds: 500 # Cooldown in milliseconds between using dragon abilities (e.g., fireball) (Default: 500)
    summon_seconds: 30       # Cooldown in seconds after summoning a dragon before it can be used again (Default: 30)

# Dragon entity base statistics
dragon_stats:
  max_health: 100         # Maximum health points (integer, Default: 100)
  base_damage: 15.0       # Base damage dealt by attacks (Default: 15.0) - Currently not implemented
  damage_reduction: 0.25  # Damage reduction multiplier (0.0 = no reduction, 1.0 = full immunity, Default: 0.25 = 75% reduction)
  move_speed: 1.8         # Base movement speed (Default: 1.8)
  ice_dragon_fire_vulnerability: 1.5 # Damage multiplier for Ice Dragons taking fire/lava damage (Default: 1.5 = 50% extra damage)

# Settings for specific dragon abilities
abilities:
  # Fireball ability (used by Fire Dragon)
  fireball:
    explosion_radius: 3.0 # Size of the explosion (Default: 3.0)
    set_fire: true        # Whether fireballs should set blocks on fire (Default: true)
    fire_chance: 0.3333   # Chance for blocks to catch fire (0.0 to 1.0, Default: 0.3333)

  # Iceball ability (used by Ice Dragon)
  iceball:
    freeze_duration_ticks: 100 # Duration of slowness effect in ticks (20 ticks = 1 second, Default: 100 = 5 seconds)
    freeze_amplifier: 1      # Amplifier for slowness effect (0 = Slowness I, 1 = Slowness II, etc., Default: 1)
    snow_radius: 3           # Radius for placing snow layers on impact (Default: 3)
    snow_chance: 0.7         # Chance to place snow layer on valid blocks (0.0 to 1.0, Default: 0.7)

  # Lightning Ball ability (used by Lightning Dragon)
  lightning_ball:
    explosion_radius: 1.2 # Size of the explosion (Default: 1.2)

  # Item required to use dragon abilities (Format: "minecraft:item_id" or just "item_id")
  required_shoot_item:
    fire: "minecraft:fire_charge" # Default: Fire Charge
    ice: "minecraft:snowball"     # Default: Snowball
    lightning: "minecraft:glowstone_dust" # Default: Glowstone Dust

# Dragon Naming Rules
naming:
  min_length: 3 # Minimum allowed length for a dragon's name (Default: 3)
  max_length: 16 # Maximum allowed length for a dragon's name (Default: 16)

# GUI Settings
gui:
  storage_size: 5 # Maximum number of eggs a player can store in the management GUI (Default: 5)
```

## Dragon Types and Incubation

Dragons hatch into different types based on the environment where the egg is placed during incubation:

### Fire Dragon
*   **Incubation**: Hot environments (Nether, near lava/fire/furnaces/torches).
*   **Abilities**: Shoots explosive fireballs (uses Fire Charge by default), Fire Resistance.

### Ice Dragon
*   **Incubation**: Cold environments (Cold biomes, underwater, near ice/snow).
*   **Abilities**: Shoots freezing ice balls (uses Snowball by default), Water Breathing. Vulnerable to fire damage.

### Lightning Dragon
*   **Incubation**: Dark environments (Deep underground, low light levels, possibly during thunderstorms - exact conditions may vary).
*   **Abilities**: Shoots small lightning balls (uses Glowstone Dust by default), Night Vision.

## Ability Items & Crafting

Dragons use standard Minecraft items to fuel their abilities, configured in `config.yml`.

*   **Default Items:** Fire Charge (Fire), Snowball (Ice), Glowstone Dust (Lightning).
*   **Check Recipes:** Use the `/dragon recipes` command in-game to see crafting recipes for relevant items, including the **Dragon Loaf**.

### Dragon Loaf Crafting
A special food item for dragons.
*   **Recipe (Shaped):**
    ```
    WWW
    BEB
    WWW
    ```
    *   W = Wheat
    *   B = Cooked Beef
    *   E = Dragon Egg (Vanilla)
*   **Output:** 1x Dragon Loaf

## Database Configuration

RokiDragon uses DbLib for database management.

### SQLite (Default)
*   No extra configuration needed. Data is stored in `nukkit.db` in your server's root directory (or as configured by DbLib).

### MySQL
1.  Edit `plugins/DbLib/config.yml`.
2.  Set `use-mysql` to `true`.
3.  Configure your MySQL connection details (`host`, `port`, `database`, `username`, `password`).
4.  Restart the server.

### Data Migration
If you previously used an older version of RokiDragon with a different database structure (`dragons.db`), the plugin *should* attempt to automatically migrate data when started with DbLib installed. **Always back up your old database file before updating.** Verify data integrity after migration.

## Commands

### Player Commands (`rokidragon.use`)
*   `/dragon` (Alias: `/d`) - Main command hub.
    *   `/dragon help`: Shows available `/dragon` subcommands.
    *   `/dragon info`: Displays information about your current dragon (type, name, health if summoned).
    *   `/dragon summon`: Summons your hatched dragon. Has a cooldown after use and after dragon death.
    *   `/dragon despawn`: Dismisses your currently summoned dragon.
    *   `/dragon name <new_name>`: Renames your dragon (respects length limits in `config.yml`).
    *   `/dragon recipes`: Shows crafting recipes for ability items and Dragon Loaf.
    *   `/dragon lost`: Opens the Lost Egg recovery menu in the GUI.
*   `/managedragons` (Alias: `/md`) - Opens the main Dragon Management GUI.
    *   Purchase eggs.
    *   Manage stored eggs.
    *   Customize dragon (name, appearance, etc.).
    *   Recover lost eggs.

### Admin Commands (`rokidragon.admin`)
*   `/dragonlogs` (Alias: `/dlogs`) - View logs related to lost dragon eggs.
    *   `/dragonlogs help`: Shows available `/dragonlogs` subcommands.
    *   `/dragonlogs list [limit]`: Shows the most recent loss logs (default limit: 10).
    *   `/dragonlogs player <name|uuid>`: Shows loss logs for a specific player.

## Permissions

*   `rokidragon.use`
    *   Description: Allows using basic dragon commands (`/dragon`, `/managedragons`).
    *   Default: `true` (All players)
*   `rokidragon.admin`
    *   Description: Allows using admin commands (`/dragonlogs`) and potentially other administrative functions.
    *   Default: `op` (Server operators)

## Usage Guide

1.  **Get an Egg:** Open the management GUI with `/managedragons` and purchase an egg (requires sufficient funds if EconomyAPI is used).
2.  **Incubate:** Place the egg item in the desired environment (hot, cold, or dark) to determine the dragon type. Wait for the incubation time specified in `config.yml` (default: 1 hour).
3.  **Hatch:** Once incubated, the egg should hatch (details may vary - it might become a specific hatched item or automatically register).
4.  **Summon:** Use `/dragon summon` to bring your dragon into the world.
5.  **Ride:** Right-click the dragon to mount. Use standard movement controls (WASD, Space, Shift).
6.  **Use Abilities:** While riding, right-click while holding the required item (Fire Charge, Snowball, or Glowstone Dust by default) to use the dragon's ability. There's a cooldown between uses.
7.  **Manage:** Use `/managedragons` to rename, customize appearance (if available), or manage stored eggs. Use `/dragon name <name>` as an alternative for renaming.
8.  **Dismiss:** Use `/dragon despawn` to safely remove your dragon from the world.
9.  **BE CAREFUL:** If your dragon dies in combat or otherwise, it is **lost forever**.

## Development

### Building from Source
1.  Clone the repository: `git clone https://github.com/YoussGm3o8/roki-dragon.git`
2.  Navigate to the directory: `cd roki-dragon`
3.  Build with Maven: `mvn clean package`
4.  Find the compiled JAR in the `target/` directory.

### API Usage
Access plugin managers via the singleton instance:
```java
// Get plugin instance
DragonPlugin plugin = DragonPlugin.getInstance();

// Access managers
DatabaseManager dbManager = plugin.getDatabaseManager();
DragonEggManager eggManager = plugin.getEggManager();
DragonManager dragonManager = plugin.getDragonManager();
LanguageManager langManager = plugin.getLanguageManager();
// etc.
```

### Contributing
1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Make your changes.
4.  Commit your changes with clear messages.
5.  Push your branch to your fork.
6.  Submit a pull request to the main repository.

## Troubleshooting

*   **Cannot Buy Egg:** Check EconomyAPI is installed, player has sufficient funds, check `/dragon` cooldowns in `config.yml`.
*   **Egg Not Incubating/Hatching:** Ensure the egg is placed correctly in a valid environment, check server logs for errors, verify `incubation_time_seconds` in `config.yml`.
*   **Cannot Summon Dragon:** Ensure the egg has hatched, check `/dragon summon` cooldown, check player has `rokidragon.use` permission, check server logs.
*   **Cannot Use Abilities:** Ensure you are riding the dragon, holding the correct item (`required_shoot_item` in `config.yml`), check ability cooldown (`ability_milliseconds` in `config.yml`).
*   **Wrong Dragon Type Hatched:** Double-check the incubation environment conditions.
*   **Commands Not Working:** Verify plugin loaded correctly (check server logs), ensure correct command syntax, check permissions (`rokidragon.use` or `rokidragon.admin`).

## License

This project is licensed under the MIT License. See the LICENSE file for details. (Assuming MIT based on previous README)
