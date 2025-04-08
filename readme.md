# Lestora Dynamic Lights

This is a standalone client mod that allows items and blocks with light levels to dynamically emit light.  This could be when held, when throwing entities, or when placing entities and blocks.

On the client-side mod, you can set the efficiency (tick update time).

This mod will use default light levels as documented in Minecraft, let me know if I get one wrong as it's not exposed in code and I had to manually do it.

This mod also allows you to optionally depend on Lestora Config, which will create at `config/lestora-lighting.toml` with default values, where you can add or update values.

## Features
- **Usage:** Dynamically lights up the area while holding a glowing item, or if you throw an entity.
- **Configuration:** Use the dependency on Lestora Config's lestora-lighting.toml, used for configuring objects and their light levels.

## Manual Installation
1. Download the mod JAR from CurseForge.
2. Place the JAR file into your `mods` folder.
3. Launch Minecraft with the Forge profile.

## Commands
- Use the command `/lestora dynamicLighting efficiency [0-100]` to set the efficiency.  1 is the best, 15 is similar to OptiFine, 100 is rediculously slow, lol.  0 effectively turns the mod off.
- Use the command `/lestora dynamicLighting fixNearby` in case some artifact remains lit.  Report this so I can troubleshoot if it happens (It has only happened once to me and I can't figure out why).  This works in a 20 block radius from the player.
- Use the command `/lestora dynamicLighting blocksEnabled [true/false]` true by default.  Determines if it should render custom block lighting defined in Lestora Config, such as Diamond Ore or whatever you want.

## Compatibility
- **Minecraft Version:** 1.21.4
- **Forge Version:** 54.1.0

## Troubleshooting
If you run into issues (e.g., crashes or unexpected behavior), check the logs in your `crash-reports` or `logs` folder. You can also open an issue on the mod’s GitHub repository.

## Contributing
Contributions are welcome! Please submit pull requests or open issues if you have suggestions or bug reports.

## License
This project is licensed under the MIT License.
