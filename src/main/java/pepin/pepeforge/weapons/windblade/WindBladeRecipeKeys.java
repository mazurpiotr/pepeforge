package pepin.pepeforge.weapons.windblade;

import org.bukkit.NamespacedKey;

public final class WindBladeRecipeKeys {

    public static final NamespacedKey IRON_WIND_BLADE = new NamespacedKey("pepeforge", "iron_wind_blade");
    public static final NamespacedKey DIAMOND_WIND_BLADE = new NamespacedKey("pepeforge", "diamond_wind_blade");
    public static final NamespacedKey NETHERITE_WIND_BLADE = new NamespacedKey("pepeforge", "netherite_wind_blade");

    private WindBladeRecipeKeys() {
    }

    public static NamespacedKey forTier(WindBladeTier tier) {
        return switch (tier) {
            case IRON -> IRON_WIND_BLADE;
            case DIAMOND -> DIAMOND_WIND_BLADE;
            case NETHERITE -> NETHERITE_WIND_BLADE;
        };
    }
}
