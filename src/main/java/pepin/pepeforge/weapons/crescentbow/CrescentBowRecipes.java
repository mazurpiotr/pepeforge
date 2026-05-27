package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class CrescentBowRecipes {

    private static final Material CURVE_MATERIAL = Material.PHANTOM_MEMBRANE;
    private static final Material CORE_MATERIAL = Material.AMETHYST_SHARD;
    private static final Material HANDLE_MATERIAL = Material.STICK;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentBowRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(CrescentBowDefinition.ITEM_ID)) {
            return;
        }

        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW);
        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED);

        ShapedRecipe recipe = new ShapedRecipe(CrescentBowRecipeKeys.CRESCENT_BOW, itemFactory.createCrescentBow());
        /*
         * [P][A][S]
         * [P][ ][A]
         * [P][A][S]
         */
        recipe.shape("PAS", "P A", "PAS");
        recipe.setIngredient('P', CURVE_MATERIAL);
        recipe.setIngredient('A', CORE_MATERIAL);
        recipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, CrescentBowRecipeKeys.CRESCENT_BOW, recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED, itemFactory.createCrescentBow());
        /*
         * [S][A][P]
         * [A][ ][P]
         * [S][A][P]
         */
        mirroredRecipe.shape("SAP", "A P", "SAP");
        mirroredRecipe.setIngredient('P', CURVE_MATERIAL);
        mirroredRecipe.setIngredient('A', CORE_MATERIAL);
        mirroredRecipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED, mirroredRecipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW);
        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED);
    }
}
