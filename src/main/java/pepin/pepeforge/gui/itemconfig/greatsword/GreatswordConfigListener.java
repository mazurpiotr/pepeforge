package pepin.pepeforge.gui.itemconfig.greatsword;

import org.bukkit.event.inventory.InventoryClickEvent;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.weapons.greatsword.GreatswordDefinition;

public final class GreatswordConfigListener {

    private final PepeForgePlugin plugin;

    public GreatswordConfigListener(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleClick(InventoryClickEvent event) {
        String path = "mechanics.greatsword.";
        switch (event.getSlot()) {
            case 18 -> {
                plugin.getConfig().set(path + "projectile_knockback_immunity",
                        GreatswordDefinition.DEFAULT_PROJECTILE_KNOCKBACK_IMMUNITY);
                plugin.getConfig().set(path + "micro_dash_enabled",
                        GreatswordDefinition.DEFAULT_MICRO_DASH_ENABLED);
                plugin.getConfig().set(path + "micro_dash_strength",
                        GreatswordDefinition.DEFAULT_MICRO_DASH_STRENGTH);
            }
            case 19 -> plugin.getConfig().set(path + "projectile_knockback_immunity",
                    !plugin.getConfig().getBoolean(path + "projectile_knockback_immunity",
                            GreatswordDefinition.DEFAULT_PROJECTILE_KNOCKBACK_IMMUNITY));
            case 20 -> plugin.getConfig().set(path + "micro_dash_enabled",
                    !plugin.getConfig().getBoolean(path + "micro_dash_enabled",
                            GreatswordDefinition.DEFAULT_MICRO_DASH_ENABLED));
            case 21 -> {
                double current = plugin.getConfig().getDouble(path + "micro_dash_strength",
                        GreatswordDefinition.DEFAULT_MICRO_DASH_STRENGTH);
                double change = event.isLeftClick()
                        ? -GreatswordDefinition.MICRO_DASH_STRENGTH_STEP
                        : GreatswordDefinition.MICRO_DASH_STRENGTH_STEP;
                double next = Math.max(GreatswordDefinition.MIN_MICRO_DASH_STRENGTH,
                        Math.min(GreatswordDefinition.MAX_MICRO_DASH_STRENGTH, current + change));
                plugin.getConfig().set(path + "micro_dash_strength", next);
            }
            default -> {
                return false;
            }
        }
        plugin.saveConfig();
        return true;
    }
}
