package pepin.pepeforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigMenuListener implements Listener {

    private final PepeForgePlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;

    /** Players who changed at least one setting since opening the config menu. */
    private final Set<UUID> pendingReload = ConcurrentHashMap.newKeySet();

    public ConfigMenuListener(PepeForgePlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
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

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        boolean isConfigMenu = ConfigMenu.isConfigMenu(event.getInventory());
        boolean isItemConfigMenu = ItemConfigMenu.isItemConfigMenu(event.getInventory());

        if (!isConfigMenu && !isItemConfigMenu) {
            return;
        }

        // Only send the reminder when fully leaving the config GUI (not navigating between sub-menus).
        // The player is navigating between sub-menus when the close is immediately followed by opening
        // another config-related inventory. We detect this by checking the next scheduled open — but
        // since Bukkit fires InventoryCloseEvent before InventoryOpenEvent, the simplest safe approach
        // is: only remove and notify on the TOP-LEVEL menu close, which signals the session end.
        if (isConfigMenu && pendingReload.remove(player.getUniqueId())) {
            player.sendMessage(lang.message("messages.config.reload_reminder"));
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
            if (!player.hasPermission("pepeforge.reload") && !player.isOp()) {
                player.sendMessage(lang.message("messages.command.no_permission"));
            } else {
                plugin.reloadPlugin();
                pendingReload.remove(player.getUniqueId());
                player.sendMessage(lang.message("messages.config.reloaded"));
            }
            player.closeInventory();
            return;
        }

        String itemId = itemFactory.getItemId(clicked);
        if (itemId != null) {
            player.openInventory(ItemConfigMenu.create(itemId, itemFactory, plugin));
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
            boolean current = plugin.getConfig().getBoolean(configPath + ".enabled", true);
            plugin.getConfig().set(configPath + ".enabled", !current);
            plugin.saveConfig();
            pendingReload.add(player.getUniqueId());
            refresh = true;
        } else if (event.getSlot() == 15) {
            boolean current = plugin.getConfig().contains(configPath + ".recipe_enabled")
                    ? plugin.getConfig().getBoolean(configPath + ".recipe_enabled")
                    : true;
            plugin.getConfig().set(configPath + ".recipe_enabled", !current);
            plugin.saveConfig();
            pendingReload.add(player.getUniqueId());
            refresh = true;
        } else if ("anchor".equals(itemId)) {
            if (event.getSlot() == 18) {
                plugin.getConfig().set(configPath + ".ability_cooldown", 5000L);
                plugin.getConfig().set(configPath + ".snare_duration", 40);
                plugin.getConfig().set(configPath + ".snare_cooldown", 5000L);
                plugin.getConfig().set(configPath + ".ability_range", 20.0D);
                plugin.getConfig().set(configPath + ".snare_enabled", true);
                plugin.getConfig().set(configPath + ".hook_enabled", true);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 19) {
                boolean current = plugin.getConfig().getBoolean(configPath + ".hook_enabled", true);
                plugin.getConfig().set(configPath + ".hook_enabled", !current);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 20) {
                long current = plugin.getConfig().getLong(configPath + ".ability_cooldown", 5000L);
                long change = event.isLeftClick() ? -500L : 500L;
                long newValue = Math.max(500L, Math.min(30000L, current + change));
                plugin.getConfig().set(configPath + ".ability_cooldown", newValue);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 21) {
                double current = plugin.getConfig().getDouble(configPath + ".ability_range", 20.0D);
                double change = event.isLeftClick() ? -1.0D : 1.0D;
                double newValue = Math.max(5.0D, Math.min(50.0D, current + change));
                plugin.getConfig().set(configPath + ".ability_range", newValue);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 23) {
                boolean current = plugin.getConfig().getBoolean(configPath + ".snare_enabled", true);
                plugin.getConfig().set(configPath + ".snare_enabled", !current);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 24) {
                int current = plugin.getConfig().getInt(configPath + ".snare_duration", 40);
                int change = event.isLeftClick() ? -10 : 10;
                int newValue = Math.max(10, Math.min(200, current + change));
                plugin.getConfig().set(configPath + ".snare_duration", newValue);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            } else if (event.getSlot() == 25) {
                long current = plugin.getConfig().getLong(configPath + ".snare_cooldown", 5000L);
                long change = event.isLeftClick() ? -1000L : 1000L;
                long newValue = Math.max(1000L, Math.min(60000L, current + change));
                plugin.getConfig().set(configPath + ".snare_cooldown", newValue);
                plugin.saveConfig();
                pendingReload.add(player.getUniqueId());
                refresh = true;
            }
        }

        if (event.getSlot() == 26) {
            player.openInventory(ConfigMenu.create(itemFactory));
        }

        if (refresh) {
            player.openInventory(ItemConfigMenu.create(itemId, itemFactory, plugin));
        }
    }
}
