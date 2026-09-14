package pepin.pepeforge.gui.itemconfig.stormcleaver;

import org.bukkit.event.inventory.InventoryClickEvent;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.stormcleaver.StormcleaverDefinition;

public class StormcleaverConfigListener {

    private final PepeForgePlugin plugin;

    public StormcleaverConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {

        boolean refresh = false;

        if (event.getSlot() == 18) {
                plugin.getConfig().set("mechanics.stormcleaver.charges_required", StormcleaverDefinition.DEFAULT_CHARGES_REQUIRED);
                plugin.getConfig().set("mechanics.stormcleaver.jump_velocity_multiplier",
                    StormcleaverDefinition.DEFAULT_JUMP_VELOCITY_MULTIPLIER);
                plugin.getConfig().set("mechanics.stormcleaver.charge_decay_interval",
                    StormcleaverDefinition.DEFAULT_CHARGE_DECAY_INTERVAL);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 19) {
                int current = plugin.getConfig().getInt("mechanics.stormcleaver.charges_required",
                    StormcleaverDefinition.DEFAULT_CHARGES_REQUIRED);
            int change = event.isLeftClick() ? -1 : 1;
            int newValue = Math.max(1, Math.min(10, current + change));
            plugin.getConfig().set("mechanics.stormcleaver.charges_required", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 20) {
                double current = plugin.getConfig().getDouble("mechanics.stormcleaver.jump_velocity_multiplier",
                    StormcleaverDefinition.DEFAULT_JUMP_VELOCITY_MULTIPLIER);
            double change = event.isLeftClick() ? -0.1D : 0.1D;
            double newValue = Math.max(0.1D, Math.min(5.0D, current + change));
            plugin.getConfig().set("mechanics.stormcleaver.jump_velocity_multiplier", newValue);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 21) {
                int current = plugin.getConfig().getInt("mechanics.stormcleaver.charge_decay_interval",
                    StormcleaverDefinition.DEFAULT_CHARGE_DECAY_INTERVAL);
            int change = event.isLeftClick() ? -10 : 10;
            int newValue = Math.max(10, Math.min(1200, current + change));
            plugin.getConfig().set("mechanics.stormcleaver.charge_decay_interval", newValue);
            plugin.saveConfig();
            refresh = true;
        }

        return refresh;
    }
}
