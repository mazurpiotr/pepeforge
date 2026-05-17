package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class CrescentBowRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentBowRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(CrescentBowDefinition.ITEM_ID)) {
            return;
        }

        Bukkit.removeRecipe(CrescentBowRecipeKeys.CRESCENT_BOW);
        Bukkit.removeRecipe(CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED);

        ShapedRecipe recipe = new ShapedRecipe(CrescentBowRecipeKeys.CRESCENT_BOW, itemFactory.createCrescentBow());
        /*
         * [P][A][S]
         * [P][ ][A]
         * [P][A][S]
         */
        recipe.shape("PAS", "P A", "PAS");
        recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED, itemFactory.createCrescentBow());
        /*
         * [S][A][P]
         * [A][ ][P]
         * [S][A][P]
         */
        mirroredRecipe.shape("SAP", "A P", "SAP");
        mirroredRecipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        mirroredRecipe.setIngredient('A', Material.AMETHYST_SHARD);
        mirroredRecipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(mirroredRecipe);
    }

    public void unregisterAll() {
        Bukkit.removeRecipe(CrescentBowRecipeKeys.CRESCENT_BOW);
        Bukkit.removeRecipe(CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED);
    }
}
