package pepin.pepeforge.tools.scythe;

import org.bukkit.NamespacedKey;

public final class ScytheRecipeKeys {

    public static final NamespacedKey IRON_SCYTHE = new NamespacedKey("pepeforge", "iron_scythe");
    public static final NamespacedKey DIAMOND_SCYTHE = new NamespacedKey("pepeforge", "diamond_scythe");
    public static final NamespacedKey NETHERITE_SCYTHE = new NamespacedKey("pepeforge", "netherite_scythe");

    private ScytheRecipeKeys() {
    }

    public static NamespacedKey forTier(ScytheTier tier) {
        return switch (tier) {
            case IRON -> IRON_SCYTHE;
            case DIAMOND -> DIAMOND_SCYTHE;
            case NETHERITE -> NETHERITE_SCYTHE;
        };
    }
}
