package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.env.ServerEnv;
import pepin.pepeforge.util.itemmeta.spigot.SpigotAttackAnimationAdapter;

public final class AttackAnimationCompat {

    private static final AttackAnimationAdapter ADAPTER = createAdapter();

    private AttackAnimationCompat() {
    }

    public static boolean apply(ItemStack item, boolean stab) {
        return ADAPTER.apply(item, stab);
    }

    private static AttackAnimationAdapter createAdapter() {
        if (ServerEnv.hasDataComponentApi()) {
            try {
                return (AttackAnimationAdapter) Class.forName(
                        "pepin.pepeforge.util.itemmeta.paper.PaperAttackAnimationAdapter")
                        .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not initialize Paper attack animations", exception);
            }
        }
        return new SpigotAttackAnimationAdapter();
    }
}
