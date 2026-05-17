# PepeForge Items

Detailed item and recipe reference for the current PepeForge item set.

## 🪚 Chisel

Decorative utility tool for cycling carved block variants.

| Feature | Details |
|---------|---------|
| Use | Right-click a supported block to advance one decorative variant |
| Reverse | Sneak + right-click to cycle backward |
| Cost | 1 durability per successful conversion |
| Rarity | Common |

**Crafting** (Shaped Recipe):
```
 I 
 C 
 S 
```
Where I = Iron Ingot, C = Copper Ingot, S = Stick

## ⚔️ Wind Blade

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

## 🏹 Crescent Bow

Special ranged weapon that fires a 3-arrow volley.

| Feature | Details |
|---------|---------|
| Arrows | Fires 3 arrows in crescent spread (center + 2 sides) |
| Ammo | Consumes 1 arrow per volley |
| Rarity | Epic |

**Crafting** (Shaped Recipe):
```
 PSA
 P S
 PSA
```
Where P = Phantom Membrane, S = Stick, A = Amethyst Shard

## 🔱 Crescent Spear

Special melee weapon that launches enemies on every third hit.

| Feature | Details |
|---------|---------|
| Proc | Every 3rd hit deals bonus damage and launches the target upward |
| Buff | Grants Speed I for 15s after a launch |
| Rarity | Epic |

**Crafting** (Shaped Recipe):
```
 A 
 S 
 S 
```
Where A = Amethyst Shard, S = Stick

## 🗡️ Katana

Two-handed parry weapon built around short defensive timing windows.

| Feature | Details |
|---------|---------|
| Parry | Right-click enters a brief parry stance |
| Defense | Blocks melee and reflects projectiles during the active window |
| Restriction | Requires an empty off-hand for custom mechanics |
| Rarity | Rare |

**Crafting** (Shaped Recipe):
```
 I 
 I 
 S 
```
Where I = Iron Ingot, S = Stick

## 🌾 Scythe

Harvest tool with auto-replant functionality.

| Tier | Harvest Area | Effect |
|------|--------------|--------|
| **Iron Scythe** | 1×1 | Harvests ripe crops, replants from drops |
| **Diamond Scythe** | 3×3 | Harvests ripe crops, replants from drops |
| **Netherite Scythe** | 5×5 | Harvests ripe crops, replants from drops |

**Crafting** (Shaped Recipe):
```
MMM
 S 
S  
```
Where M = tier material (Iron/Diamond/Netherite Ingot), S = Stick

**Supported Crops**:
- Wheat
- Carrots
- Potatoes
- Beetroots
- Nether Wart

Replant order: Uses harvested seeds first, then inventory fallback.

## Give Names

Available internal item names for `/pepeforge give`:

- `crescent_bow`
- `crescent_spear`
- `chisel`
- `katana`
- `iron_wind_blade`
- `diamond_wind_blade`
- `netherite_wind_blade`
- `iron_scythe`
- `diamond_scythe`
- `netherite_scythe`
