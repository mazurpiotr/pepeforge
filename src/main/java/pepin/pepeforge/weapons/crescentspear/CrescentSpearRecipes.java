package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class CrescentSpearRecipes {

    private static final Material BLADE_MATERIAL = Material.AMETHYST_SHARD;
    private static final Material HANDLE_MATERIAL = Material.STICK;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentSpearRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {

        NamespacedKey key = CrescentSpearRecipeKeys.CRESCENT_SPEAR;

        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(CrescentSpearDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createCrescentSpear());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        /*
         * [ ][A][ ]
         * [ ][S][ ]
         * [ ][S][ ]
         */
        recipe.shape(" A ", " S ", " S ");
        recipe.setIngredient('A', BLADE_MATERIAL);
        recipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, key, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, CrescentSpearRecipeKeys.CRESCENT_SPEAR);
    }
}
