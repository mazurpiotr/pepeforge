package pepin.pepeforge.gui;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

public final class CustomItemsMenu {

    private CustomItemsMenu() {
    }

    public static @NonNull Inventory create(PluginLang lang, ItemFactory itemFactory) {
        Holder holder = new Holder();

        Inventory inventory = Objects.requireNonNull(
            Bukkit.createInventory(
                holder,
                InventoryType.CHEST,
                lang.text("messages.menu.title")
        ));

        holder.setInventory(inventory);

        int slot = 0;
        for (var item : itemFactory.createAllCustomItems()) {
            inventory.setItem(slot++, item);
        }

        return inventory;
    }

    private static final class Holder implements InventoryHolder {

        private @Nullable Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Inventory has not been initialized yet");
            }
            return inventory;
        }
    }

    public static boolean isCustomItemsMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }
}
