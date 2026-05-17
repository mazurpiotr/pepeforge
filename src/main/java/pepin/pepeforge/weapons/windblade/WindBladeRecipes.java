package pepin.pepeforge.weapons.windblade;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class WindBladeRecipes {

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
        Bukkit.removeRecipe(WindBladeRecipeKeys.IRON_WIND_BLADE);
        Bukkit.removeRecipe(WindBladeRecipeKeys.DIAMOND_WIND_BLADE);
        Bukkit.removeRecipe(WindBladeRecipeKeys.NETHERITE_WIND_BLADE);
    }

    private void registerTieredBlade(WindBladeTier tier) {
        if (!itemFactory.isRecipeEnabled(tier.itemId())) {
            return;
        }

        NamespacedKey key = WindBladeRecipeKeys.forTier(tier);
        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createWindBlade(tier));
        /*
         * [ ][M][ ]
         * [ ][M][ ]
         * [ ][B][ ]
         */
        recipe.shape(" M ", " M ", " B ");
        recipe.setIngredient('M', tier.recipeMaterial());
        recipe.setIngredient('B', Material.BREEZE_ROD);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerNetheriteUpgrade() {
        if (!itemFactory.isRecipeEnabled(WindBladeTier.NETHERITE.itemId())) {
            return;
        }

        NamespacedKey key = WindBladeRecipeKeys.NETHERITE_WIND_BLADE;
        Bukkit.removeRecipe(key);

        ItemStack result = itemFactory.createWindBlade(WindBladeTier.NETHERITE);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.ExactChoice(itemFactory.createWindBlade(WindBladeTier.DIAMOND)),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)
        );
        plugin.getServer().addRecipe(recipe);
    }
}
