<div align="center">

# Pepe's Forge
![License](https://img.shields.io/github/license/mazurpiotr/pepeforge?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.4.0-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11+-brightgreen?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21%20%7C%2025-orange?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Spigot%20%7C%20Folia-fuchsia?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)

A Paper, Spigot, Purpur, and Folia plugin adding custom weapons and tools to Minecraft servers.

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
- Heavy Anchor (grappling hook movement & snare utility)
- Throwing Knives and Stormcleaver (ranged attacks and charged thunder leaps)
- Custom models and textures through the modern resource pack published with each release

See [ITEMS.md](ITEMS.md) for the full item and recipe list.

## Installation

1. Download the latest JAR from [Releases](../../releases).
2. Place `pepeforge-X.Y.Z.jar` in your server's `plugins/` folder.
3. Download and configure your Resource Pack (see **Resource Pack Installation** below).
4. Restart your server.

## Resource Pack Installation

Since 1.2.0 Pepe's Forge uses Minecraft's modern item model system. It works on **Paper/Purpur** (Recommended), **Spigot/CraftBukkit** and Folia for Minecraft 1.21.11+. You can download the resource pack from [GitHub Releases](https://github.com/mazurpiotr/pepeforge/releases).

*⚠ Note for Paper users: By default, Pepe's Forge uses client-side translations (`translations.use_client_side` = true), which **requires** players to have the resource pack loaded. Otherwise, custom item names and lore will appear as raw translation keys (e.g., `item.pepeforge.crimson_sword.name`).*

*For the best experience, distributing the resource pack automatically with a plugin such as **ResourcePackManager** or **ForceResourcePack** is highly recommended.*

## Migration Guide (From 1.0 or 1.1)

<details>
<summary><b>Click to expand migration details</b></summary>

Upgrading from an older version of Pepe's Forge to the new `item_model` system preserves the logical identity of supported existing items.

- **Existing Items:** Supported legacy items can be migrated when they are handled by the plugin.
- **Identity Preservation:** The plugin retains the internal `item_id` used to identify custom items.
- **Upgrade:** Replace the JAR, restart the server, and distribute the updated Resource Pack. Test representative existing items after major updates.

</details>

## FAQ

**Q: Does the Resource Pack work on Spigot?**
A: The resource pack is designed for the supported 1.21.11+ server platforms. Paper and Folia provide the full client-side translation path; Spigot uses the compatible server-side fallback where needed.

**Q: Will my existing items stop working after updating from an older version?**
A: Supported legacy items retain their logical `item_id` and may be migrated when the plugin handles them. Appearance and text should be checked after an upgrade.

**Q: Does the Resource Pack conflict with other custom texture packs?**
A: The pack keeps custom models in the `pepeforge` namespace and does not replace vanilla assets. Compatibility still depends on how another pack handles the same client resources.

## Configuration

Edit `plugins/PepeForge/config.yml` to enable or disable custom items, recipes and localization options.

Each custom item and its recipe can be configured independently.

### Statistics (bStats)

Pepe's Forge uses [bStats](https://bstats.org/) to collect anonymous usage data, such as the popularity of specific weapons and configuration settings.
This helps guide the future development of the plugin. You can opt-out at any time by setting `metrics.enabled: false` in `config.yml`.

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge config` - Opens an in-game GUI menu to toggle items and recipes
- `/pepeforge reload` - Reloads the plugin configuration
- `/pepeforge migration <on|pause|disable>` - Controls lazy legacy-item migration
- `/pepeforge give <item> <player>` - Gives an item to a player
- `/pepeforge setlevel <level>` - Sets the level of the Crimson Sword in your main hand (for debugging / testing)

## Permissions

- `pepeforge.give` - Use `/pepeforge give` (OP by default)
- `pepeforge.items` - Use `/pepeforge items` (OP by default)
- `pepeforge.config` - Use `/pepeforge config` (OP by default)
- `pepeforge.reload` - Use `/pepeforge reload` (OP by default)
- `pepeforge.migration` - Use `/pepeforge migration` (OP by default)
- `pepeforge.setlevel` - Use `/pepeforge setlevel <level>` (OP by default)

## Issues & Support

While I try to properly test all mechanics and gameplay interactions, some undiscovered edge cases, exploits or balancing problems may still happen.

If you run into any bugs or weird behavior, please open an issue on GitHub.

Suggestions, feedback and feature requests are always appreciated.

## Compatibility

- Minecraft 1.21.11+
- Java 21 for Minecraft 1.21.11; Java 25 for the 26.x builds
- Paper, Purpur, Spigot and Folia

Paper, Purpur and Folia provide the best experience when client-side item translations are enabled. Spigot and CraftBukkit use the server-side fallback text path.

## Protection and Anti-Griefing

Pepe's Forge is developed with server protection, PvP and claim plugins in mind. Combat damage uses normal server damage events, so protection plugins may cancel those events; cancelled hits do not apply related weapon effects. Block grappling does not modify blocks.

This is based on standard server events rather than dedicated integrations with specific protection plugins. It does not guarantee complete compatibility with every protection or movement rule, and support for protection-sensitive mechanics may be expanded in future.

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
