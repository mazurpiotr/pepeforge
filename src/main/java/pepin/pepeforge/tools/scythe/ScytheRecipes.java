package pepin.pepeforge.tools.scythe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
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
        registerShaped(ScytheTier.IRON);
        registerShaped(ScytheTier.DIAMOND);
        registerNetheriteUpgrade();
    }

    public void unregisterAll() {
        for (ScytheTier tier : ScytheTier.values()) {
            RecipeRegistrar.remove(plugin, ScytheRecipeKeys.forTier(tier));
        }
    }

    private void registerShaped(ScytheTier tier) {
        NamespacedKey key = ScytheRecipeKeys.forTier(tier);
        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createScythe(tier));
        /*
         * [B][B][B]
         * [ ][S][ ]
         * [S][ ][ ]
         */
        recipe.shape("BBB", " S ", "S  ");
        recipe.setIngredient('B', tier.bladeMaterial());
        recipe.setIngredient('S', tier.handleMaterial());
        RecipeRegistrar.add(plugin, key, recipe);
    }

    private void registerNetheriteUpgrade() {
        if (!itemFactory.isRecipeEnabled(ScytheTier.NETHERITE.itemId())) {
            return;
        }

        NamespacedKey key = ScytheRecipeKeys.NETHERITE_SCYTHE;
        RecipeRegistrar.remove(plugin, key);

        ItemStack result = itemFactory.createScythe(ScytheTier.NETHERITE);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.ExactChoice(itemFactory.createScythe(ScytheTier.DIAMOND)),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        );
        RecipeRegistrar.add(plugin, key, recipe);
    }
}
