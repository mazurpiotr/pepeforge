package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class CrescentKhopeshRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentKhopeshRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        RecipeRegistrar.remove(plugin, CrescentKhopeshRecipeKeys.CRESCENT_KHOPESH);
        if (!itemFactory.isRecipeEnabled(CrescentKhopeshDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(
                CrescentKhopeshRecipeKeys.CRESCENT_KHOPESH,
                itemFactory.createCrescentKhopesh());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        recipe.shape(" A ", "NAN", " S ");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('N', Material.IRON_NUGGET);
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, CrescentKhopeshRecipeKeys.CRESCENT_KHOPESH, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, CrescentKhopeshRecipeKeys.CRESCENT_KHOPESH);
    }
}
