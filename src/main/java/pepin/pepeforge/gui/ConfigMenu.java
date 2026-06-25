package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.item.ItemFactory;

public final class ConfigMenu {

    private ConfigMenu() {
    }

    public static Inventory create(ItemFactory itemFactory) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 54, ChatColor.DARK_GRAY + "Configuration");
        int slot = 0;
        for (String itemId : itemFactory.getAllCanonicalIds()) {
            ItemStack item = itemFactory.createByName(itemId);
            if (item != null) {
                // Add a lore line indicating if it's currently disabled
                if (!itemFactory.isItemEnabled(itemId)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        java.util.List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
                        lore.add(0, ChatColor.RED + "DISABLED");
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
            meta.setDisplayName(ChatColor.YELLOW + "Reload Plugin");
            reloadBtn.setItemMeta(meta);
        }
        inventory.setItem(53, reloadBtn);

        return inventory;
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
