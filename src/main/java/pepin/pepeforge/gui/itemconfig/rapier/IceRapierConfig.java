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
import pepin.pepeforge.weapons.rapier.IceRapierSettings;

public final class IceRapierConfig {

    private final PepeForgePlugin plugin;

    public IceRapierConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        inventory.setItem(18, button(ConfigIconography.RESET, ColorUtil.RED + "Reset Ice Rapier Defaults",
                List.of(ColorUtil.GRAY + "Reset only Ice Rapier settings",
                        ColorUtil.GRAY + "Reload the plugin to apply changes")));
        for (Setting setting : Setting.values()) {
            inventory.setItem(setting.slot, button(setting.icon,
                    ColorUtil.GOLD + setting.label + ": " + ColorUtil.GREEN
                            + setting.format(setting.current(plugin.getConfig())),
                    List.of(ColorUtil.GRAY + setting.description,
                            ColorUtil.GRAY + "Range: " + setting.format(setting.min) + " - " + setting.format(setting.max),
                            ColorUtil.GRAY + "Left-Click: -1 | Right-Click: +1",
                            ColorUtil.GRAY + "Reload the plugin to apply changes")));
        }
    }

    private ItemStack button(Material material, String title, List<String> lore) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    enum Setting {
        MIN_DAMAGE(19, "min_damage", "Minimum Damage", Material.IRON_INGOT,
                IceRapierSettings.DEFAULT_MIN_DAMAGE, IceRapierSettings.MIN_DAMAGE,
                IceRapierSettings.MAX_DAMAGE, false, "HP", "Damage near the end of the blade's durability"),
        MAX_DAMAGE(20, "max_damage", "Maximum Damage", Material.DIAMOND,
                IceRapierSettings.DEFAULT_MAX_DAMAGE, IceRapierSettings.MIN_DAMAGE,
                IceRapierSettings.MAX_DAMAGE, false, "HP", "Damage when the blade is fully repaired"),
        DURABILITY(21, "max_durability", "Maximum Durability", Material.ANVIL,
                IceRapierSettings.DEFAULT_MAX_DURABILITY, IceRapierSettings.MIN_DURABILITY,
                IceRapierSettings.MAX_DURABILITY, true, "points", "Custom maximum blade durability"),
        FREEZE(22, "freeze_ticks", "Freeze Meter", Material.POWDER_SNOW_BUCKET,
                IceRapierSettings.DEFAULT_FREEZE_TICKS, IceRapierSettings.MIN_FREEZE_TICKS,
                IceRapierSettings.MAX_FREEZE_TICKS, true, "ticks", "Both modes; naturally decays, not a duration");

        final int slot;
        final String path;
        final String label;
        final Material icon;
        final double defaultValue;
        final double min;
        final double max;
        final boolean integer;
        final String unit;
        final String description;

        Setting(int slot, String key, String label, Material icon, double defaultValue,
                double min, double max, boolean integer, String unit, String description) {
            this.slot = slot;
            this.path = "mechanics.ice_rapier." + key;
            this.label = label;
            this.icon = icon;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.unit = unit;
            this.description = description;
        }

        double current(FileConfiguration config) {
            IceRapierSettings settings = IceRapierSettings.fromConfig(config);
            return switch (this) {
                case MIN_DAMAGE -> settings.minDamage();
                case MAX_DAMAGE -> settings.maxDamage();
                case DURABILITY -> settings.maxDurability();
                case FREEZE -> settings.freezeTicks();
            };
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
