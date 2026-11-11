package pepin.pepeforge.gui.itemconfig.rapier;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.rapier.IceRapierConfig.Setting;

public final class IceRapierConfigListener {

    private final PepeForgePlugin plugin;

    public IceRapierConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (!event.isLeftClick() && !event.isRightClick()) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        if (event.getSlot() == 18) {
            for (Setting setting : Setting.values()) {
                setting.save(config, setting.defaultValue);
            }
            plugin.saveConfig();
            return true;
        }
        for (Setting setting : Setting.values()) {
            if (event.getSlot() == setting.slot) {
                double minimum = setting == Setting.MAX_DAMAGE ? Setting.MIN_DAMAGE.current(config) : setting.min;
                double maximum = setting == Setting.MIN_DAMAGE ? Setting.MAX_DAMAGE.current(config) : setting.max;
                double next = Math.max(minimum, Math.min(maximum,
                        setting.current(config) + (event.isLeftClick() ? -1 : 1)));
                if (setting == Setting.MIN_DAMAGE || setting == Setting.MAX_DAMAGE) {
                    double currentMin = Setting.MIN_DAMAGE.current(config);
                    double currentMax = Setting.MAX_DAMAGE.current(config);
                    Setting.MIN_DAMAGE.save(config, currentMin);
                    Setting.MAX_DAMAGE.save(config, currentMax);
                }
                setting.save(config, next);
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }
}
