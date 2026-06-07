package pepin.pepeforge.tools.chisel;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

public class ChiselModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;

    private ChiselRecipes recipes;
    private ChiselRecipeDiscoveryListener discoveryListener;

    public ChiselModule(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public void onEnable() {
        this.recipes = new ChiselRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        plugin.getServer().getPluginManager().registerEvents(new ChiselListener(itemFactory), plugin);

        this.discoveryListener = new ChiselRecipeDiscoveryListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(this.discoveryListener, plugin);
    }

    @Override
    public void onDisable() {
        if (this.recipes != null) {
            this.recipes.unregisterAll();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (this.discoveryListener != null) {
            this.discoveryListener.discoverFor(player);
        }
    }
}
