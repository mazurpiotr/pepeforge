package pepin.pepeforge.gui.itemconfig.greatsword;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.greatsword.GreatswordDefinition;

import java.util.List;
import java.util.Locale;

public final class GreatswordConfig {

    private final PepeForgePlugin plugin;

    public GreatswordConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        ItemStack resetButton = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetButton.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset Greatsword Defaults");
            resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset shared momentum effects"));
            resetButton.setItemMeta(resetMeta);
        }
        inventory.setItem(18, resetButton);

        boolean projectileImmunity = plugin.getConfig().getBoolean(
                "mechanics.greatsword.projectile_knockback_immunity",
                GreatswordDefinition.DEFAULT_PROJECTILE_KNOCKBACK_IMMUNITY);
        inventory.setItem(19, createToggleButton(
                "Projectile Knockback Immunity", projectileImmunity));

        boolean microDash = plugin.getConfig().getBoolean(
                "mechanics.greatsword.micro_dash_enabled",
                GreatswordDefinition.DEFAULT_MICRO_DASH_ENABLED);
        inventory.setItem(20, createToggleButton("Hit Micro Dash", microDash));

        double strength = plugin.getConfig().getDouble(
                "mechanics.greatsword.micro_dash_strength",
                GreatswordDefinition.DEFAULT_MICRO_DASH_STRENGTH);
        ItemStack strengthButton = new ItemStack(ConfigIconography.STRENGTH);
        ItemMeta strengthMeta = strengthButton.getItemMeta();
        if (strengthMeta != null) {
            strengthMeta.setDisplayName(ColorUtil.GOLD + "Micro Dash Strength: " + ColorUtil.GREEN
                    + String.format(Locale.ROOT, "%.2f", strength));
            strengthMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.05",
                    ColorUtil.GRAY + "Right-Click: +0.05"));
            strengthButton.setItemMeta(strengthMeta);
        }
        inventory.setItem(21, strengthButton);
    }

    private ItemStack createToggleButton(String name, boolean enabled) {
        ItemStack button = new ItemStack(ConfigIconography.TOGGLE);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.GOLD + name + ": "
                    + (enabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
            meta.setLore(List.of(ColorUtil.GRAY + "Click to toggle"));
            button.setItemMeta(meta);
        }
        return button;
    }
}
