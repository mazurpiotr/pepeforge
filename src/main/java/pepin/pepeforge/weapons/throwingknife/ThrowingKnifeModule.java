package pepin.pepeforge.weapons.throwingknife;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.cooldown.CooldownManager;

public final class ThrowingKnifeModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;

    private ThrowingKnifeRecipes recipes;
    private ThrowingKnifeRecipeDiscoveryListener discoveryListener;
    private ThrowingKnifeListener listener;

    public ThrowingKnifeModule(PepeForgePlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public void onEnable() {
        this.recipes = new ThrowingKnifeRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new ThrowingKnifeListener(plugin, itemFactory, cooldownManager);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.discoveryListener = new ThrowingKnifeRecipeDiscoveryListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(this.discoveryListener, plugin);
    }

    @Override
    public void onDisable() {
        if (this.recipes != null) {
            this.recipes.unregisterAll();
        }
        if (this.listener != null) {
            this.listener.cleanup();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (this.discoveryListener != null) {
            this.discoveryListener.discoverFor(player);
        }
    }
}
