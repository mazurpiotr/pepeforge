package pepin.pepeforge.tools.chisel;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class ChiselRecipes {

    private static final Material TIP_MATERIAL = Material.STONE;
    private static final Material CORE_MATERIAL = Material.IRON_INGOT;
    private static final Material HANDLE_MATERIAL = Material.STICK;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public ChiselRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {

        NamespacedKey key = ChiselRecipeKeys.CHISEL;

        RecipeRegistrar.remove(plugin, key);
        
        if (!itemFactory.isRecipeEnabled(ChiselDefinition.ITEM_ID)) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createChisel());
        /*
         * [ ][I][ ]
         * [ ][C][ ]
         * [ ][S][ ]
         */
        recipe.shape(" I ", " C ", " S ");
        recipe.setIngredient('I', TIP_MATERIAL);
        recipe.setIngredient('C', CORE_MATERIAL);
        recipe.setIngredient('S', HANDLE_MATERIAL);
        RecipeRegistrar.add(plugin, key, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, ChiselRecipeKeys.CHISEL);
    }
}
