package pepin.pepeforge.weapons.katana;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class KatanaRecipes {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public KatanaRecipes(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll() {
        if (!itemFactory.isRecipeEnabled(KatanaDefinition.ITEM_ID)) {
            return;
        }

        Bukkit.removeRecipe(KatanaRecipeKeys.KATANA);
        Bukkit.removeRecipe(KatanaRecipeKeys.KATANA_MIRRORED);

        ShapedRecipe recipe = new ShapedRecipe(KatanaRecipeKeys.KATANA, itemFactory.createKatana());
        /*
         * [ ][I][I]
         * [ ][I][ ]
         * [ ][S][ ]
         */
        recipe.shape(" II", " I ", " S ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);

        ShapedRecipe mirroredRecipe = new ShapedRecipe(KatanaRecipeKeys.KATANA_MIRRORED, itemFactory.createKatana());
        /*
         * [I][I][ ]
         * [ ][I][ ]
         * [ ][S][ ]
         */
        mirroredRecipe.shape("II ", " I ", " S ");
        mirroredRecipe.setIngredient('I', Material.IRON_INGOT);
        mirroredRecipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(mirroredRecipe);
    }

    public void unregisterAll() {
        Bukkit.removeRecipe(KatanaRecipeKeys.KATANA);
        Bukkit.removeRecipe(KatanaRecipeKeys.KATANA_MIRRORED);
    }
}
