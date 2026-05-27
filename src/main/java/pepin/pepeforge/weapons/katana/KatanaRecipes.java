package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class KatanaRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public KatanaRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(KatanaDefinition.ITEM_ID)) {
            return;
        }

        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA);
        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA_MIRRORED);

        ShapedRecipe recipe = new ShapedRecipe(KatanaRecipeKeys.KATANA, itemFactory.createKatana());
        /*
         * [ ][ ][I]
         * [ ][I][ ]
         * [S][ ][ ]
         */
        recipe.shape("  I", " I ", "S  ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, KatanaRecipeKeys.KATANA, recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(KatanaRecipeKeys.KATANA_MIRRORED, itemFactory.createKatana());
        /*
         * [I][ ][ ]
         * [ ][I][ ]
         * [ ][ ][S]
         */
        mirroredRecipe.shape("I  ", " I ", "  S");
        mirroredRecipe.setIngredient('I', Material.IRON_INGOT);
        mirroredRecipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, KatanaRecipeKeys.KATANA_MIRRORED, mirroredRecipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA);
        RecipeRegistrar.remove(plugin, KatanaRecipeKeys.KATANA_MIRRORED);
    }
}
