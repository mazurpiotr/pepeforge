package pepin.pepeforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

public final class CustomItemsMenuListener implements Listener {

    private final PluginLang lang;
    private final ItemFactory itemFactory;

    public CustomItemsMenuListener(PluginLang lang, ItemFactory itemFactory) {
        this.lang = lang;
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!CustomItemsMenu.isCustomItemsMenu(event.getView().getTopInventory())) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        String itemId = itemFactory.getItemId(clicked);
        if (itemId == null || !itemFactory.isItemEnabled(itemId)) {
            return;
        }

        ItemStack granted = clicked.clone();
        player.getInventory().addItem(granted).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        player.sendMessage(lang.message("messages.menu.item_received"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!CustomItemsMenu.isCustomItemsMenu(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);
    }
}
