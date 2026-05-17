# PepeForge

A Spigot/Paper plugin adding custom weapons and tools to Minecraft servers.

**License**: MIT | **Status**: MVP Release

This is currently a development build and may still contain bugs.

## Features

PepeForge adds custom combat and utility items built around a few clear gameplay themes:

- mobility-focused melee weapons
- special ranged weapons with custom shot patterns
- farming tools with harvest automation
- decorative utility tools for block-variant cycling

Current mechanics include:

- speed-trigger melee effects
- combo-based melee payoffs
- parry / projectile reflection windows
- multi-shot bow behavior
- harvest + auto-replant flow
- decorative block cycling

For the current item and recipe reference, see [ITEMS.md](ITEMS.md).

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place `pepeforge-1.0.0.jar` in your server's `plugins/` folder
3. (Optional) Download the resource pack from [Releases](../../releases) for custom item models and textures
- For server-side distribution, using `ResourcePackManager` is recommended.
4. Restart server

## Configuration

Edit `plugins/PepeForge/config.yml` to customize:

- plugin message language
- client-side vs server-side item text translation mode
- enabling or disabling specific custom item recipes

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge give <item> <player>` - Gives an item to a player

## Localization

Supports English and Polish:
- **Server-side**: English (en_us) and Polish (pl_pl) available in `lang/` folder
- **Client-side**: Item names and lore via resource pack (if client-side translation enabled)

## Resource Pack

Optional resource pack included with dedicated custom models and textures for all items.

## Compatibility

- **Target**: Bukkit, Spigot, Paper
- **Java**: 21+
- **Minecraft**: 1.21.11+

## Permissions

- `pepeforge.give` - Use `/pepeforge give` (OP by default)
- `pepeforge.items` - Use `/pepeforge items` (OP by default)

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
