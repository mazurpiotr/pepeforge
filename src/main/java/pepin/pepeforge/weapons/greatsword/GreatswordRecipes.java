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
        for (GreatswordTier tier : GreatswordTier.values()) {
            RecipeRegistrar.remove(plugin, GreatswordRecipeKeys.forTier(tier));
        }
    }

    private void registerShaped(GreatswordTier tier) {
        NamespacedKey key = GreatswordRecipeKeys.forTier(tier);
        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createGreatsword(tier));
        recipe.setCategory(org.bukkit.inventory.recipe.CraftingBookCategory.EQUIPMENT);
        /*
         * [ ][M][ ]
         * [M][M][M]
         * [ ][S][ ]
         */
        recipe.shape(" M ", "MMM", " S ");
        recipe.setIngredient('M', tier.bladeMaterial());
        recipe.setIngredient('S', tier.handleMaterial());
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
