<div align="center">
   
# PepeForge
![License](https://img.shields.io/github/license/mazurpiotr/pepeforge?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0-blue?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11+-brightgreen?style=for-the-badge)
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
- Legendary Crimson Sword and Solar Shield
- custom models and textures via resource pack

See [ITEMS.md](ITEMS.md) for the full item and recipe list.

**All custom weapons, tools, and mechanics are designed to strictly respect standard region claims and anti-grief plugins.**

## Installation

1. Download the latest JAR from [Releases](../../releases).
2. Place `pepeforge-X.Y.Z.jar` in your server's `plugins/` folder.
3. Download and configure your Resource Pack (see **Resource Pack Installation** below).
4. Restart your server.

## Resource Pack Installation

PepeForge utilizes a **hybrid visual routing system**, providing two distinct resource packs to maximize compatibility and take advantage of modern Minecraft features. You can download the resource packs from [GitHub Releases](https://github.com/mazurpiotr/pepeforge/releases).

### Choosing Your Resource Pack

**Classic Pack (`PepeForge-ResourcePack-Classic.zip`)**
- **Target Platform:** Spigot / CraftBukkit.
- **Routing:** Uses `CustomModelData`.
- **Description:** The traditional method for custom item models. Essential for full backward compatibility on older server software or specific setups.

**Modern Pack (`PepeForge-ResourcePack-Modern.zip`)**
- **Target Platform:** Paper / Folia (1.21.11+).
- **Routing:** Uses the new `item_model` component.
- **Description:** Recommended for modern servers. It does not overwrite vanilla models, ensuring cleaner compatibility with other plugins and resource packs.

### Recommended Configuration (Paper)
If you are running Paper or Folia 1.21.11+, we strongly recommend using the **Modern Pack**. This leverages the newer `item_model` API, offering native visual handling without conflicting with vanilla Minecraft resources.

*⚠ If `translations.use_client_side` is enabled and players do not have the resource pack installed, custom item names and lore will appear as raw translation keys (e.g. `item.pepeforge.crimson_sword.name`) instead of readable text.*

*For the best experience, distributing the resource pack automatically with a plugin such as **ResourcePackManager** or **ForceResourcePack** is highly recommended.*

## Migration Guide

Upgrading from an older version of PepeForge to the new hybrid system is seamless.

- **Existing Items:** All of your previously generated custom items will continue to function without any issues.
- **Identity Preservation:** The plugin retains the internal `item_id` to logically identify custom items.
- **Visual Fallbacks:** `CustomModelData` is still written to all items as a fallback. This guarantees that items created in older versions or moved between Spigot and Paper servers will still render correctly using the Classic pack.

## FAQ

**Q: Which Resource Pack should I choose?**
A: Use the **Modern Pack** if you are running Paper/Folia 1.21.11+. Use the **Classic Pack** if you are on standard Spigot or CraftBukkit.

**Q: Can I use the Modern Pack on Spigot?**
A: No, standard Spigot does not natively support the new `item_model` data component in the same way Paper does. You should use the Classic Pack.

**Q: Will my existing items stop working?**
A: Not at all! Existing items will preserve their functionality and visuals because the plugin maintains both `item_id` and `CustomModelData` on the backend.

**Q: Does the Modern Pack conflict with other Resource Packs?**
A: No. The Modern Pack takes advantage of the `item_model` component, which adds new custom items without overriding existing vanilla Minecraft items or models, drastically reducing conflicts.

## Configuration

Edit `plugins/PepeForge/config.yml` to enable or disable custom items, recipes and localization options.

Each custom item and its recipe can be configured independently.

### Statistics (bStats)

PepeForge uses [bStats](https://bstats.org/) to collect anonymous usage data, such as the popularity of specific weapons and configuration settings.
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
- Paper & Purpur recommended
- Spigot supported with limited features

> Paper/Purpur provide the best experience and full feature support.

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
