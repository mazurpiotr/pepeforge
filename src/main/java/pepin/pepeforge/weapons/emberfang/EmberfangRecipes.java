package pepin.pepeforge.weapons.emberfang;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class EmberfangRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public EmberfangRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        RecipeRegistrar.remove(plugin, EmberfangRecipeKeys.EMBERFANG);
        if (!itemFactory.isRecipeEnabled(EmberfangDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(
                EmberfangRecipeKeys.EMBERFANG,
                itemFactory.createEmberfang());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        recipe.shape(" M ", " M ", " B ");
        recipe.setIngredient('M', Material.MAGMA_BLOCK);
        recipe.setIngredient('B', Material.IRON_INGOT);
        RecipeRegistrar.add(plugin, EmberfangRecipeKeys.EMBERFANG, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, EmberfangRecipeKeys.EMBERFANG);
    }
}
