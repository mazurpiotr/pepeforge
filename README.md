<div align="center">
   
# Pepe's Forge
![License](https://img.shields.io/github/license/mazurpiotr/pepeforge?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.2-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11+-brightgreen?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Spigot-fuchsia?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)

A Spigot/Paper plugin adding custom weapons and tools to Minecraft servers.

---
</div>

## Features

Pepe's Forge adds custom weapons, tools and gameplay mechanics to Minecraft servers.

Current content includes:
- Greatswords and Katana (two-handed rhythm & parry weapons)
- Wind-themed weapons (high-mobility gear with dash abilities)
- Crescent-themed weapons (moonlight-powered bow and spear)
- Chisel and scythes (specialized building & AoE farming tools)
- Legendary Crimson Sword and Solar Shield (combat progression & sun energy defense)
- Custom models and textures via modern/classic resource packs

See [ITEMS.md](ITEMS.md) for the full item and recipe list.

**All custom weapons, tools, and mechanics are designed to strictly respect standard region claims and anti-grief plugins.**

## Installation

1. Download the latest JAR from [Releases](../../releases).
2. Place `pepeforge-X.Y.Z.jar` in your server's `plugins/` folder.
3. Download and configure your Resource Pack (see **Resource Pack Installation** below).
4. Restart your server.

## Resource Pack Installation

Since 1.2.0 Pepe's Forge uses exclusively the modern `item_model` system for visual routing, which is much cleaner and more efficient than the old system. It works natively on **Paper/Purpur** (Recommended) and **Spigot/CraftBukkit** environments for Minecraft 1.21.11+. You can download the resource pack from [GitHub Releases](https://github.com/mazurpiotr/pepeforge/releases).

*⚠ Note for Paper users: By default, Pepe's Forge uses client-side translations (`translations.use_client_side` = true), which **requires** players to have the resource pack loaded. Otherwise, custom item names and lore will appear as raw translation keys (e.g., `item.pepeforge.crimson_sword.name`).*

*For the best experience, distributing the resource pack automatically with a plugin such as **ResourcePackManager** or **ForceResourcePack** is highly recommended.*

## Migration Guide (From 1.0 or 1.1)

<details>
<summary><b>Click to expand migration details</b></summary>

Upgrading from an older version of Pepe's Forge to the new `item_model` system is fully backward-compatible.

- **Existing Items:** All of your previously generated custom items will continue to function without any issues thanks to the built-in lazy migrator script.
- **Identity Preservation:** The plugin retains the internal `item_id` to logically identify custom items.
- **Seamless Upgrade:** Simply drop the new `.jar` into your plugins folder and distribute the updated Resource Pack.

</details>

## FAQ

**Q: Does the Resource Pack work on Spigot?**
A: Yes! Starting with PepeForge 1.2.0, the Resource Pack works flawlessly across all 1.21.11+ server software, taking full advantage of the `item_model` component.

**Q: Will my existing items stop working after updating from an older version?**
A: Not at all! The backend preserves the logical `item_id` and actively modernizes old items on the fly, so all your old items will remain functional and visually intact.

**Q: Does the Resource Pack conflict with other custom texture packs?**
A: Because our pack exclusively uses modern `item_model` routing, it completely avoids overwriting vanilla items (like shears or swords). This vastly improves compatibility with other custom packs!

## Configuration

Edit `plugins/PepeForge/config.yml` to enable or disable custom items, recipes and localization options.

Each custom item and its recipe can be configured independently.

### Statistics (bStats)

Pepe's Forge uses [bStats](https://bstats.org/) to collect anonymous usage data, such as the popularity of specific weapons and configuration settings.
This helps guide the future development of the plugin. You can opt-out at any time by setting `metrics.enabled: false` in `config.yml`.

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge give <item> <player>` - Gives an item to a player
- `/pepeforge setlevel <level>` - Sets the level of the Crimson Sword in your main hand (for debugging / testing)

## Permissions

- `pepeforge.give` - Use `/pepeforge give` (OP by default)
- `pepeforge.items` - Use `/pepeforge items` (OP by default)
- `pepeforge.setlevel` - Use `/pepeforge setlevel <level>` (OP by default)

## Issues & Support

While I try to properly test all mechanics and gameplay interactions, some undiscovered edge cases, exploits or balancing problems may still happen.

If you run into any bugs or weird behavior, please open an issue on GitHub.

Suggestions, feedback and feature requests are always appreciated.

## Compatibility

- Minecraft 1.21.11+
- Java 21+
- Paper, Purpur, and Spigot supported

> Paper/Purpur provide the best experience due to native client-side translation capabilities.

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
