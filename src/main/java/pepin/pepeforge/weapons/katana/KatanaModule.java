package pepin.pepeforge.weapons.katana;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.cooldown.CooldownManager;

public class KatanaModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final CooldownManager cooldownManager;

    private KatanaRecipes recipes;
    private KatanaListener listener;
    private KatanaRecipeDiscoveryListener discoveryListener;

    public KatanaModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public void onEnable() {
        this.recipes = new KatanaRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new KatanaListener(plugin, itemFactory, lang, cooldownManager);
        this.listener.startStatusTask();
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.discoveryListener = new KatanaRecipeDiscoveryListener(plugin);
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
