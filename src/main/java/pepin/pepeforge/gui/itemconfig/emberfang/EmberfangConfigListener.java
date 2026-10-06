package pepin.pepeforge.gui.itemconfig.emberfang;

import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.emberfang.EmberfangDefinition;

public final class EmberfangConfigListener {

    private final PepeForgePlugin plugin;

    public EmberfangConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {
        if (event.getSlot() == 18) {
            plugin.getConfig().set(
                    EmberfangDefinition.FIRE_DAMAGE_CONFIG_PATH,
                    EmberfangDefinition.FIRE_DAMAGE);
        } else if (event.getSlot() == 19) {
            double current = plugin.getConfig().getDouble(
                    EmberfangDefinition.FIRE_DAMAGE_CONFIG_PATH,
                    EmberfangDefinition.FIRE_DAMAGE);
            if (!Double.isFinite(current)) {
                current = EmberfangDefinition.FIRE_DAMAGE;
            }
            double change = event.isLeftClick()
                    ? -EmberfangDefinition.FIRE_DAMAGE_STEP
                    : EmberfangDefinition.FIRE_DAMAGE_STEP;
            double next = Math.max(
                    EmberfangDefinition.MIN_FIRE_DAMAGE,
                    Math.min(EmberfangDefinition.MAX_FIRE_DAMAGE, current + change));
            next = Math.round(next / EmberfangDefinition.FIRE_DAMAGE_STEP)
                    * EmberfangDefinition.FIRE_DAMAGE_STEP;
            plugin.getConfig().set(EmberfangDefinition.FIRE_DAMAGE_CONFIG_PATH, next);
        } else {
            return false;
        }

        plugin.saveConfig();
        return true;
    }
}
