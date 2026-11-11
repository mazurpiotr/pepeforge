package pepin.pepeforge.weapons.rapier;

import org.bukkit.configuration.file.FileConfiguration;

public record RapierSettings(double ironDamage, double diamondDamage, double netheriteDamage,
        double attackSpeed, boolean frontalBonusEnabled,
        double frontalBonusPercent, double frontalConeDegrees) {

    public static final double DEFAULT_IRON_DAMAGE = 5.0D;
    public static final double DEFAULT_DIAMOND_DAMAGE = 6.0D;
    public static final double DEFAULT_NETHERITE_DAMAGE = 7.0D;
    public static final double MIN_DAMAGE = 1.0D;
    public static final double MAX_DAMAGE = 40.0D;
    public static final double DEFAULT_ATTACK_SPEED = 2.0D;
    public static final double MIN_ATTACK_SPEED = 0.5D;
    public static final double MAX_ATTACK_SPEED = 4.0D;
    public static final boolean DEFAULT_FRONTAL_BONUS_ENABLED = false;
    public static final double DEFAULT_FRONTAL_BONUS_PERCENT = 20.0D;
    public static final double MIN_FRONTAL_BONUS_PERCENT = 0.0D;
    public static final double MAX_FRONTAL_BONUS_PERCENT = 100.0D;
    public static final double DEFAULT_FRONTAL_CONE_DEGREES = 120.0D;
    public static final double MIN_FRONTAL_CONE_DEGREES = 1.0D;
    public static final double MAX_FRONTAL_CONE_DEGREES = 180.0D;
    private static final String PREFIX = "mechanics.rapier.";

    public static RapierSettings fromConfig(FileConfiguration config) {
        return new RapierSettings(
                bounded(config, "damage.iron", DEFAULT_IRON_DAMAGE, MIN_DAMAGE, MAX_DAMAGE),
                bounded(config, "damage.diamond", DEFAULT_DIAMOND_DAMAGE, MIN_DAMAGE, MAX_DAMAGE),
                bounded(config, "damage.netherite", DEFAULT_NETHERITE_DAMAGE, MIN_DAMAGE, MAX_DAMAGE),
                bounded(config, "attack_speed", DEFAULT_ATTACK_SPEED, MIN_ATTACK_SPEED, MAX_ATTACK_SPEED),
                config.getBoolean(PREFIX + "frontal_bonus_enabled", DEFAULT_FRONTAL_BONUS_ENABLED),
                bounded(config, "frontal_bonus_percent", DEFAULT_FRONTAL_BONUS_PERCENT,
                        MIN_FRONTAL_BONUS_PERCENT, MAX_FRONTAL_BONUS_PERCENT),
                bounded(config, "frontal_cone_degrees", DEFAULT_FRONTAL_CONE_DEGREES,
                        MIN_FRONTAL_CONE_DEGREES, MAX_FRONTAL_CONE_DEGREES));
    }

    public double attackDamage(RapierTier tier) {
        return switch (tier) {
            case IRON -> ironDamage;
            case DIAMOND -> diamondDamage;
            case NETHERITE -> netheriteDamage;
            case ICE -> IceRapierSettings.DEFAULT_MAX_DAMAGE;
        };
    }

    private static double bounded(FileConfiguration config, String path, double fallback, double min, double max) {
        double value = config.getDouble(PREFIX + path, fallback);
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
