package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class GreatswordRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public GreatswordRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        registerShaped(GreatswordTier.IRON);
        registerShaped(GreatswordTier.DIAMOND);
        registerNetheriteUpgrade();
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, GreatswordRecipeKeys.IRON_GREATSWORD);
        RecipeRegistrar.remove(plugin, GreatswordRecipeKeys.DIAMOND_GREATSWORD);
        RecipeRegistrar.remove(plugin, GreatswordRecipeKeys.NETHERITE_GREATSWORD);
    }

    private void registerShaped(GreatswordTier tier) {
        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        NamespacedKey key = GreatswordRecipeKeys.forTier(tier);
        RecipeRegistrar.remove(plugin, key);

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createGreatsword(tier));
        /*
         * [ ][M][ ]
         * [M][M][M]
         * [ ][S][ ]
         */
        recipe.shape(" M ", "MMM", " S ");
        recipe.setIngredient('M', tier.bladeMaterial());
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, key, recipe);
    }

    private void registerNetheriteUpgrade() {
        if (!itemFactory.isRecipeEnabled(GreatswordTier.NETHERITE.itemId())) {
            return;
        }

        NamespacedKey key = GreatswordRecipeKeys.NETHERITE_GREATSWORD;
        RecipeRegistrar.remove(plugin, key);

        ItemStack result = itemFactory.createGreatsword(GreatswordTier.NETHERITE);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.ExactChoice(itemFactory.createGreatsword(GreatswordTier.DIAMOND)),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        );
        RecipeRegistrar.add(plugin, key, recipe);
    }
}
