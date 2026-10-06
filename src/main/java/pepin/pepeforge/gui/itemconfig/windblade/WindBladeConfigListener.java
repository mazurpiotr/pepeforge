package pepin.pepeforge.gui.itemconfig.windblade;

import org.bukkit.event.inventory.InventoryClickEvent;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

public class WindBladeConfigListener {

    private final PepeForgePlugin plugin;

    public WindBladeConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {

        boolean refresh = false;
        if (event.getSlot() == 18) {
                plugin.getConfig().set("mechanics.wind_blade.dash_cooldown",
                    WindBladeTier.DEFAULT_DASH_COOLDOWN_MILLIS);
                plugin.getConfig().set("mechanics.wind_blade.dash_strength", WindBladeTier.DEFAULT_DASH_STRENGTH);
                plugin.getConfig().set("mechanics.wind_blade.dash_while_gliding",
                    WindBladeTier.DEFAULT_DASH_WHILE_GLIDING);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 19) {
                long current = plugin.getConfig().getLong("mechanics.wind_blade.dash_cooldown",
                    WindBladeTier.DEFAULT_DASH_COOLDOWN_MILLIS);
            long change = event.isLeftClick() ? -500L : 500L;
            long newValue = Math.max(500L, Math.min(30000L, current + change));
            plugin.getConfig().set("mechanics.wind_blade.dash_cooldown", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 20) {
                double current = plugin.getConfig().getDouble("mechanics.wind_blade.dash_strength",
                    WindBladeTier.DEFAULT_DASH_STRENGTH);
            double change = event.isLeftClick() ? -0.1D : 0.1D;
            double newValue = Math.max(0.1D, Math.min(5.0D, current + change));
            plugin.getConfig().set("mechanics.wind_blade.dash_strength", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 21) {
                boolean current = plugin.getConfig().getBoolean("mechanics.wind_blade.dash_while_gliding",
                    WindBladeTier.DEFAULT_DASH_WHILE_GLIDING);
            plugin.getConfig().set("mechanics.wind_blade.dash_while_gliding", !current);
            plugin.saveConfig();
            refresh = true;
        }
        return refresh;
    }
}