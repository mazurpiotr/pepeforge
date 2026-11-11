package pepin.pepeforge.weapons.emberfang;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

public final class EmberfangModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private EmberfangListener listener;
    private EmberfangRecipes recipes;
    private EmberfangRecipeDiscovery recipeDiscovery;

    public EmberfangModule(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public void onEnable() {
        recipes = new EmberfangRecipes(plugin, itemFactory);
        recipes.registerAll();

        listener = new EmberfangListener(plugin, itemFactory);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listener.start();

        recipeDiscovery = new EmberfangRecipeDiscovery(itemFactory);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
        if (recipes != null) {
            recipes.unregisterAll();
            recipes = null;
        }
        recipeDiscovery = null;
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (recipeDiscovery != null) {
            recipeDiscovery.discoverFor(player);
        }
    }
}
