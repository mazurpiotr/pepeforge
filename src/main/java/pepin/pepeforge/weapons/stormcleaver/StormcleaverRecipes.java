package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ShapedRecipe;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class StormcleaverRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public StormcleaverRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        RecipeRegistrar.remove(plugin, StormcleaverRecipeKeys.STORMCLEAVER);
        if (!itemFactory.isRecipeEnabled(StormcleaverDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(
                StormcleaverRecipeKeys.STORMCLEAVER,
                itemFactory.createStormcleaver());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        recipe.shape("ICI", " S ", " S ");
        recipe.setIngredient('I', Material.IRON_BLOCK);
        recipe.setIngredient('C', Material.CONDUIT);
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, StormcleaverRecipeKeys.STORMCLEAVER, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, StormcleaverRecipeKeys.STORMCLEAVER);
    }
}
