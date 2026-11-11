package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.recipe.RecipeRegistrar;

public final class ObsidianGlaiveRecipes {
    public static final NamespacedKey KEY = new NamespacedKey("pepeforge", "obsidian_glaive");
    private final JavaPlugin plugin;
    private final ItemFactory factory;

    public ObsidianGlaiveRecipes(JavaPlugin plugin, ItemFactory factory) {
        this.plugin = plugin;
        this.factory = factory;
    }

    public void registerAll() {
        unregisterAll();
        if (!factory.isRecipeEnabled(ObsidianGlaiveDefinition.ITEM_ID)) {
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(KEY, factory.createObsidianGlaive());
        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        recipe.shape("  O", " S ", "S  ");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('S', Material.STICK);
        RecipeRegistrar.add(plugin, KEY, recipe);
    }

    public void unregisterAll() {
        RecipeRegistrar.remove(plugin, KEY);
    }

    public void discoverFor(Player player) {
        if (factory.isRecipeEnabled(ObsidianGlaiveDefinition.ITEM_ID)
                && player.getInventory().contains(Material.OBSIDIAN)) {
            player.discoverRecipe(KEY);
        }
    }
}
