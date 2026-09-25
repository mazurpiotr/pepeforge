package pepin.pepeforge.util.persistence;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class PersistentDataCompat {

    private PersistentDataCompat() {
    }

    public static int getInt(Player player, @NotNull NamespacedKey key) {
        PersistentDataContainer container = getWritableContainer(player);
        @NotNull NamespacedKey nonNullKey = Objects.requireNonNull(key);
        @NotNull PersistentDataType<Integer, Integer> integerType = Objects.requireNonNull(
            PersistentDataType.INTEGER);
        return container.getOrDefault(nonNullKey, integerType, 0);
    }

    public static void setInt(Player player, @NotNull NamespacedKey key, int value) {
        @NotNull NamespacedKey nonNullKey = Objects.requireNonNull(key);
        @NotNull PersistentDataType<Integer, Integer> integerType = Objects.requireNonNull(
            PersistentDataType.INTEGER);
        getWritableContainer(player).set(nonNullKey, integerType, value);
    }

    private static PersistentDataContainer getWritableContainer(Player player) {
        for (Method method : PersistentDataHolder.class.getMethods()) {
            if (!method.getName().equals("getPersistentDataContainer")
                    || method.getParameterCount() != 0
                    || method.getReturnType() != PersistentDataContainer.class) {
                continue;
            }

            try {
                return (PersistentDataContainer) method.invoke(player);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to access writable persistent data", exception);
            }
        }

        throw new IllegalStateException("Writable persistent data is unavailable");
    }
}