package pepin.pepeforge.weapons.windblade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class WindBladeRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public WindBladeRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        registerShaped(WindBladeTier.IRON);
        registerShaped(WindBladeTier.DIAMOND);
        registerNetheriteUpgrade();
    }

    public void unregisterAll() {
        for (WindBladeTier tier : WindBladeTier.values()) {
            RecipeRegistrar.remove(plugin, WindBladeRecipeKeys.forTier(tier));
        }
    }

    private void registerShaped(WindBladeTier tier) {
        NamespacedKey key = WindBladeRecipeKeys.forTier(tier);
        RecipeRegistrar.remove(plugin, key);

        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createWindBlade(tier));
        /*
         * [ ][M][ ]
         * [ ][M][ ]
         * [ ][B][ ]
         */
        recipe.shape(" M ", " M ", " B ");
        recipe.setIngredient('M', tier.bladeMaterial());
        recipe.setIngredient('B', tier.handleMaterial());
        RecipeRegistrar.add(plugin, key, recipe);
    }

    private void registerNetheriteUpgrade() {
        if (!itemFactory.isRecipeEnabled(WindBladeTier.NETHERITE.itemId())) {
            return;
        }

        NamespacedKey key = WindBladeRecipeKeys.NETHERITE_WIND_BLADE;
        RecipeRegistrar.remove(plugin, key);

        ItemStack result = itemFactory.createWindBlade(WindBladeTier.NETHERITE);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.ExactChoice(itemFactory.createWindBlade(WindBladeTier.DIAMOND)),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        );
        RecipeRegistrar.add(plugin, key, recipe);
    }
}
