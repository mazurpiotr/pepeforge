package pepin.pepeforge.item;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemMigrationListener implements Listener {

    private final ItemMigrator migrator;

    public ItemMigrationListener(ItemMigrator migrator) {
        this.migrator = migrator;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!migrator.isActive()) return;
        migrateInventory(event.getPlayer().getInventory());
        migrateInventory(event.getPlayer().getEnderChest());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!migrator.isActive()) return;
        migrateInventory(event.getInventory());
        migrateInventory(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!migrator.isActive()) return;
        ItemStack item = event.getItem().getItemStack();
        if (migrator.migrateItem(item)) {
            event.getItem().setItemStack(item);
        }
    }

    private void migrateInventory(Inventory inventory) {
        if (inventory == null) return;
        
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && migrator.migrateItem(item)) {
                inventory.setItem(i, item);
                changed = true;
            }
        }
    }
}
