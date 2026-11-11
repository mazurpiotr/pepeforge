package pepin.pepeforge.gui.itemconfig.obsidianglaive;

import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.obsidianglaive.ObsidianGlaiveSettings;

public final class ObsidianGlaiveConfigListener {

    private static final String PATH = "mechanics.obsidian_glaive.";
    private final PepeForgePlugin plugin;

    public ObsidianGlaiveConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (event.getSlot() == 18) {
            ObsidianGlaiveSettings settings = ObsidianGlaiveSettings.fromConfig(plugin.getConfig());
            boolean changed = Double.compare(plugin.getConfig().getDouble(PATH + "damage",
                            ObsidianGlaiveSettings.DEFAULT_DAMAGE), ObsidianGlaiveSettings.DEFAULT_DAMAGE) != 0
                    || settings.stabEnabled() != ObsidianGlaiveSettings.DEFAULT_STAB_ENABLED
                    || settings.stabLiftEnabled() != ObsidianGlaiveSettings.DEFAULT_STAB_LIFT_ENABLED;
            if (!changed) {
                return false;
            }
            plugin.getConfig().set(PATH + "damage", ObsidianGlaiveSettings.DEFAULT_DAMAGE);
            plugin.getConfig().set(PATH + "stab_enabled", ObsidianGlaiveSettings.DEFAULT_STAB_ENABLED);
            plugin.getConfig().set(PATH + "stab_lift_enabled",
                    ObsidianGlaiveSettings.DEFAULT_STAB_LIFT_ENABLED);
        } else if (event.getSlot() == 19 && (event.isLeftClick() || event.isRightClick())) {
            double current = ObsidianGlaiveSettings.fromConfig(plugin.getConfig()).damage();
            double change = event.isLeftClick() ? -0.5 : 0.5;
            double next = Math.max(ObsidianGlaiveSettings.MIN_DAMAGE,
                    Math.min(ObsidianGlaiveSettings.MAX_DAMAGE, current + change));
            next = Math.round(next * 2.0) / 2.0;
            if (Double.compare(current, next) == 0) {
                return false;
            }
            plugin.getConfig().set(PATH + "damage", next);
        } else if (event.getSlot() == 20) {
            plugin.getConfig().set(PATH + "stab_enabled", !plugin.getConfig().getBoolean(PATH + "stab_enabled",
                    ObsidianGlaiveSettings.DEFAULT_STAB_ENABLED));
        } else if (event.getSlot() == 21) {
            plugin.getConfig().set(PATH + "stab_lift_enabled",
                    !plugin.getConfig().getBoolean(PATH + "stab_lift_enabled",
                            ObsidianGlaiveSettings.DEFAULT_STAB_LIFT_ENABLED));
        } else {
            return false;
        }
        plugin.saveConfig();
        return true;
    }
}
