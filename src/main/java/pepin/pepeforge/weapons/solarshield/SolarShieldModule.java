package pepin.pepeforge.weapons.solarshield;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.ui.BossBarManager;

public class SolarShieldModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final BossBarManager bossBarManager;

    private SolarShieldRecipes recipes;
    private SolarShieldListener listener;
    private SolarShieldRecipeDiscovery recipeDiscovery;

    public SolarShieldModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.bossBarManager = bossBarManager;
    }

    @Override
    public void onEnable() {
        this.recipes = new SolarShieldRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new SolarShieldListener(plugin, itemFactory, lang, bossBarManager);
        this.listener.startStatusTask();
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);
        
        this.recipeDiscovery = new SolarShieldRecipeDiscovery(plugin);
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
