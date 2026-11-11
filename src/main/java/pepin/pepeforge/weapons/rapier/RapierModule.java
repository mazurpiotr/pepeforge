package pepin.pepeforge.weapons.rapier;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.weapons.rapier.compat.RapierAttackCharge;

public final class RapierModule implements ItemModule {
    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private RapierRecipes recipes;
    private RapierListener listener;
    private RapierRecipeDiscovery recipeDiscovery;
    private RapierAttackCharge attackCharge;
    private IceRapierListener iceListener;

    public RapierModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
    }

    @Override
    public void onEnable() {
        recipes = new RapierRecipes(plugin, itemFactory);
        recipes.registerAll();
        RapierSettings settings = RapierSettings.fromConfig(plugin.getConfig());
        attackCharge = settings.frontalBonusEnabled() ? RapierAttackCharge.create() : RapierAttackCharge.bukkit();
        plugin.getServer().getPluginManager().registerEvents(attackCharge, plugin);
        listener = new RapierListener(itemFactory, lang, settings, attackCharge);
        listener.start();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        iceListener = new IceRapierListener(itemFactory, IceRapierSettings.fromConfig(plugin.getConfig()));
        iceListener.start();
        plugin.getServer().getPluginManager().registerEvents(iceListener, plugin);
        recipeDiscovery = new RapierRecipeDiscovery(itemFactory);
    }

    @Override
    public void onDisable() {
        if (iceListener != null) {
            iceListener.stop();
            HandlerList.unregisterAll(iceListener);
        }
        if (listener != null) {
            listener.stop();
            HandlerList.unregisterAll(listener);
        }
        if (recipes != null) {
            recipes.unregisterAll();
        }
        if (attackCharge != null) {
            attackCharge.stop();
            HandlerList.unregisterAll(attackCharge);
        }
        recipeDiscovery = null;
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (recipeDiscovery != null) {
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (itemFactory.refreshRapier(item)) {
                    inventory.setItem(slot, item);
                }
            }
            recipeDiscovery.discoverFor(player);
        }
    }
}
