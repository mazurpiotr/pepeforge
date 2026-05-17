package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class CrescentSpearRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentSpearRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(CrescentSpearDefinition.ITEM_ID)) {
            return;
        }

        Bukkit.removeRecipe(CrescentSpearRecipeKeys.CRESCENT_SPEAR);

        ShapedRecipe recipe = new ShapedRecipe(CrescentSpearRecipeKeys.CRESCENT_SPEAR, itemFactory.createCrescentSpear());
        /*
         * [ ][A][ ]
         * [ ][S][ ]
         * [ ][S][ ]
         */
        recipe.shape(" A ", " S ", " S ");
        recipe.setIngredient('A', CrescentSpearDefinition.BLADE_MATERIAL);
        recipe.setIngredient('S', CrescentSpearDefinition.HANDLE_MATERIAL);
        plugin.getServer().addRecipe(recipe);
    }

    public void unregisterAll() {
        Bukkit.removeRecipe(CrescentSpearRecipeKeys.CRESCENT_SPEAR);
    }
}
