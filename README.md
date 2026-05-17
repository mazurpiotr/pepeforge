# PepeForge

A Spigot/Paper plugin adding custom weapons and tools to Minecraft servers.

**License**: MIT | **Status**: MVP Release

## Features

This plugin introduces 9 custom items across 5 weapon/tool classes:

### 🪚 Chisel
Decorative utility tool for cycling carved block variants.

| Feature | Details |
|---------|---------|
| Use | Right-click a supported block to advance one decorative variant |
| Reverse | Sneak + right-click to cycle backward |
| Cost | 1 durability per successful conversion |
| Rarity | Rare |

**Crafting** (Shaped Recipe):
```
 I 
 C 
 S 
```
Where I = Iron Ingot, C = Copper Ingot, S = Stick

### ⚔️ Wind Blade
Elemental melee weapons with speed effects.

| Tier | Attack Damage | Effect |
|------|---------------|--------|
| **Iron Wind Blade** | 5 | Speed I for 2s on hit |
| **Diamond Wind Blade** | 6 | Speed I while holding |
| **Netherite Wind Blade** | 7 | Speed I while holding + Speed II for 3s on hit |

**Crafting** (Shaped Recipe):
```
 M 
 M 
 B 
```
Where M = tier material (Iron/Diamond Ingot) and B = Breeze Rod.

Netherite upgrade uses Smithing Table with Diamond Wind Blade + Netherite Ingot.

### 🏹 Crescent Bow
Special ranged weapon that fires a 3-arrow volley.

| Feature | Details |
|---------|---------|
| Arrows | Fires 3 arrows in crescent spread (center + 2 sides) |
| Ammo | Consumes 1 arrow per volley |
| Rarity | Rare |

**Crafting** (Shaped Recipe):
```
 PSA
 P S
 PSA
```
Where P = Phantom Membrane, S = Stick, A = Amethyst Shard

### 🔱 Crescent Spear
Special melee weapon that launches enemies on every third hit.

| Feature | Details |
|---------|---------|
| Proc | Every 3rd hit launches the target upward |
| Buff | Grants Speed I for 15s after a launch |
| Rarity | Rare |

**Crafting** (Shaped Recipe):
```
 A 
 S 
 S 
```
Where A = Amethyst Shard, S = Stick

### 🌾 Scythe
Harvest tool with auto-replant functionality.

| Tier | Harvest Area | Effect |
|------|--------------|--------|
| **Iron Scythe** | 3×3 | Harvests ripe crops, replants from drops |
| **Diamond Scythe** | 5×5 | Harvests ripe crops, replants from drops |
| **Netherite Scythe** | 7×7 | Harvests ripe crops, replants from drops |

**Crafting** (Shaped Recipe):
```
MMM
 S
S
```
Where B = tier material (Iron/Diamond/Netherite Ingot), S = stick

**Supported Crops**:
- Wheat
- Carrots
- Potatoes
- Beetroots
- Nether Wart

Replant order: Uses harvested seeds first, then inventory fallback.

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Place `pepeforge-0.1.0.jar` in your server's `plugins/` folder
3. (Optional) Download the resource pack from [Releases](../../releases) for custom item models and textures
4. Restart server

## Configuration

Edit `plugins/PepeForge/config.yml` to customize:

- plugin message language
- client-side vs server-side item text translation mode
- enabling or disabling specific custom item recipes

## Commands

- `/pepeforge items` - Opens a menu to preview all items
- `/pepeforge give <item> <player>` - Gives an item to a player

**Available items**: `crescent_bow`, `crescent_spear`, `chisel`, `katana`, `iron_wind_blade`, `diamond_wind_blade`, `netherite_wind_blade`, `iron_scythe`, `diamond_scythe`, `netherite_scythe`

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
