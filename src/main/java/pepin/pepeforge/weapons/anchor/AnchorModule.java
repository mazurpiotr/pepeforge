package pepin.pepeforge.weapons.anchor;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.CooldownManager;

public final class AnchorModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final PluginLang lang;

    private AnchorRecipes recipes;
    private AnchorListener listener;
    private AnchorRecipeDiscoveryListener discoveryListener;

    public AnchorModule(PepeForgePlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.lang = lang;
    }

    @Override
    public void onEnable() {
        this.recipes = new AnchorRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new AnchorListener(plugin, itemFactory, cooldownManager, lang);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.discoveryListener = new AnchorRecipeDiscoveryListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(this.discoveryListener, plugin);
    }

    @Override
    public void onDisable() {
        if (this.listener != null) {
            this.listener.cleanup();
        }
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
