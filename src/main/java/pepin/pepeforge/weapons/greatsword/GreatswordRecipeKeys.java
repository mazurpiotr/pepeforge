package pepin.pepeforge.weapons.greatsword;

import org.bukkit.NamespacedKey;

public final class GreatswordRecipeKeys {

    public static final NamespacedKey IRON_GREATSWORD = new NamespacedKey("pepeforge", "iron_greatsword");
    public static final NamespacedKey DIAMOND_GREATSWORD = new NamespacedKey("pepeforge", "diamond_greatsword");
    public static final NamespacedKey NETHERITE_GREATSWORD = new NamespacedKey("pepeforge", "netherite_greatsword");

    private GreatswordRecipeKeys() {
    }

    public static NamespacedKey forTier(GreatswordTier tier) {
        return switch (tier) {
            case IRON -> IRON_GREATSWORD;
            case DIAMOND -> DIAMOND_GREATSWORD;
            case NETHERITE -> NETHERITE_GREATSWORD;
        };
    }
}
