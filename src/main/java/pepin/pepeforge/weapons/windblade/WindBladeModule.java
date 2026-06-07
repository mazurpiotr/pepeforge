package pepin.pepeforge.weapons.windblade;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.AuraManager;
import pepin.pepeforge.util.CooldownManager;

public class WindBladeModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final CooldownManager cooldownManager;
    private final AuraManager auraManager;

    private WindBladeRecipes recipes;
    private WindBladeListener listener;
    private WindBladeRecipeDiscoveryListener discoveryListener;

    public WindBladeModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang, CooldownManager cooldownManager, AuraManager auraManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.cooldownManager = cooldownManager;
        this.auraManager = auraManager;
    }

    @Override
    public void onEnable() {
        this.recipes = new WindBladeRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new WindBladeListener(plugin, itemFactory, lang, cooldownManager, auraManager);
        this.listener.startHoldingTask();
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.discoveryListener = new WindBladeRecipeDiscoveryListener(plugin, itemFactory);
        plugin.getServer().getPluginManager().registerEvents(this.discoveryListener, plugin);
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
        if (this.discoveryListener != null) {
            this.discoveryListener.discoverFor(player);
        }
    }
}
