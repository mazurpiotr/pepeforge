package pepin.pepeforge.weapons.rapier;

import org.bukkit.NamespacedKey;

public final class RapierRecipeKeys {
    public static final NamespacedKey IRON_RAPIER = new NamespacedKey("pepeforge", "iron_rapier");
    public static final NamespacedKey DIAMOND_RAPIER = new NamespacedKey("pepeforge", "diamond_rapier");
    public static final NamespacedKey NETHERITE_RAPIER = new NamespacedKey("pepeforge", "netherite_rapier");
    public static final NamespacedKey ICE_RAPIER = new NamespacedKey("pepeforge", "ice_rapier");

    private RapierRecipeKeys() {
    }

    public static NamespacedKey forTier(RapierTier tier) {
        return switch (tier) {
            case IRON -> IRON_RAPIER;
            case DIAMOND -> DIAMOND_RAPIER;
            case NETHERITE -> NETHERITE_RAPIER;
            case ICE -> ICE_RAPIER;
        };
    }
}
