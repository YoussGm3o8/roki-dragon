# RokiDragonPlugin

RokiDragonPlugin is a Nukkit plugin that introduces a rideable dragon entity to your Minecraft server. Players can purchase, hatch, and summon their own dragons. The plugin also includes features for managing dragon eggs and ensuring that only the rightful owner can use them.

## Features

- **Custom Dragon Entity**: Adds a custom dragon entity that players can summon and ride.
- **Dragon Egg Management**: Players can purchase dragon eggs, which will hatch after a specified time.
- **Ownership Verification**: Ensures that only the rightful owner can use a dragon egg or summon a dragon.
- **Admin Commands**: Admins can forcefully hatch dragon eggs for players.
- **Economy Integration**: Integrates with EconomyAPI to handle dragon egg purchases.

## Installation

1. Download the latest release of the plugin from the [releases page](https://github.com/YoussGm3o8/roki-dragon/releases).
2. Place the downloaded JAR file into the `plugins` folder of your Nukkit server.
3. Start your server to generate the default configuration files.
4. Configure the plugin by editing the `config.yml` file located in the `plugins/RokiDragon` folder.

## Dependencies

You need to have EconomyAPI and MobPlugin on your server for this plugin to work.
1. MobPlugin: https://cloudburstmc.org/resources/mobplugin.3/
2. EconomyAPI: https://cloudburstmc.org/resources/economyapi.14/

## Configuration

The `config.yml` file allows you to customize various aspects of the plugin:

```yaml
dragon-egg-price: 128000
dragon-hatching-time: 3600 # Hatching time in seconds (default: 1 hour)
dragon-buy-cooldown: 60 # Cooldown in seconds between purchases (default: 60 seconds)
```

## Commands

- `/summondragon [buy|lost|admin <player>]`: Main command to summon or buy a dragon.
  - `buy`: Purchase a dragon egg.
  - `lost`: Repurchase a lost dragon egg at a reduced cost.
  - `admin <player>`: Admin command to forcefully hatch a dragon egg for a player.

## Permissions

- `rokidragon.command.buy`: Allows a player to buy a dragon egg.
- `rokidragon.command.summon`: Allows a player to summon a dragon.

## Usage

### Buying a Dragon Egg

Players can buy a dragon egg using the `/summondragon buy` command. The egg will hatch after the specified hatching time.

### Summoning a Dragon

Once the egg has hatched, players can summon their dragon using the `/summondragon` command. If the dragon is already summoned, the command will despawn it.

### Repurchasing a Lost Egg

If a player loses their dragon egg, they can repurchase it at a reduced cost using the `/summondragon lost` command.

### Admin Commands

Admins can forcefully hatch a dragon egg for a player using the `/summondragon admin <player>` command.

## Development

### Building from Source

To build the plugin from source, you will need Maven. Clone the repository and run the following command:

```sh
mvn clean package
```

The built JAR file will be located in the `target` directory.

### Contributing

Contributions are welcome! Please open an issue or submit a pull request on GitHub.
