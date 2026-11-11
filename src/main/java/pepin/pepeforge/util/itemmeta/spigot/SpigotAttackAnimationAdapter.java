package pepin.pepeforge.util.itemmeta.spigot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.itemmeta.AttackAnimationAdapter;

public final class SpigotAttackAnimationAdapter implements AttackAnimationAdapter {

    private final Access access = findAccess();

    @Override
    public boolean apply(ItemStack item, boolean stab) {
        if (access == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        ItemMeta defaultMeta = new ItemStack(item.getType()).getItemMeta();
        if (meta == null || defaultMeta == null) {
            return false;
        }
        try {
            // A fresh component removes custom durations stored by older rapiers.
            Object animation = access.getAnimation().invoke(defaultMeta);
            access.setType().invoke(animation, stab ? access.stab() : access.whack());
            access.setAnimation().invoke(meta, animation);
            item.setItemMeta(meta);
            return true;
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Could not apply the Spigot attack animation", exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access the Spigot attack animation API", exception);
        }
    }

    private static Access findAccess() {
        try {
            Class<?> component = Class.forName("org.bukkit.inventory.meta.components.SwingAnimationComponent");
            Class<?> type = Class.forName("org.bukkit.inventory.meta.components.SwingAnimationComponent$Type");
            return new Access(ItemMeta.class.getMethod("getSwingAnimation"),
                    ItemMeta.class.getMethod("setSwingAnimation", component),
                    component.getMethod("setType", type),
                    type.getField("STAB").get(null), type.getField("WHACK").get(null));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException exception) {
            return null;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not inspect the Spigot attack animation API", exception);
        }
    }

    private record Access(Method getAnimation, Method setAnimation, Method setType,
                          Object stab, Object whack) {
    }
}
