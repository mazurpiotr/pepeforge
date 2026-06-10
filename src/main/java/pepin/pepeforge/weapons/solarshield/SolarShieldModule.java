package pepin.pepeforge.weapons.solarshield;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;

public class SolarShieldModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;

    private SolarShieldRecipes recipes;
    private SolarShieldListener listener;

    public SolarShieldModule(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @Override
    public void onEnable() {
        this.recipes = new SolarShieldRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new SolarShieldListener(plugin, itemFactory);
        this.listener.startStatusTask();
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);
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
        // Not specifically handling discovery events in this file, handled via global discovery mechanism
    }
}
