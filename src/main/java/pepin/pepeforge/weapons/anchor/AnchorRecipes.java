package pepin.pepeforge.weapons.anchor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class AnchorRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public AnchorRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        NamespacedKey key = AnchorRecipeKeys.ANCHOR;
        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(AnchorDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createAnchor());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);

        // Layout:
        // IBI
        // I
        // IR
        recipe.shape("IBI", " I ", " IR");

        recipe.setIngredient('B', Material.IRON_BLOCK);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.LEAD);

        RecipeRegistrar.add(plugin, key, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, AnchorRecipeKeys.ANCHOR);
    }
}
