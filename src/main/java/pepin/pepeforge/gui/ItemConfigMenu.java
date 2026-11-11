package pepin.pepeforge.gui;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.anchor.AnchorConfig;
import pepin.pepeforge.gui.itemconfig.rapier.RapierConfig;
import pepin.pepeforge.gui.itemconfig.rapier.IceRapierConfig;
import pepin.pepeforge.gui.itemconfig.crescentkhopesh.CrescentKhopeshConfig;
import pepin.pepeforge.gui.itemconfig.emberfang.EmberfangConfig;
import pepin.pepeforge.gui.itemconfig.obsidianglaive.ObsidianGlaiveConfig;
import pepin.pepeforge.gui.itemconfig.greatsword.GreatswordConfig;
import pepin.pepeforge.gui.itemconfig.stormcleaver.StormcleaverConfig;
import pepin.pepeforge.gui.itemconfig.windblade.WindBladeConfig;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.util.ColorUtil;

public final class ItemConfigMenu {

    private ItemConfigMenu() {
    }

    public static @NonNull Inventory create(String itemId, ItemFactory itemFactory, PepeForgePlugin plugin) {
        Inventory inventory = Objects.requireNonNull(Bukkit.createInventory(
                new Holder(itemId),
                27,
                ColorUtil.DARK_GRAY + "Config: " + itemId));

        refresh(inventory, itemFactory, plugin);
        return inventory;
    }

    public static void refresh(Inventory inventory, ItemFactory itemFactory, PepeForgePlugin plugin) {
        String itemId = getItemId(inventory);
        if (itemId == null) {
            throw new IllegalArgumentException("Expected an item configuration inventory");
        }
        inventory.clear();
        buildCommonButtons(itemId, itemFactory, inventory);

        if ("anchor".equals(itemId)) {
            AnchorConfig anchorConfig = new AnchorConfig(plugin);
            anchorConfig.build(inventory);
        }

        if (isWindBlade(itemId)) {
            WindBladeConfig windBladeConfig = new WindBladeConfig(plugin);
            windBladeConfig.build(inventory);
        }

        if (ItemIds.ICE_RAPIER.equals(itemId)) {
            new IceRapierConfig(plugin).build(inventory);
        } else if (isRapier(itemId)) {
            new RapierConfig(plugin).build(inventory);
        }

        if (isGreatsword(itemId)) {
            GreatswordConfig greatswordConfig = new GreatswordConfig(plugin);
            greatswordConfig.build(inventory);
        }

        if ("stormcleaver".equals(itemId)) {
            StormcleaverConfig stormcleaverConfig = new StormcleaverConfig(plugin);
            stormcleaverConfig.build(inventory);
        }

        if (ItemIds.EMBERFANG.equals(itemId)) {
            EmberfangConfig emberfangConfig = new EmberfangConfig(plugin);
            emberfangConfig.build(inventory);
        }

        if (ItemIds.OBSIDIAN_GLAIVE.equals(itemId)) {
            new ObsidianGlaiveConfig(plugin, plugin.getPluginLang()).build(inventory);
        }

        if (ItemIds.CRESCENT_KHOPESH.equals(itemId)) {
            new CrescentKhopeshConfig(plugin).build(inventory);
        }

    }

    private static void buildCommonButtons(String itemId, ItemFactory itemFactory, Inventory inventory) {
        boolean isEnabled = itemFactory.isItemEnabled(itemId);
        boolean isRecipeEnabled = itemFactory.isRecipeEnabled(itemId);

        ItemStack enabledBtn = ConfigIconography.createStateButton(isEnabled);
        ItemMeta enabledMeta = enabledBtn.getItemMeta();
        if (enabledMeta != null) {
            enabledMeta.setDisplayName(ColorUtil.WHITE + "Item Enabled: "
                    + (isEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
            enabledBtn.setItemMeta(enabledMeta);
        }
        inventory.setItem(11, enabledBtn);

        if (hasRecipeToggle(itemId)) {
            ItemStack recipeBtn = ConfigIconography.createStateButton(isRecipeEnabled);
            ItemMeta recipeMeta = recipeBtn.getItemMeta();
            if (recipeMeta != null) {
                recipeMeta.setDisplayName(ColorUtil.WHITE + "Crafting Recipe Enabled: "
                        + (isRecipeEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
                recipeBtn.setItemMeta(recipeMeta);
            }
            inventory.setItem(15, recipeBtn);
        }

        inventory.setItem(26, ConfigIconography.createBackButton());
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

    public static boolean hasRecipeToggle(String itemId) {
        return switch (itemId) {
            case ItemIds.ICE_RAPIER,
                    ItemIds.IRON_RAPIER,
                    ItemIds.DIAMOND_RAPIER,
                    ItemIds.NETHERITE_RAPIER,
                    ItemIds.CRESCENT_BOW,
                    ItemIds.CRESCENT_SPEAR,
                    ItemIds.CRESCENT_KHOPESH,
                    ItemIds.CHISEL,
                    ItemIds.KATANA,
                    ItemIds.IRON_WIND_BLADE,
                    ItemIds.DIAMOND_WIND_BLADE,
                    ItemIds.IRON_GREATSWORD,
                    ItemIds.DIAMOND_GREATSWORD,
                    ItemIds.IRON_SCYTHE,
                    ItemIds.DIAMOND_SCYTHE,
                    ItemIds.SOLAR_SHIELD,
                    ItemIds.ANCHOR,
                    ItemIds.THROWING_KNIFE,
                    ItemIds.STORMCLEAVER,
                    ItemIds.EMBERFANG,
                    ItemIds.OBSIDIAN_GLAIVE -> true;
            default -> false;
        };
    }

    public static boolean isWindBlade(String itemId) {
        return "iron_wind_blade".equals(itemId)
                || "diamond_wind_blade".equals(itemId)
                || "netherite_wind_blade".equals(itemId);
    }

    public static boolean isRapier(String itemId) {
        return ItemIds.ICE_RAPIER.equals(itemId)
                || ItemIds.IRON_RAPIER.equals(itemId)
                || ItemIds.DIAMOND_RAPIER.equals(itemId)
                || ItemIds.NETHERITE_RAPIER.equals(itemId);
    }

    public static boolean isGreatsword(String itemId) {
        return "iron_greatsword".equals(itemId)
                || "diamond_greatsword".equals(itemId)
                || "netherite_greatsword".equals(itemId);
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
