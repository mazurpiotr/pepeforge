package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

public final class CustomItemsMenu {

    private CustomItemsMenu() {
    }

    public static Inventory create(PluginLang lang, ItemFactory itemFactory) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 18, lang.text("messages.menu.title"));
        int slot = 0;
        for (var item : itemFactory.createAllCustomItems()) {
            inventory.setItem(slot++, item);
        }
        return inventory;
    }

    public static boolean isCustomItemsMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    private static final class Holder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
