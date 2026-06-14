package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class SolarShieldRecipes {

    private static final NamespacedKey KEY = new NamespacedKey("pepeforge", "solar_shield");

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public SolarShieldRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        RecipeRegistrar.remove(plugin, KEY);

        if (!itemFactory.isRecipeEnabled(SolarShieldDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(KEY, itemFactory.createSolarShield());
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        /*
         * [G][I][G]
         * [I][E][I]
         * [G][I][G]
         */
        recipe.shape("GIG", "IEI", "GIG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        RecipeRegistrar.add(plugin, KEY, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, KEY);
    }
}
