<div align="center">
   
# PepeForge
![License](https://img.shields.io/github/license/mazurpiotr/pepeforge?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1+-brightgreen?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-fuchsia?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)

A Spigot/Paper plugin adding custom weapons and tools to Minecraft servers.

---
</div>

## Features

PepeForge adds custom weapons, tools and gameplay mechanics to Minecraft servers.

Current content includes:
- Greatswords
- Wind-themed weapons
- Crescent-themed weapons
- Chisel and scythes
- Optional custom models and textures via resource pack

See [ITEMS.md](ITEMS.md) for the full item and recipe list.

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place `pepeforge-1.0.0.jar` in your server's `plugins/` folder
3. (Optional) Download the resource pack from [Releases](../../releases) for custom item models, textures and client-side item translation
   - For server-side distribution, using `ResourcePackManager` is recommended.
4. Restart server

## Configuration

Edit `plugins/PepeForge/config.yml` to enable or disable custom items, recipes and localization options.

Each custom item and its recipe can be configured independently.

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge give <item> <player>` - Gives an item to a player

## Permissions

- `pepeforge.give` - Use `/pepeforge give` (OP by default)
- `pepeforge.items` - Use `/pepeforge items` (OP by default)

## Issues & Support

While I try to properly test all mechanics and gameplay interactions, some undiscovered edge cases, exploits or balancing problems may still happen.

If you run into any bugs or weird behavior, please open an issue on GitHub.

Suggestions, feedback and feature requests are always appreciated.

## Compatibility

- Minecraft 1.21.11+
- Java 21+
- Paper & Purpur recommended
- Spigot supported with limited features

> Paper/Purpur provide the best experience and full feature support.

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
