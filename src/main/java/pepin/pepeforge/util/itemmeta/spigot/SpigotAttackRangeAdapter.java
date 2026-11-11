package pepin.pepeforge.util.itemmeta.spigot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.itemmeta.AttackRangeAdapter;

public final class SpigotAttackRangeAdapter implements AttackRangeAdapter {

    private final Access access = findAccess();

    @Override
    public boolean apply(ItemStack item, double minReach, double maxReach,
                         double minCreativeReach, double maxCreativeReach, double hitboxMargin) {
        if (access == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        try {
            Object range = access.getRange().invoke(meta);
            access.setMinReach().invoke(range, (float) minReach);
            access.setMaxReach().invoke(range, (float) maxReach);
            access.setMinCreativeReach().invoke(range, (float) minCreativeReach);
            access.setMaxCreativeReach().invoke(range, (float) maxCreativeReach);
            access.setHitboxMargin().invoke(range, (float) hitboxMargin);
            access.setRange().invoke(meta, range);
            return item.setItemMeta(meta);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Could not apply the Spigot attack range", exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access the Spigot attack range API", exception);
        }
    }

    private static Access findAccess() {
        try {
            Class<?> component = Class.forName("org.bukkit.inventory.meta.components.AttackRangeComponent");
            return new Access(ItemMeta.class.getMethod("getAttackRange"),
                    ItemMeta.class.getMethod("setAttackRange", component),
                    component.getMethod("setMinReach", float.class),
                    component.getMethod("setMaxReach", float.class),
                    component.getMethod("setMinCreativeReach", float.class),
                    component.getMethod("setMaxCreativeReach", float.class),
                    component.getMethod("setHitboxMargin", float.class));
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return null;
        }
    }

    private record Access(Method getRange, Method setRange, Method setMinReach, Method setMaxReach,
                          Method setMinCreativeReach, Method setMaxCreativeReach, Method setHitboxMargin) {
    }
}
