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

    private static final Material IRON_RECIPE_MATERIAL = Material.IRON_INGOT;
    private static final Material DIAMOND_RECIPE_MATERIAL = Material.DIAMOND;
    private static final Material BREEZE_CORE_MATERIAL = Material.BREEZE_ROD;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public WindBladeRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        registerTieredBlade(WindBladeTier.IRON);
        registerTieredBlade(WindBladeTier.DIAMOND);
        registerNetheriteUpgrade();
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, WindBladeRecipeKeys.IRON_WIND_BLADE);
        RecipeRegistrar.remove(plugin, WindBladeRecipeKeys.DIAMOND_WIND_BLADE);
        RecipeRegistrar.remove(plugin, WindBladeRecipeKeys.NETHERITE_WIND_BLADE);
    }

    private void registerTieredBlade(WindBladeTier tier) {
        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        NamespacedKey key = WindBladeRecipeKeys.forTier(tier);
        RecipeRegistrar.remove(plugin, key);

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createWindBlade(tier));
        /*
         * [ ][M][ ]
         * [ ][M][ ]
         * [ ][B][ ]
         */
        recipe.shape(" M ", " M ", " B ");
        recipe.setIngredient('M', recipeMaterialFor(tier));
        recipe.setIngredient('B', BREEZE_CORE_MATERIAL);
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

    private Material recipeMaterialFor(WindBladeTier tier) {
        return switch (tier) {
            case IRON -> IRON_RECIPE_MATERIAL;
            case DIAMOND -> DIAMOND_RECIPE_MATERIAL;
            case NETHERITE -> Material.NETHERITE_INGOT;
        };
    }
}
