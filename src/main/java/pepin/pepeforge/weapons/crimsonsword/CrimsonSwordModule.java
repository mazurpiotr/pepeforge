package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.entity.Player;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.util.aura.AuraManager;

public class CrimsonSwordModule implements ItemModule {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final CrimsonSwordManager crimsonSwordManager;
    private final AuraManager auraManager;

    private CrimsonSwordListener listener;

    public CrimsonSwordModule(PepeForgePlugin plugin, ItemFactory itemFactory, CrimsonSwordManager crimsonSwordManager, AuraManager auraManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.crimsonSwordManager = crimsonSwordManager;
        this.auraManager = auraManager;
    }

    @Override
    public void onEnable() {
        this.listener = new CrimsonSwordListener(itemFactory, crimsonSwordManager, auraManager);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);
    }

    @Override
    public void onDisable() {
        if (this.listener != null) {
            this.listener.stop();
        }
    }

    @Override
    public void discoverRecipesFor(Player player) {
        // Crimson sword has no specific recipes managed dynamically right now
    }
}
