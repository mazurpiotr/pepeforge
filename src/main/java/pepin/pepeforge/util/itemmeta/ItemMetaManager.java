package pepin.pepeforge.util.itemmeta;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.env.ServerEnv;

import java.util.List;

public final class ItemMetaManager {

    private ItemMetaManager() {
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        if (ServerEnv.hasDataComponentApi()) {
            PaperItemMetaAdapter.setDisplayName(meta, name);
        } else {
            ItemMetaCompat.setDisplayName(meta, name);
        }
    }

    public static void setItemName(ItemMeta meta, String name) {
        if (ServerEnv.hasDataComponentApi()) {
            PaperItemMetaAdapter.setItemName(meta, name);
        } else {
            ItemMetaCompat.setItemName(meta, name);
        }
    }

    public static String getDisplayName(ItemMeta meta) {
        if (ServerEnv.hasDataComponentApi()) {
            return PaperItemMetaAdapter.getDisplayName(meta);
        } else {
            return ItemMetaCompat.getDisplayName(meta);
        }
    }

    public static String getItemName(ItemMeta meta) {
        if (ServerEnv.hasDataComponentApi()) {
            return PaperItemMetaAdapter.getItemName(meta);
        } else {
            return ItemMetaCompat.getItemName(meta);
        }
    }

    public static void setStringLore(ItemMeta meta, List<String> lore) {
        if (ServerEnv.hasDataComponentApi()) {
            PaperItemMetaAdapter.setLore(meta, lore);
        } else {
            ItemMetaCompat.setStringLore(meta, lore);
        }
    }

    public static void setCustomModelData(ItemMeta meta, int value) {
        ItemMetaCompat.setCustomModelData(meta, value);
    }

    public static String readCustomModelData(ItemMeta meta) {
        return ItemMetaCompat.readCustomModelData(meta);
    }

    public static boolean hasCustomModelData(ItemMeta meta, int targetData) {
        return ItemMetaCompat.hasCustomModelData(meta, targetData);
    }

    public static String readItemModel(ItemMeta meta) {
        return ItemMetaCompat.readItemModel(meta);
    }

    public static void addMainHandAttribute(ItemMeta meta, Attribute attribute, String name, double amount) {
        ItemMetaCompat.addMainHandAttribute(meta, attribute, name, amount);
    }

    public static void setItemModelIfSupported(ItemMeta meta, NamespacedKey itemModelKey) {
        ItemMetaCompat.setItemModelIfSupported(meta, itemModelKey);
    }
}
