# PepeForge

A Spigot/Paper plugin adding custom weapons and tools to Minecraft servers.

**License**: MIT | **Status**: MVP Release

This plugin is in active development and may still contain bugs.

## Features

PepeForge currently adds 9 custom weapons and two utility tool types to Minecraft servers.

Weapons include the Greatsword series, Wind-themed weapons and Crescent-themed weapons. Tools include chisels and scythes.

An optional resource pack adds custom textures and models for all new weapons and tools.

For the current item and recipe reference, see [ITEMS.md](ITEMS.md).

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

## Localization

Supports English and Polish with both server-side and optional client-side item text:

- **Server-side**: `lang/en_us.yml` and `lang/pl_pl.yml` are always available for messages, item names and lore
- **Client-side**: Paper/Folia/Purpur can render item translation keys from the resource pack when `translations.use_client_side` is `true`
- **Spigot fallback**: Spigot does not support Paper item text components, so the plugin will use server-side item names and lore instead
- **Config behavior**: `translations.use_client_side: false` forces server-side wording even on Paper/Purpur

## Resource Pack

The resource pack is optional but recommended for:

- custom models and textures for all items
- client-side item localization for Paper/Folia/Purpur
- the full visual experience of new weapons and tools

If the pack is not installed, custom items still function with server-side names and lore.

## Compatibility

- **Best support**: Paper and Purpur (full client-side translation + item text when enabled)
- **Supported fallback**: Spigot uses server-side localization and item lore from `lang/*.yml`
- **Java**: 21+
- **Minecraft**: 1.21.11+

> Paper/Purpur are recommended for the full feature set. The plugin is built to avoid hard failures when Paper-only item text APIs are unavailable.

## Permissions

- `pepeforge.give` - Use `/pepeforge give` (OP by default)
- `pepeforge.items` - Use `/pepeforge items` (OP by default)

## License

MIT License - See [LICENSE.md](LICENSE.md)

---
