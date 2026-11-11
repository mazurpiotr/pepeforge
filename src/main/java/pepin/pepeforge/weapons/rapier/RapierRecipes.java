package pepin.pepeforge.weapons.rapier;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;
import pepin.pepeforge.util.recipe.RecipeChoiceCompat;

public final class RapierRecipes {
    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public RapierRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        unregisterAll();
        registerShaped(RapierTier.IRON);
        registerShaped(RapierTier.DIAMOND);
        registerShaped(RapierTier.ICE);
        if (isRecipeAvailable(itemFactory, RapierTier.NETHERITE)) {
            NamespacedKey key = RapierRecipeKeys.NETHERITE_RAPIER;
            RecipeRegistrar.add(plugin, key, new SmithingTransformRecipe(key,
                    itemFactory.createRapier(RapierTier.NETHERITE),
                    new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    RecipeChoiceCompat.exactChoice(itemFactory.createRapier(RapierTier.DIAMOND)),
                    new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT)));
        }
    }

    public void unregisterAll() {
        for (RapierTier tier : RapierTier.values()) {
            RecipeRegistrar.remove(plugin, RapierRecipeKeys.forTier(tier));
        }
    }

    static boolean isRecipeAvailable(ItemFactory itemFactory, RapierTier tier) {
        return itemFactory.isItemEnabled(tier.itemId()) && itemFactory.isRecipeEnabled(tier.itemId())
                && (tier != RapierTier.NETHERITE || itemFactory.isItemEnabled(RapierTier.DIAMOND.itemId()));
    }

    private void registerShaped(RapierTier tier) {
        if (!isRecipeAvailable(itemFactory, tier)) {
            return;
        }
        NamespacedKey key = RapierRecipeKeys.forTier(tier);
        ShapedRecipe recipe = new ShapedRecipe(key, itemFactory.createRapier(tier));
        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        recipe.shape("SMM");
        recipe.setIngredient('M', tier.bladeMaterial());
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, key, recipe);
    }
}
