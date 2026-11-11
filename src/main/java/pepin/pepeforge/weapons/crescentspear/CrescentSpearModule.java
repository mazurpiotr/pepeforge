package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;

public class CrescentSpearModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;

    private CrescentSpearRecipes recipes;
    private CrescentSpearListener listener;
    private CrescentSpearRecipeDiscovery recipeDiscovery;

    public CrescentSpearModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
    }

    @Override
    public void onEnable() {
        this.recipes = new CrescentSpearRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new CrescentSpearListener(plugin, itemFactory, lang);
        this.listener.startStatusTask();
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.recipeDiscovery = new CrescentSpearRecipeDiscovery();
    }

    @Override
    public void onDisable() {
        if (this.listener != null) {
            this.listener.stop();
        }
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
