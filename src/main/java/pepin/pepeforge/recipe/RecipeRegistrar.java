package pepin.pepeforge.recipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class RecipeRegistrar {

    private RecipeRegistrar() {
    }

    public static void remove(JavaPlugin plugin, NamespacedKey key) {
        Boolean removedWithResend = null;
        if (!plugin.getServer().getOnlinePlayers().isEmpty()) {
            removedWithResend = invokeRemoveWithResend(plugin, key);
        }

        boolean removed = removedWithResend != null ? removedWithResend : plugin.getServer().removeRecipe(key);
        if (!removed && removedWithResend != null) {
            plugin.getServer().removeRecipe(key);
        }
    }

    public static void add(JavaPlugin plugin, NamespacedKey key, Recipe recipe) {
        Boolean addedWithResend = null;
        if (!plugin.getServer().getOnlinePlayers().isEmpty()) {
            addedWithResend = invokeAddWithResend(plugin, recipe);
        }

        boolean added = addedWithResend != null ? addedWithResend : plugin.getServer().addRecipe(recipe);
        if (!added && addedWithResend != null) {
            // If the resend-enabled method exists but failed, retry with the plain Bukkit API.
            added = plugin.getServer().addRecipe(recipe);
        }

        if (!added && plugin.getServer().getRecipe(key) == null) {
            plugin.getLogger().warning("Could not register recipe " + key);
        }
    }

    private static Boolean invokeAddWithResend(JavaPlugin plugin, Recipe recipe) {
        try {
            Method method = plugin.getServer().getClass().getMethod("addRecipe", Recipe.class, boolean.class);
            return (Boolean) method.invoke(plugin.getServer(), recipe, true);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().warning("Could not call addRecipe with client refresh: " + exception.getMessage());
            return null;
        }
    }

    private static Boolean invokeRemoveWithResend(JavaPlugin plugin, NamespacedKey key) {
        try {
            Method method = plugin.getServer().getClass().getMethod("removeRecipe", NamespacedKey.class, boolean.class);
            return (Boolean) method.invoke(plugin.getServer(), key, true);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().warning("Could not call removeRecipe with client refresh: " + exception.getMessage());
            return null;
        }
    }
}
