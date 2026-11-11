package pepin.pepeforge.gui.itemconfig.rapier;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.rapier.RapierSettings;

public final class RapierConfig {

    static final String BONUS_ENABLED_PATH = "mechanics.rapier.frontal_bonus_enabled";
    private final PepeForgePlugin plugin;

    public RapierConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        inventory.setItem(18, button(ConfigIconography.RESET, ColorUtil.RED + "Reset Rapier Defaults",
                List.of(ColorUtil.GRAY + "Reset shared settings for all three tiers")));
        boolean bonusEnabled = plugin.getConfig().getBoolean(BONUS_ENABLED_PATH,
                RapierSettings.DEFAULT_FRONTAL_BONUS_ENABLED);
        inventory.setItem(13, button(ConfigIconography.createStateButton(bonusEnabled),
                ColorUtil.GOLD + "Frontal Bonus: " + ColorUtil.GREEN + bonusEnabled,
                List.of(ColorUtil.GRAY + "Click to toggle the optional frontal damage bonus")));
        for (Setting setting : Setting.values()) {
            inventory.setItem(setting.slot, button(setting.icon,
                    ColorUtil.GOLD + setting.label + ": " + ColorUtil.GREEN
                            + setting.format(setting.current(plugin.getConfig())),
                    List.of(ColorUtil.GRAY + "Rapier family configuration",
                            ColorUtil.GRAY + "Range: " + setting.format(setting.min) + " - " + setting.format(setting.max),
                            ColorUtil.GRAY + "Left-Click: -" + setting.format(setting.step),
                            ColorUtil.GRAY + "Right-Click: +" + setting.format(setting.step))));
        }
    }

    private ItemStack button(Material material, String title, List<String> lore) {
        return button(new ItemStack(material), title, lore);
    }

    private ItemStack button(ItemStack button, String title, List<String> lore) {
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    enum Setting {
        IRON_DAMAGE(19, "damage.iron", "Iron Damage", Material.IRON_INGOT,
                RapierSettings.DEFAULT_IRON_DAMAGE, RapierSettings.MIN_DAMAGE, RapierSettings.MAX_DAMAGE, 1, "HP", false),
        DIAMOND_DAMAGE(20, "damage.diamond", "Diamond Damage", Material.DIAMOND,
                RapierSettings.DEFAULT_DIAMOND_DAMAGE, RapierSettings.MIN_DAMAGE, RapierSettings.MAX_DAMAGE, 1, "HP", false),
        NETHERITE_DAMAGE(21, "damage.netherite", "Netherite Damage", Material.NETHERITE_INGOT,
                RapierSettings.DEFAULT_NETHERITE_DAMAGE, RapierSettings.MIN_DAMAGE, RapierSettings.MAX_DAMAGE, 1, "HP", false),
        SPEED(22, "attack_speed", "Attack Speed", ConfigIconography.STRENGTH,
                RapierSettings.DEFAULT_ATTACK_SPEED, RapierSettings.MIN_ATTACK_SPEED,
                RapierSettings.MAX_ATTACK_SPEED, 0.1, "attacks/s", false),
        BONUS(24, "frontal_bonus_percent", "Frontal Damage Bonus", ConfigIconography.STRENGTH,
                RapierSettings.DEFAULT_FRONTAL_BONUS_PERCENT, RapierSettings.MIN_FRONTAL_BONUS_PERCENT,
                RapierSettings.MAX_FRONTAL_BONUS_PERCENT, 1, "%", false),
        CONE(25, "frontal_cone_degrees", "Frontal Cone", ConfigIconography.RANGE,
                RapierSettings.DEFAULT_FRONTAL_CONE_DEGREES, RapierSettings.MIN_FRONTAL_CONE_DEGREES,
                RapierSettings.MAX_FRONTAL_CONE_DEGREES, 1, "degrees", false);

        final int slot;
        final String path;
        final String label;
        final Material icon;
        final double defaultValue;
        final double min;
        final double max;
        final double step;
        final String unit;
        final boolean integer;

        Setting(int slot, String key, String label, Material icon, double defaultValue,
                double min, double max, double step, String unit, boolean integer) {
            this.slot = slot;
            this.path = "mechanics.rapier." + key;
            this.label = label;
            this.icon = icon;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.step = step;
            this.unit = unit;
            this.integer = integer;
        }

        double current(FileConfiguration config) {
            double value = config.getDouble(path, defaultValue);
            double bounded = Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : defaultValue;
            return integer ? (int) bounded : bounded;
        }

        void save(FileConfiguration config, double value) {
            if (integer) {
                config.set(path, (int) value);
            } else {
                config.set(path, value);
            }
        }

        String format(double value) {
            return (integer ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.2f", value)) + " " + unit;
        }
    }
}
