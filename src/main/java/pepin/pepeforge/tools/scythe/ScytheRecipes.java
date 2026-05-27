package pepin.pepeforge.tools.scythe;

import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class ScytheRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public ScytheRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        for (ScytheTier tier : ScytheTier.values()) {
            register(tier);
        }
    }

    public void unregisterAll() {
        for (ScytheTier tier : ScytheTier.values()) {
            RecipeRegistrar.remove(plugin, RecipeKeys.forTier(tier));
        }
    }

    private void register(ScytheTier tier) {
        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        RecipeRegistrar.remove(plugin, RecipeKeys.forTier(tier));

        ShapedRecipe recipe = new ShapedRecipe(RecipeKeys.forTier(tier), itemFactory.createScythe(tier));
        /*
         * [B][B][B]
         * [ ][S][ ]
         * [S][ ][ ]
         */
        recipe.shape("BBB", " S ", "S  ");
        recipe.setIngredient('B', tier.bladeMaterial());
        recipe.setIngredient('S', tier.handleMaterial());
        RecipeRegistrar.add(plugin, RecipeKeys.forTier(tier), recipe);
    }
}
