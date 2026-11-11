package pepin.pepeforge.util.recipe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates recipe choices that match a specific custom item, including its item metadata.
 * Used to ensure proper handling of netherite upgrades for custom items.
 * It hides the API difference between Paper 26.2 and older versions of the recipe API.
 */
public final class RecipeChoiceCompat {

    private RecipeChoiceCompat() {
    }

    public static @NonNull RecipeChoice exactChoice(@NonNull ItemStack stack) {
        try {
            Method factory = RecipeChoice.class.getMethod(
                    "exactChoice",
                    ItemStack.class,
                    ItemStack[].class);
            Object result = factory.invoke(null, stack, new ItemStack[0]);
            if (result instanceof RecipeChoice choice) {
                return choice;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return createLegacyExactChoice(stack);
    }

    @SuppressWarnings("removal")
    private static @NonNull RecipeChoice createLegacyExactChoice(@NonNull ItemStack stack) {
        return new RecipeChoice.ExactChoice(stack);
    }
}
