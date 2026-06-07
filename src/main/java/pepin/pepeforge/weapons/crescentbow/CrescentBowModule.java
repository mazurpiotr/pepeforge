package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

public class CrescentBowModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;

    private CrescentBowRecipes recipes;
    private CrescentBowRecipeDiscoveryListener discoveryListener;

    public CrescentBowModule(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public void onEnable() {
        this.recipes = new CrescentBowRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        plugin.getServer().getPluginManager().registerEvents(new CrescentBowListener(itemFactory), plugin);

        this.discoveryListener = new CrescentBowRecipeDiscoveryListener(plugin);
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
