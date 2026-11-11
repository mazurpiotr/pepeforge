package pepin.pepeforge.gui;

import java.util.Objects;
import java.util.List;

import org.jspecify.annotations.NonNull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.item.ItemFactory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ColorUtil;

public final class ConfigMenu {
    public static final int METRICS_SLOT = 51;

    private ConfigMenu() {
    }

    public static @NonNull Inventory create(ItemFactory itemFactory, JavaPlugin plugin, PluginLang lang) {
        Inventory inventory = Objects.requireNonNull(Bukkit.createInventory(
                new Holder(),
                54,
                ColorUtil.DARK_GRAY + "Configuration"));

        int slot = 0;
        for (String itemId : itemFactory.getAllCanonicalIds()) {
            ItemStack item = itemFactory.createByName(itemId);
            if (item != null) {
                // Add a lore line indicating if it's currently disabled
                if (!itemFactory.isItemEnabled(itemId)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        java.util.List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
                        lore.add(0, ColorUtil.RED + "DISABLED");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
                inventory.setItem(slot++, item);
            }
        }

        ItemStack reloadBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = reloadBtn.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.YELLOW + "Reload Plugin");
            reloadBtn.setItemMeta(meta);
        }
        inventory.setItem(53, reloadBtn);
        updateMetricsButton(inventory, plugin, lang);

        return inventory;
    }

    public static void updateMetricsButton(Inventory inventory, JavaPlugin plugin, PluginLang lang) {
        boolean enabled = plugin.getConfig().getBoolean("metrics.enabled", true);
        ItemStack button = ConfigIconography.createStateButton(enabled);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.text("messages.config.metrics.title"));
            meta.setLore(List.of(
                    lang.text("messages.config.metrics.description"),
                    lang.text("messages.config.metrics." + (enabled ? "enabled" : "disabled")),
                    lang.text("messages.config.metrics." + (enabled ? "disable" : "enable")),
                    lang.text("messages.config.metrics.immediate")));
            button.setItemMeta(meta);
        }
        inventory.setItem(METRICS_SLOT, button);
    }

    public static boolean isConfigMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
