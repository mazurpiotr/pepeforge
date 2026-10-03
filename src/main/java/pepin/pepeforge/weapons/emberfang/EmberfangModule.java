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
    private EmberfangRecipeDiscoveryListener discoveryListener;

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

        discoveryListener = new EmberfangRecipeDiscoveryListener(plugin, itemFactory);
        plugin.getServer().getPluginManager().registerEvents(discoveryListener, plugin);
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
        discoveryListener = null;
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (discoveryListener != null) {
            discoveryListener.discoverFor(player);
        }
    }
}
