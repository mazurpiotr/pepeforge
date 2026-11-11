package pepin.pepeforge.weapons.rapier;

import org.bukkit.configuration.file.FileConfiguration;

public record IceRapierSettings(double minDamage, double maxDamage, int maxDurability, int freezeTicks) {

    public static final double DEFAULT_MIN_DAMAGE = 4.0D;
    public static final double DEFAULT_MAX_DAMAGE = 8.0D;
    public static final int DEFAULT_MAX_DURABILITY = 32;
    public static final int DEFAULT_FREEZE_TICKS = 200;
    public static final double MIN_DAMAGE = 1.0D;
    public static final double MAX_DAMAGE = 40.0D;
    public static final int MIN_DURABILITY = 1;
    public static final int MAX_DURABILITY = 250;
    public static final int MIN_FREEZE_TICKS = 1;
    public static final int MAX_FREEZE_TICKS = 600;
    private static final String PREFIX = "mechanics.ice_rapier.";

    public static IceRapierSettings fromConfig(FileConfiguration config) {
        double minimum = bounded(config, "min_damage", DEFAULT_MIN_DAMAGE, MIN_DAMAGE, MAX_DAMAGE);
        double maximum = Math.max(minimum,
                bounded(config, "max_damage", DEFAULT_MAX_DAMAGE, MIN_DAMAGE, MAX_DAMAGE));
        return new IceRapierSettings(minimum, maximum,
                (int) bounded(config, "max_durability", DEFAULT_MAX_DURABILITY, MIN_DURABILITY, MAX_DURABILITY),
                (int) bounded(config, "freeze_ticks", DEFAULT_FREEZE_TICKS, MIN_FREEZE_TICKS, MAX_FREEZE_TICKS));
    }

    public double attackDamage(int damage, int durability) {
        double remaining = 1.0D - Math.max(0, damage) / (double) Math.max(1, durability);
        return minDamage + (maxDamage - minDamage) * Math.max(0.0D, Math.min(1.0D, remaining));
    }

    private static double bounded(FileConfiguration config, String key, double fallback, double min, double max) {
        double value = config.getDouble(PREFIX + key, fallback);
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
