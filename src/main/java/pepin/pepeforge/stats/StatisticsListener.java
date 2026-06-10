package pepin.pepeforge.stats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.item.ItemFactory;

public class StatisticsListener implements Listener {

    private final StatisticsManager statsManager;
    private final ItemFactory itemFactory;

    public StatisticsListener(StatisticsManager statsManager, ItemFactory itemFactory) {
        this.statsManager = statsManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (result != null) {
            String itemId = itemFactory.getItemId(result);
            if (itemId != null) {
                // Determine how many operations happen roughly. 
                // A shift click can craft multiple. However calculating the exact number 
                // is complex due to inventory space. For simplicity, we just count the operation 
                // or assume 1 weapon/tool crafted at a time as PepeForge items are unstackable.
                statsManager.incrementCrafted(itemId);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            if (event.getInventory().getResult() != null) {
                result = event.getInventory().getResult();
            }
        }
        if (result != null) {
            String itemId = itemFactory.getItemId(result);
            if (itemId != null) {
                statsManager.incrementCrafted(itemId);
            }
        }
    }
}
