package pepin.pepeforge.weapons.katana;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.combat.TwoHandedEquipmentObserver;
import pepin.pepeforge.util.cooldown.CooldownManager;

public class KatanaModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final TwoHandedEquipmentObserver equipmentObserver;
    private final CooldownManager cooldownManager;

    private KatanaRecipes recipes;
    private KatanaListener listener;
    private KatanaRecipeDiscovery recipeDiscovery;

    public KatanaModule(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang, CooldownManager cooldownManager,
            TwoHandedEquipmentObserver equipmentObserver) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.equipmentObserver = equipmentObserver;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public void onEnable() {
        this.recipes = new KatanaRecipes(plugin, itemFactory);
        this.recipes.registerAll();

        this.listener = new KatanaListener(plugin, itemFactory, lang, cooldownManager);
        this.listener.start();
        equipmentObserver.register(this.listener);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        this.recipeDiscovery = new KatanaRecipeDiscovery();
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
