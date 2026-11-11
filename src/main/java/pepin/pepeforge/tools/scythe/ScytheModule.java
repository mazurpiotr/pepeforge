package pepin.pepeforge.tools.scythe;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

public class ScytheModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;

    private ScytheRecipes recipes;
    private ScytheRecipeDiscovery recipeDiscovery;

    public ScytheModule(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public void onEnable() {
        this.recipes = new ScytheRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        plugin.getServer().getPluginManager().registerEvents(new ScytheListener(itemFactory), plugin);

        this.recipeDiscovery = new ScytheRecipeDiscovery();
    }

    @Override
    public void onDisable() {
        if (this.recipes != null) {
            this.recipes.unregisterAll();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (this.recipeDiscovery != null) {
            this.recipeDiscovery.discoverFor(player);
        }
    }
}
