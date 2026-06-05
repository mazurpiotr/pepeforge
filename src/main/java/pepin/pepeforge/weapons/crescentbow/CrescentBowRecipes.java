package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

        NamespacedKey key = CrescentBowRecipeKeys.CRESCENT_BOW;
        NamespacedKey keyMirrored = CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED;

        RecipeRegistrar.remove(plugin, key);
        RecipeRegistrar.remove(plugin, keyMirrored);

        if (!itemFactory.isRecipeEnabled(CrescentBowDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createCrescentBow());
        /*
         * [P][A][S]
         * [P][ ][A]
         * [P][A][S]
         */
        recipe.shape("PAS", "P A", "PAS");
        recipe.setIngredient('P', CURVE_MATERIAL);
        recipe.setIngredient('A', CORE_MATERIAL);
        recipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, key, recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(keyMirrored, itemFactory.createCrescentBow());
        /*
         * [S][A][P]
         * [A][ ][P]
         * [S][A][P]
         */
        mirroredRecipe.shape("SAP", "A P", "SAP");
        mirroredRecipe.setIngredient('P', CURVE_MATERIAL);
        mirroredRecipe.setIngredient('A', CORE_MATERIAL);
        mirroredRecipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, keyMirrored, mirroredRecipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW);
        RecipeRegistrar.remove(plugin, CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED);
    }
}
