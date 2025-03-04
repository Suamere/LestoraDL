# Lestora Dynamic Lights

This is a standalone client mod that allows you to configure entities with light levels, as well as set the efficiency (tick update time).  Held entities and thrown entities will then emit that light level from the position wherever the entity is.

## Features
- **Usage:** Dynamically lights up the area while holding a glowing item, or if you throw an entity.
- **Configuration:** Added lestora-lighting.toml, used for configuring objects and their light levels.

## Manual Installation
1. Download the mod JAR from CurseForge.
2. Place the JAR file into your `mods` folder.
3. Launch Minecraft with the Forge profile.

## Commands
- Use the command `/lestora dynamicLighting whatAmIHolding`: This command will tell you what you're holding in each hand, in case you need the official name in order to add it to the config file.
- Use the command `/lestora dynamicLighting [0-100]` to set the efficiency.  1 is the best, 15 is similar to OptiFine, 100 is rediculously slow, lol.  0 effectively turns the mod off.
- Use the command `/lestora dynamicLighting fixNearby` in case some artifact remains lit.  Report this so I can troubleshoot if it happens (It has only happened once to me and I can't figure out why).  This works in a 20 block radius from the player.
- Use the command `/lestora dynamicLighting chunkDistance [0-10]` to set the chunk loading distance for Placed BLOCKS (beta).  So if your configuration has something like "minecraft:diamond_ore, 14", then this is how many chunk distance those blocks will emit light from the player (0 is off, 3 is recommended)

## Compatibility
- **Minecraft Version:** 1.21.4
- **Forge Version:** 54.1.0

## Troubleshooting
If you run into issues (e.g., crashes or unexpected behavior), check the logs in your `crash-reports` or `logs` folder. You can also open an issue on the mod’s GitHub repository.

## Contributing
Contributions are welcome! Please submit pull requests or open issues if you have suggestions or bug reports.

## License
This project is licensed under the MIT License.
