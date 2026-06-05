package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class KatanaRecipes {

    private static final Material CORE_MATERIAL = Material.IRON_INGOT;
    private static final Material HANDLE_MATERIAL = Material.STICK;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public KatanaRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {

        NamespacedKey key = KatanaRecipeKeys.KATANA;
        NamespacedKey keyMirrored = KatanaRecipeKeys.KATANA_MIRRORED;

        RecipeRegistrar.remove(plugin, key);
        RecipeRegistrar.remove(plugin, keyMirrored);

        if (!itemFactory.isRecipeEnabled(KatanaDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createKatana());
        /*
         * [ ][ ][I]
         * [ ][I][ ]
         * [S][ ][ ]
         */
        recipe.shape("  I", " I ", "S  ");
        recipe.setIngredient('I', CORE_MATERIAL);
        recipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, key, recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(keyMirrored, itemFactory.createKatana());
        /*
         * [I][ ][ ]
         * [ ][I][ ]
         * [ ][ ][S]
         */
        mirroredRecipe.shape("I  ", " I ", "  S");
        mirroredRecipe.setIngredient('I', CORE_MATERIAL);
        mirroredRecipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, keyMirrored, mirroredRecipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA);
        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA_MIRRORED);
    }
}
