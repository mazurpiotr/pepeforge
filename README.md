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

Pepe's Forge adds custom weapons, tools to Minecraft servers.

See [ITEMS.md](ITEMS.md) for the full item and recipe list.

## Installation

1. Download the JAR matching your server's Minecraft release from [Releases](../../releases).
2. Make sure the server uses the Java runtime required by that release.
3. Place `pepeforge-X.Y.Z.jar` in your server's `plugins/` folder and start the server once.
4. Configure Resource Pack delivery as described below.
5. Restart the server or run `/pepeforge reload` after changing the configuration.

## Resource Pack Installation

Since 1.2.0 Pepe's Forge uses Minecraft's modern `item_model` system. The plugin supports **Paper**, **Purpur**, **Spigot**, and **Folia** on Minecraft 1.21.11+.

The matching Resource Pack is required for custom item models and complete item presentation. Download it from [GitHub Releases](https://github.com/mazurpiotr/pepeforge/releases).

### Resource Pack Delivery

Resource Pack delivery supports two modes. `MANAGER` is the default and leaves delivery to an external Resource Pack manager:

```yaml
resource_pack:
  mode: MANAGER
```

In `GITHUB` mode, PepeForge sends the Resource Pack directly from GitHub Releases:

```yaml
resource_pack:
  mode: GITHUB
```

On first startup in `GITHUB` mode, Pepe's Forge copies the build's default release URL and SHA-1 into `plugins/PepeForge/resource-pack.properties`. Edit this file to use a different Resource Pack URL. At startup, Pepe's Forge checks the URL's `.sha1` sidecar and updates `sha1` if the sidecar matches the downloaded pack. If the sidecar is unavailable, a matching local SHA-1 can still be used.

### Language Support

On Paper, Purpur, and Folia, item names and descriptions can follow each player's Minecraft client language when client-side translations are enabled. The Resource Pack must be loaded for this feature; otherwise, players may see raw translation keys such as `item.pepeforge.katana.name`.

Spigot and CraftBukkit use the server-side language fallback configured by the administrator and do not provide client-side translations.

## Migration Guide (From 1.0 or 1.1)

<details>
<summary><b>Click to expand migration details</b></summary>

Upgrading from an older version of Pepe's Forge to the new `item_model` system preserves the logical identity of supported existing items.

- **Existing Items:** Supported legacy items can be migrated when they are handled by the plugin.
- **Identity Preservation:** The plugin retains the internal `item_id` used to identify custom items.
- **Upgrade:** Replace the JAR, restart the server, and distribute the updated Resource Pack. Test representative existing items after major updates.

</details>

## FAQ

**Q: Will my existing items stop working after updating from an older version?**
A: Supported items from Pepe's Forge 1.0 and 1.1 retain their logical `item_id` and are migrated when handled by the plugin. See the [Migration Guide](#migration-guide-from-10-or-11) for the upgrade workflow.

**Q: Does the Resource Pack conflict with other custom texture packs?**
A: The pack keeps custom models in the `pepeforge` namespace and does not replace vanilla assets. Compatibility still depends on how another pack handles the same client resources.

## Configuration

Edit `plugins/PepeForge/config.yml` to configure item availability, crafting recipes, selected mechanics, language and translation behavior, resource-pack delivery, and anonymous metrics.

Use `/pepeforge config` for the supported in-game item and recipe settings. Custom crafting recipes and smithing upgrades may be controlled separately where applicable.

The generated `config.yml` contains the current settings and defaults for the installed plugin version. Missing default settings are added automatically while existing values are preserved. Run `/pepeforge reload` after making manual changes.

If `language` does not match a bundled or local language file, the plugin falls back to `en_us` and saves that value in the config.

Bundled `en_us.yml` and `pl_pl.yml` files in `plugins/PepeForge/lang` are refreshed from the installed plugin version at startup. When an existing file differs, PepeForge saves a versioned `.backup-*` copy before replacing it. Custom language files that are not bundled remain untouched.

### Statistics (bStats)

Pepe's Forge uses [bStats](https://bstats.org/) to collect anonymous usage data, including crafting and give counts and which items are disabled in the server configuration.
This helps guide the future development of the plugin. You can opt-out at any time by setting `metrics.enabled: false` in `config.yml`.

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge config` - Opens an in-game GUI menu to toggle items and recipes
- `/pepeforge reload` - Reloads the plugin configuration
- `/pepeforge migration <on|pause|disable>` - Controls lazy legacy-item migration
- `/pepeforge give <item> <player>` - Gives an item to an online player when their inventory has room; failed deliveries are not counted in item-given statistics
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
- Use the Java runtime required by the selected Minecraft release
- Paper, Purpur, Spigot and Folia

Paper, Purpur and Folia provide the best experience when client-side item translations are enabled. Spigot and CraftBukkit use the server-side fallback text path.

## Protection and Anti-Griefing

Pepe's Forge is developed with server protection, PvP and claim plugins in mind. Combat damage uses normal server damage events, so protection plugins may cancel those events; cancelled hits do not apply related weapon effects. Block grappling does not modify blocks.

This is based on standard server events rather than dedicated integrations with specific protection plugins. It does not guarantee complete compatibility with every protection or movement rule, and support for protection-sensitive mechanics may be expanded in future.

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
