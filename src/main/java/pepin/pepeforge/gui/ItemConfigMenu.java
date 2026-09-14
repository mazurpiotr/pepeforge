package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.anchor.AnchorConfig;
import pepin.pepeforge.gui.itemconfig.stormcleaver.StormcleaverConfig;
import pepin.pepeforge.gui.itemconfig.windblade.WindBladeConfig;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.ColorUtil;

public final class ItemConfigMenu {

    private ItemConfigMenu() {
    }

    public static Inventory create(String itemId, ItemFactory itemFactory, PepeForgePlugin plugin) {
        Inventory inventory = Bukkit.createInventory(new Holder(itemId), 27, ColorUtil.DARK_GRAY + "Config: " + itemId);

        buildCommonButtons(itemId, itemFactory, inventory);

        if ("anchor".equals(itemId)) {
            AnchorConfig anchorConfig = new AnchorConfig(plugin);
            anchorConfig.build(inventory);
        }

        if (isWindBlade(itemId)) {
            WindBladeConfig windBladeConfig = new WindBladeConfig(plugin);
            windBladeConfig.build(inventory);
        }

        if ("stormcleaver".equals(itemId)) {
            StormcleaverConfig stormcleaverConfig = new StormcleaverConfig(plugin);
            stormcleaverConfig.build(inventory);
        }

        return inventory;
    }

    private static void buildCommonButtons(String itemId, ItemFactory itemFactory, Inventory inventory) {
        boolean isEnabled = itemFactory.isItemEnabled(itemId);
        boolean isRecipeEnabled = itemFactory.isRecipeEnabled(itemId);

        ItemStack enabledBtn = new ItemStack(isEnabled ? ConfigIconography.ENABLED : ConfigIconography.DISABLED);
        ItemMeta enabledMeta = enabledBtn.getItemMeta();
        if (enabledMeta != null) {
            enabledMeta.setDisplayName(ColorUtil.WHITE + "Item Enabled: "
                    + (isEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
            enabledBtn.setItemMeta(enabledMeta);
        }
        inventory.setItem(11, enabledBtn);

        ItemStack recipeBtn = new ItemStack(isRecipeEnabled ? ConfigIconography.ENABLED : ConfigIconography.DISABLED);
        ItemMeta recipeMeta = recipeBtn.getItemMeta();
        if (recipeMeta != null) {
            recipeMeta.setDisplayName(ColorUtil.WHITE + "Recipe Enabled: "
                    + (isRecipeEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
            recipeBtn.setItemMeta(recipeMeta);
        }
        inventory.setItem(15, recipeBtn);

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ColorUtil.YELLOW + "Back");
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(26, backBtn);
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

    public static boolean isWindBlade(String itemId) {
        return "iron_wind_blade".equals(itemId)
                || "diamond_wind_blade".equals(itemId)
                || "netherite_wind_blade".equals(itemId);
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
