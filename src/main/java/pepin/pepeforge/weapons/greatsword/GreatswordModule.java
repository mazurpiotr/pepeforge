package pepin.pepeforge.weapons.greatsword;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.combat.TwoHandedEquipmentObserver;

public class GreatswordModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final TwoHandedEquipmentObserver equipmentObserver;

    private GreatswordRecipes recipes;
    private GreatswordListener listener;
    private GreatswordRecipeDiscovery recipeDiscovery;

    public GreatswordModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang,
            TwoHandedEquipmentObserver equipmentObserver) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.equipmentObserver = equipmentObserver;
    }

    @Override
    public void onEnable() {
        this.recipes = new GreatswordRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new GreatswordListener(plugin, itemFactory, lang);
        this.listener.start();
        equipmentObserver.register(this.listener);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.recipeDiscovery = new GreatswordRecipeDiscovery();
    }

    @Override
    public void onDisable() {
        if (this.listener != null) {
            equipmentObserver.unregister(this.listener);
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
