package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.item.ItemFactory;

public final class ItemConfigMenu {

    private ItemConfigMenu() {
    }

    public static Inventory create(String itemId, ItemFactory itemFactory) {
        Inventory inventory = Bukkit.createInventory(new Holder(itemId), 27, ChatColor.DARK_GRAY + "Config: " + itemId);
        
        boolean isEnabled = itemFactory.isItemEnabled(itemId);
        boolean isRecipeEnabled = itemFactory.isRecipeEnabled(itemId);

        ItemStack enabledBtn = new ItemStack(isEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta enabledMeta = enabledBtn.getItemMeta();
        if (enabledMeta != null) {
            enabledMeta.setDisplayName(ChatColor.WHITE + "Item Enabled: " + (isEnabled ? ChatColor.GREEN + "TRUE" : ChatColor.RED + "FALSE"));
            enabledBtn.setItemMeta(enabledMeta);
        }
        inventory.setItem(11, enabledBtn);

        ItemStack recipeBtn = new ItemStack(isRecipeEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta recipeMeta = recipeBtn.getItemMeta();
        if (recipeMeta != null) {
            recipeMeta.setDisplayName(ChatColor.WHITE + "Recipe Enabled: " + (isRecipeEnabled ? ChatColor.GREEN + "TRUE" : ChatColor.RED + "FALSE"));
            recipeBtn.setItemMeta(recipeMeta);
        }
        inventory.setItem(15, recipeBtn);

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.YELLOW + "Back");
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(26, backBtn);

        return inventory;
    }

    public static boolean isItemConfigMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    public static String getItemId(Inventory inventory) {
        if (inventory.getHolder() instanceof Holder holder) {
            return holder.itemId;
        }
        return null;
    }

    private static final class Holder implements InventoryHolder {
        private final String itemId;

        Holder(String itemId) {
            this.itemId = itemId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
