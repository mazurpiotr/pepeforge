package pepin.pepeforge.weapons.throwingknife;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class ThrowingKnifeRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public ThrowingKnifeRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        NamespacedKey key = ThrowingKnifeRecipeKeys.THROWING_KNIFE;
        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(ThrowingKnifeDefinition.ITEM_ID)) {
            return;
        }

        ItemStack result = itemFactory.createThrowingKnife();
        result.setAmount(4);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        recipe.shape(" I", "S ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);

        RecipeRegistrar.add(plugin, key, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, ThrowingKnifeRecipeKeys.THROWING_KNIFE);
    }
}
