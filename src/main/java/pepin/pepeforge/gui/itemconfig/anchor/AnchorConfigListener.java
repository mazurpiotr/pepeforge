package pepin.pepeforge.gui.itemconfig.anchor;

import org.bukkit.event.inventory.InventoryClickEvent;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.anchor.AnchorDefinition;

public class AnchorConfigListener {

    private final PepeForgePlugin plugin;

    public AnchorConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {

        boolean refresh = false;

        if (event.getSlot() == 18) {
            plugin.getConfig().set("mechanics.anchor.ability_cooldown", AnchorDefinition.DEFAULT_ABILITY_COOLDOWN_MILLIS);
            plugin.getConfig().set("mechanics.anchor.snare_duration", AnchorDefinition.DEFAULT_SNARE_DURATION_TICKS);
            plugin.getConfig().set("mechanics.anchor.snare_cooldown", AnchorDefinition.DEFAULT_SNARE_COOLDOWN_MILLIS);
            plugin.getConfig().set("mechanics.anchor.ability_range", AnchorDefinition.DEFAULT_ABILITY_RANGE);
            plugin.getConfig().set("mechanics.anchor.snare_enabled", AnchorDefinition.DEFAULT_SNARE_ENABLED);
            plugin.getConfig().set("mechanics.anchor.hook_enabled", AnchorDefinition.DEFAULT_HOOK_ENABLED);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 19) {
            boolean current = plugin.getConfig().getBoolean("mechanics.anchor.hook_enabled", AnchorDefinition.DEFAULT_HOOK_ENABLED);
            plugin.getConfig().set("mechanics.anchor.hook_enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 20) {
            long current = plugin.getConfig().getLong("mechanics.anchor.ability_cooldown", AnchorDefinition.DEFAULT_ABILITY_COOLDOWN_MILLIS);
            long change = event.isLeftClick() ? -500L : 500L;
            long newValue = Math.max(500L, Math.min(30000L, current + change));
            plugin.getConfig().set("mechanics.anchor.ability_cooldown", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 21) {
            double current = plugin.getConfig().getDouble("mechanics.anchor.ability_range", AnchorDefinition.DEFAULT_ABILITY_RANGE);
            double change = event.isLeftClick() ? -1.0D : 1.0D;
            double newValue = Math.max(5.0D, Math.min(50.0D, current + change));
            plugin.getConfig().set("mechanics.anchor.ability_range", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 23) {
            boolean current = plugin.getConfig().getBoolean("mechanics.anchor.snare_enabled", AnchorDefinition.DEFAULT_SNARE_ENABLED);
            plugin.getConfig().set("mechanics.anchor.snare_enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 24) {
            int current = plugin.getConfig().getInt("mechanics.anchor.snare_duration", AnchorDefinition.DEFAULT_SNARE_DURATION_TICKS);
            int change = event.isLeftClick() ? -10 : 10;
            int newValue = Math.max(10, Math.min(200, current + change));
            plugin.getConfig().set("mechanics.anchor.snare_duration", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 25) {
            long current = plugin.getConfig().getLong("mechanics.anchor.snare_cooldown", AnchorDefinition.DEFAULT_SNARE_COOLDOWN_MILLIS);
            long change = event.isLeftClick() ? -1000L : 1000L;
            long newValue = Math.max(1000L, Math.min(60000L, current + change));
            plugin.getConfig().set("mechanics.anchor.snare_cooldown", newValue);
            plugin.saveConfig();
            refresh = true;
        }

        return refresh;
    }
}
