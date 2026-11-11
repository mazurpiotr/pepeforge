package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.aura.AuraManager;
import pepin.pepeforge.util.combat.TemporaryShieldManager;

public final class CrescentKhopeshModule implements ItemModule {
    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final TemporaryShieldManager shield;
    private final AuraManager auraManager;
    private CrescentKhopeshRecipes recipes;
    private CrescentKhopeshRecipeDiscovery recipeDiscovery;
    private CrescentKhopeshListener listener;
    private Moonshield legacyShieldCleanup;

    public CrescentKhopeshModule(PepeForgePlugin plugin, ItemFactory itemFactory, TemporaryShieldManager shield,
            AuraManager auraManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.shield = shield;
        this.auraManager = auraManager;
    }

    @Override
    public void onEnable() {
        recipes = new CrescentKhopeshRecipes(plugin, itemFactory);
        recipes.registerAll();
        recipeDiscovery = new CrescentKhopeshRecipeDiscovery();
        legacyShieldCleanup = new Moonshield(plugin);
        listener = new CrescentKhopeshListener(plugin, itemFactory, shield, auraManager);
        plugin.getServer().getPluginManager().registerEvents(legacyShieldCleanup, plugin);
        legacyShieldCleanup.cleanupOnlinePlayers();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();
        }
        shield.clearSource(CrescentKhopeshDefinition.SHIELD_SOURCE_ID);
        if (legacyShieldCleanup != null) {
            legacyShieldCleanup.stop();
        }
        if (recipes != null) {
            recipes.unregisterAll();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (recipeDiscovery != null) {
            recipeDiscovery.discoverFor(player);
        }
    }
}
