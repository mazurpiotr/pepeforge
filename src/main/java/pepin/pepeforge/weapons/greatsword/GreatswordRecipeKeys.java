package pepin.pepeforge.weapons.greatsword;

import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;

public final class GreatswordRecipeKeys {

    public static final @NonNull NamespacedKey IRON_GREATSWORD = new NamespacedKey("pepeforge", "iron_greatsword");
    public static final @NonNull NamespacedKey DIAMOND_GREATSWORD = new NamespacedKey("pepeforge", "diamond_greatsword");
    public static final @NonNull NamespacedKey NETHERITE_GREATSWORD = new NamespacedKey("pepeforge", "netherite_greatsword");

    private GreatswordRecipeKeys() {
    }

    public static @NonNull NamespacedKey forTier(GreatswordTier tier) {
        return switch (tier) {
            case IRON -> IRON_GREATSWORD;
            case DIAMOND -> DIAMOND_GREATSWORD;
            case NETHERITE -> NETHERITE_GREATSWORD;
        };
    }
}
