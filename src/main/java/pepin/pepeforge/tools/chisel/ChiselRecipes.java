package pepin.pepeforge.tools.chisel;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class ChiselRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public ChiselRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(ChiselDefinition.ITEM_ID)) {
            return;
        }

        Bukkit.removeRecipe(ChiselRecipeKeys.CHISEL);

        ShapedRecipe recipe = new ShapedRecipe(ChiselRecipeKeys.CHISEL, itemFactory.createChisel());
        /*
         * [ ][I][ ]
         * [ ][C][ ]
         * [ ][S][ ]
         */
        recipe.shape(" I ", " C ", " S ");
        recipe.setIngredient('I', ChiselDefinition.TOP_MATERIAL);
        recipe.setIngredient('C', ChiselDefinition.CORE_MATERIAL);
        recipe.setIngredient('S', ChiselDefinition.HANDLE_MATERIAL);
        plugin.getServer().addRecipe(recipe);
    }

    public void unregisterAll() {
        Bukkit.removeRecipe(ChiselRecipeKeys.CHISEL);
    }
}
