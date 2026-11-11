package pepin.pepeforge.gui.itemconfig.crescentkhopesh;

import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.crescentkhopesh.CrescentKhopeshConfig.Setting;
import pepin.pepeforge.weapons.crescentkhopesh.MoonstrikeMode;

public final class CrescentKhopeshConfigListener {

    private final PepeForgePlugin plugin;

    public CrescentKhopeshConfigListener(PepeForgePlugin plugin) {
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
            plugin.getConfig().set(MoonstrikeMode.CONFIG_PATH, MoonstrikeMode.SWEEP.name());
            plugin.saveConfig();
            return true;
        }
        MoonstrikeMode mode = MoonstrikeMode.fromConfig(plugin.getConfig());
        if (event.getSlot() == 24) {
            plugin.getConfig().set(MoonstrikeMode.CONFIG_PATH,
                    (mode == MoonstrikeMode.SWEEP ? MoonstrikeMode.WAVE : MoonstrikeMode.SWEEP).name());
            plugin.saveConfig();
            return true;
        }
        for (Setting setting : Setting.values()) {
            if (event.getSlot() == setting.slot && setting.isAvailable(mode)) {
                double change = event.isLeftClick() ? -setting.step : setting.step;
                double next = Math.max(setting.min, Math.min(setting.max,
                        setting.current(plugin.getConfig()) + change));
                next = Math.round(next * 100.0D) / 100.0D;
                setting.save(plugin.getConfig(), next);
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }
}
