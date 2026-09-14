package pepin.pepeforge.gui.itemconfig.windblade;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

import java.util.List;

public class WindBladeConfig {

    private final PepeForgePlugin plugin;

    public WindBladeConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {

        ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetBtn.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset Wind Blade Defaults");
            resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset shared dash settings"));
            resetBtn.setItemMeta(resetMeta);
        }
        inventory.setItem(18, resetBtn);

        ItemStack cooldownBtn = new ItemStack(ConfigIconography.COOLDOWN);
        ItemMeta cooldownMeta = cooldownBtn.getItemMeta();
        if (cooldownMeta != null) {
                double cooldown = plugin.getConfig().getLong("mechanics.wind_blade.dash_cooldown",
                    WindBladeTier.DEFAULT_DASH_COOLDOWN_MILLIS) / 1000.0D;
            cooldownMeta.setDisplayName(ColorUtil.GOLD + "Dash Cooldown: " + ColorUtil.GREEN + cooldown + "s");
            cooldownMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s",
                    ColorUtil.GRAY + "Right-Click: +0.5s"));
            cooldownBtn.setItemMeta(cooldownMeta);
        }
        inventory.setItem(19, cooldownBtn);

        ItemStack strengthBtn = new ItemStack(ConfigIconography.STRENGTH);
        ItemMeta strengthMeta = strengthBtn.getItemMeta();
        if (strengthMeta != null) {
                double strength = plugin.getConfig().getDouble("mechanics.wind_blade.dash_strength",
                    WindBladeTier.DEFAULT_DASH_STRENGTH);
            strengthMeta.setDisplayName(ColorUtil.GOLD + "Dash Strength: " + ColorUtil.GREEN + strength);
            strengthMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.1",
                    ColorUtil.GRAY + "Right-Click: +0.1"));
            strengthBtn.setItemMeta(strengthMeta);
        }
        inventory.setItem(20, strengthBtn);

        ItemStack glidingBtn = new ItemStack(ConfigIconography.TOGGLE);
        ItemMeta glidingMeta = glidingBtn.getItemMeta();
        if (glidingMeta != null) {
                boolean enabled = plugin.getConfig().getBoolean("mechanics.wind_blade.dash_while_gliding",
                    WindBladeTier.DEFAULT_DASH_WHILE_GLIDING);
            glidingMeta.setDisplayName(ColorUtil.GOLD + "Dash While Gliding: "
                    + (enabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
            glidingMeta.setLore(List.of(ColorUtil.GRAY + "Click to toggle Elytra dashing"));
            glidingBtn.setItemMeta(glidingMeta);
        }
        inventory.setItem(21, glidingBtn);
    }
}
