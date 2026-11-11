package pepin.pepeforge.util.itemmeta;

import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.env.ServerEnv;
import pepin.pepeforge.util.itemmeta.spigot.SpigotAttackRangeAdapter;

public final class AttackRangeCompat {

    private static final AttackRangeAdapter ADAPTER = createAdapter();

    private AttackRangeCompat() {
    }

    public static boolean apply(ItemStack item, double minReach, double maxReach,
                                double minCreativeReach, double maxCreativeReach) {
        return apply(item, minReach, maxReach, minCreativeReach, maxCreativeReach, 0.3);
    }

    /**
     * Applies the native attack range, returning false when the platform has no supported API.
     */
    public static boolean apply(ItemStack item, double minReach, double maxReach,
                                double minCreativeReach, double maxCreativeReach, double hitboxMargin) {
        Objects.requireNonNull(item, "item");
        validateRange(minReach, maxReach, "Survival");
        validateRange(minCreativeReach, maxCreativeReach, "Creative");
        if (!Double.isFinite(hitboxMargin) || hitboxMargin < 0 || hitboxMargin > 1) {
            throw new IllegalArgumentException("Hitbox margin must be between 0 and 1");
        }
        return ADAPTER.apply(item, minReach, maxReach, minCreativeReach, maxCreativeReach, hitboxMargin);
    }

    private static void validateRange(double minimum, double maximum, String mode) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)
                || minimum < 0 || maximum > 64 || minimum > maximum) {
            throw new IllegalArgumentException(mode + " attack range must satisfy 0 <= minimum <= maximum <= 64");
        }
    }

    private static AttackRangeAdapter createAdapter() {
        if (ServerEnv.hasDataComponentApi()) {
            try {
                return (AttackRangeAdapter) Class.forName(
                        "pepin.pepeforge.util.itemmeta.paper.PaperAttackRangeAdapter")
                        .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not initialize Paper attack ranges", exception);
            }
        }
        return new SpigotAttackRangeAdapter();
    }
}
