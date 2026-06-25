package pepin.pepeforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;

public final class ConfigMenuListener implements Listener {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;

    public ConfigMenuListener(PepeForgePlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (ConfigMenu.isConfigMenu(event.getView().getTopInventory())) {
            handleConfigMenuClick(event);
        } else if (ItemConfigMenu.isItemConfigMenu(event.getView().getTopInventory())) {
            handleItemConfigMenuClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (ConfigMenu.isConfigMenu(event.getView().getTopInventory())
                || ItemConfigMenu.isItemConfigMenu(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    private void handleConfigMenuClick(InventoryClickEvent event) {
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

        if (event.getSlot() == 53) {
            player.performCommand("pepeforge reload");
            player.closeInventory();
            return;
        }

        String itemId = itemFactory.getItemId(clicked);
        if (itemId != null) {
            player.openInventory(ItemConfigMenu.create(itemId, itemFactory));
        }
    }

    private void handleItemConfigMenuClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        String itemId = ItemConfigMenu.getItemId(event.getView().getTopInventory());
        if (itemId == null) {
            return;
        }

        String configPath = "items." + itemId;
        boolean refresh = false;

        if (event.getSlot() == 11) {
            // Toggle item enabled
            boolean current = plugin.getConfig().getBoolean(configPath + ".enabled", true);
            plugin.getConfig().set(configPath + ".enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 15) {
            // Toggle recipe enabled
            boolean current = plugin.getConfig().contains(configPath + ".recipe_enabled")
                    ? plugin.getConfig().getBoolean(configPath + ".recipe_enabled")
                    : true;
            plugin.getConfig().set(configPath + ".recipe_enabled", !current);
            plugin.saveConfig();
            refresh = true;
        } else if (event.getSlot() == 26) {
            // Back
            player.openInventory(ConfigMenu.create(itemFactory));
        }

        if (refresh) {
            player.openInventory(ItemConfigMenu.create(itemId, itemFactory));
        }
    }
}
