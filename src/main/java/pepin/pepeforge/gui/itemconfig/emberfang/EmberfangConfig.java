package pepin.pepeforge.gui.itemconfig.emberfang;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.emberfang.EmberfangDefinition;

public final class EmberfangConfig {

    private final PepeForgePlugin plugin;

    public EmberfangConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        ItemStack resetButton = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetButton.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset Emberfang Defaults");
            resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset bonus fire damage"));
            resetButton.setItemMeta(resetMeta);
        }
        inventory.setItem(18, resetButton);

        ItemStack damageButton = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta damageMeta = damageButton.getItemMeta();
        if (damageMeta != null) {
            double fireDamage = plugin.getConfig().getDouble(
                    EmberfangDefinition.FIRE_DAMAGE_CONFIG_PATH,
                    EmberfangDefinition.FIRE_DAMAGE);
            damageMeta.setDisplayName(ColorUtil.GOLD + "Bonus Fire Damage: "
                    + ColorUtil.GREEN + formatDamage(fireDamage));
            damageMeta.setLore(List.of(
                    ColorUtil.GRAY + "Range: " + formatDamage(EmberfangDefinition.MIN_FIRE_DAMAGE)
                            + "-" + formatDamage(EmberfangDefinition.MAX_FIRE_DAMAGE),
                    ColorUtil.GRAY + "Left-Click: -" + formatDamage(EmberfangDefinition.FIRE_DAMAGE_STEP),
                    ColorUtil.GRAY + "Right-Click: +" + formatDamage(EmberfangDefinition.FIRE_DAMAGE_STEP)));
            damageButton.setItemMeta(damageMeta);
        }
        inventory.setItem(19, damageButton);
    }

    private static String formatDamage(double damage) {
        return String.format(java.util.Locale.ROOT, "%.1f", damage);
    }
}
