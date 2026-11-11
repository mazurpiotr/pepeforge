package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.combat.TwoHandedEquipmentObserver;
import pepin.pepeforge.weapons.obsidianglaive.compat.GlaiveCombatAdapter;

public final class ObsidianGlaiveModule implements ItemModule {
    private final PepeForgePlugin plugin;
    private final ItemFactory factory;
    private final TwoHandedEquipmentObserver equipmentObserver;
    private ObsidianGlaiveRecipes recipes;
    private ObsidianGlaiveListener listener;
    private GlaiveCombatAdapter combat;

    public ObsidianGlaiveModule(PepeForgePlugin plugin, ItemFactory factory,
            TwoHandedEquipmentObserver equipmentObserver) {
        this.plugin = plugin;
        this.factory = factory;
        this.equipmentObserver = equipmentObserver;
    }

    @Override
    public void onEnable() {
        recipes = new ObsidianGlaiveRecipes(plugin, factory);
        recipes.registerAll();
        combat = GlaiveCombatAdapter.create(plugin, factory);
        plugin.getServer().getPluginManager().registerEvents(combat, plugin);
        listener = new ObsidianGlaiveListener(plugin, factory,
                ObsidianGlaiveSettings.fromConfig(plugin.getConfig()), combat);
        listener.start();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        equipmentObserver.register(listener);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            equipmentObserver.unregister(listener);
            listener.stop();
            HandlerList.unregisterAll(listener);
        }
        if (combat != null) {
            combat.stop();
            HandlerList.unregisterAll(combat);
        }
        if (recipes != null) {
            recipes.unregisterAll();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        if (recipes == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (factory.refreshObsidianGlaive(item)) {
                inventory.setItem(slot, item);
            }
        }
        listener.refreshEquipment(player, false);
        recipes.discoverFor(player);
    }
}
