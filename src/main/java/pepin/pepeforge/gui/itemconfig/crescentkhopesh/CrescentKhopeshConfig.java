package pepin.pepeforge.gui.itemconfig.crescentkhopesh;

import java.util.ArrayList;
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
import pepin.pepeforge.weapons.crescentkhopesh.CrescentKhopeshDefinition;
import pepin.pepeforge.weapons.crescentkhopesh.MoonstrikeMode;

public final class CrescentKhopeshConfig {

    private final PepeForgePlugin plugin;

    public CrescentKhopeshConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        ItemStack reset = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = reset.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset Crescent Khopesh Defaults");
            resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset Moonstrike settings"));
            reset.setItemMeta(resetMeta);
        }
        inventory.setItem(18, reset);

        MoonstrikeMode mode = MoonstrikeMode.fromConfig(plugin.getConfig());
        inventory.setItem(23, null);
        for (Setting setting : Setting.values()) {
            if (!setting.isAvailable(mode)) {
                continue;
            }
            ItemStack button = new ItemStack(setting.icon);
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.GOLD + setting.label + ": " + ColorUtil.GREEN
                        + setting.format(setting.current(plugin.getConfig())));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.GRAY + "Range: " + setting.format(setting.min)
                        + " - " + setting.format(setting.max));
                lore.add(ColorUtil.GRAY + "Left-Click: -" + setting.format(setting.step));
                lore.add(ColorUtil.GRAY + "Right-Click: +" + setting.format(setting.step));
                if (setting == Setting.SHIELD_CAP) {
                    lore.add(ColorUtil.GRAY + "Each target grants 0.5 shield HP (0.25 heart)");
                    lore.add(ColorUtil.GRAY + "Expires 4 seconds after the last grant");
                }
                meta.setLore(lore);
                button.setItemMeta(meta);
            }
            inventory.setItem(setting.slot, button);
        }

        ItemStack modeButton = new ItemStack(ConfigIconography.TOGGLE);
        ItemMeta modeMeta = modeButton.getItemMeta();
        if (modeMeta != null) {
            modeMeta.setDisplayName(ColorUtil.GOLD + "Moonstrike Mode: " + ColorUtil.GREEN + mode.name());
            modeMeta.setLore(List.of(ColorUtil.GRAY + "Click to switch between SWEEP and WAVE",
                    ColorUtil.GRAY + "Each variant keeps its own dimensions"));
            modeButton.setItemMeta(modeMeta);
        }
        inventory.setItem(24, modeButton);
    }

    enum Setting {
        HITS(19, "hits_required", "Required Hits", ConfigIconography.CHARGES,
                CrescentKhopeshDefinition.DEFAULT_HITS_REQUIRED, 1, 100, 1, "hits", true),
        SHIELD_CAP(20, "shield_absorption", "Shield Limit", Material.GOLD_INGOT,
                CrescentKhopeshDefinition.DEFAULT_SHIELD_ABSORPTION, 0, 40, 1, "hearts", false),
        RANGE(21, "wave_range", "Wave Range", ConfigIconography.RANGE,
                CrescentKhopeshDefinition.DEFAULT_WAVE_RANGE, 0.1, 16, 0.1, "blocks", false),
        WIDTH(22, "wave_width", "Wave Width", ConfigIconography.RANGE,
                CrescentKhopeshDefinition.DEFAULT_WAVE_WIDTH, 0.1, 16, 0.1, "blocks", false),
        SPEED(23, "wave_speed", "Wave Speed", ConfigIconography.STRENGTH,
                CrescentKhopeshDefinition.DEFAULT_WAVE_SPEED, 0.05, 16, 0.05, "blocks/tick", false),
        SWEEP_RANGE(21, "sweep_range", "Sweep Forward Reach", ConfigIconography.RANGE,
                CrescentKhopeshDefinition.DEFAULT_SWEEP_RANGE, 0.1, 16, 0.1, "blocks", false),
        SWEEP_WIDTH(22, "sweep_width", "Sweep Width", ConfigIconography.RANGE,
                CrescentKhopeshDefinition.DEFAULT_SWEEP_WIDTH, 0.1, 16, 0.1, "blocks", false);

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
            this.path = "mechanics.crescent_khopesh." + key;
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
            double value = integer ? config.getInt(path, (int) defaultValue) : config.getDouble(path, defaultValue);
            return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : defaultValue;
        }

        boolean isAvailable(MoonstrikeMode mode) {
            return switch (this) {
                case RANGE, WIDTH, SPEED -> mode == MoonstrikeMode.WAVE;
                case SWEEP_RANGE, SWEEP_WIDTH -> mode == MoonstrikeMode.SWEEP;
                default -> true;
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
            if (this == SHIELD_CAP) {
                value /= 2.0D;
            }
            return (integer ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.2f", value))
                    + " " + unit;
        }
    }
}
