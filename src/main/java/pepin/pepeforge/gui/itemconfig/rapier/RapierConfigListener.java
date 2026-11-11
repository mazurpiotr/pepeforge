package pepin.pepeforge.gui.itemconfig.rapier;

import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.rapier.RapierConfig.Setting;
import pepin.pepeforge.weapons.rapier.RapierSettings;

public final class RapierConfigListener {

    private final PepeForgePlugin plugin;

    public RapierConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (!event.isLeftClick() && !event.isRightClick()) {
            return false;
        }
        if (event.getSlot() == 18) {
            for (Setting setting : Setting.values()) {
                setting.save(plugin.getConfig(), setting.defaultValue);
            }
            plugin.getConfig().set(RapierConfig.BONUS_ENABLED_PATH, RapierSettings.DEFAULT_FRONTAL_BONUS_ENABLED);
            plugin.saveConfig();
            return true;
        }
        if (event.getSlot() == 13) {
            boolean current = plugin.getConfig().getBoolean(RapierConfig.BONUS_ENABLED_PATH,
                    RapierSettings.DEFAULT_FRONTAL_BONUS_ENABLED);
            plugin.getConfig().set(RapierConfig.BONUS_ENABLED_PATH, !current);
            plugin.saveConfig();
            return true;
        }
        for (Setting setting : Setting.values()) {
            if (event.getSlot() == setting.slot) {
                double change = event.isLeftClick() ? -setting.step : setting.step;
                double next = Math.max(setting.min, Math.min(setting.max,
                        setting.current(plugin.getConfig()) + change));
                setting.save(plugin.getConfig(), Math.round(next * 100.0D) / 100.0D);
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }
}
