package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.configuration.file.FileConfiguration;

public record ObsidianGlaiveSettings(double damage, boolean stabEnabled, boolean stabLiftEnabled) {
    public static final double DEFAULT_DAMAGE = 7.0D;
    public static final double MIN_DAMAGE = 1.0D;
    public static final double MAX_DAMAGE = 40.0D;
    public static final boolean DEFAULT_STAB_ENABLED = true;
    public static final boolean DEFAULT_STAB_LIFT_ENABLED = true;
    private static final String PREFIX = "mechanics.obsidian_glaive.";

    public static ObsidianGlaiveSettings fromConfig(FileConfiguration config) {
        double value = config.getDouble(PREFIX + "damage", DEFAULT_DAMAGE);
        double damage = Double.isFinite(value) ? Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, value)) : DEFAULT_DAMAGE;
        return new ObsidianGlaiveSettings(damage,
                config.getBoolean(PREFIX + "stab_enabled", DEFAULT_STAB_ENABLED),
                config.getBoolean(PREFIX + "stab_lift_enabled", DEFAULT_STAB_LIFT_ENABLED));
    }
}
