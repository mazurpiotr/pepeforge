package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;

public final class StormcleaverModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private StormcleaverRecipes recipes;
    private StormcleaverListener listener;
    private StormcleaverRecipeDiscoveryListener discoveryListener;

    public StormcleaverModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
    }

    @Override
    public void onEnable() {
        recipes = new StormcleaverRecipes(plugin, itemFactory);
        recipes.registerAll();

        listener = new StormcleaverListener(plugin, itemFactory, lang);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listener.startStatusTask();

        discoveryListener = new StormcleaverRecipeDiscoveryListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(discoveryListener, plugin);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();
            listener.cleanup();
        }
        if (recipes != null) {
            recipes.unregisterAll();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (discoveryListener != null) {
            discoveryListener.discoverFor(player);
        }
    }
}
