package pepin.pepeforge.util.recipe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    @SuppressWarnings("deprecation")
    private static @NonNull RecipeChoice createLegacyExactChoice(@NonNull ItemStack stack) {
        return new RecipeChoice.ExactChoice(stack);
    }
}
