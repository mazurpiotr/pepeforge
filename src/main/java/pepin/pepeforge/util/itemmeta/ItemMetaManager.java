package pepin.pepeforge.util.itemmeta;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.env.AdventureReflect;
import pepin.pepeforge.util.env.ServerEnv;

import java.util.List;

public final class ItemMetaManager {

    private static final ItemMetaAdapter ADAPTER;

    static {
        ItemMetaAdapter temp = null;
        if (ServerEnv.hasDataComponentApi() && AdventureReflect.isSupported()) {
            try {
                temp = (ItemMetaAdapter) Class.forName("pepin.pepeforge.util.itemmeta.PaperItemMetaAdapterImpl")
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        ADAPTER = temp != null ? temp : new SpigotItemMetaAdapterImpl();
    }

    private ItemMetaManager() {
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        ADAPTER.setDisplayName(meta, name);
    }

    public static void setItemName(ItemMeta meta, String name) {
        ADAPTER.setItemName(meta, name);
    }

    public static String getDisplayName(ItemMeta meta) {
        return ADAPTER.getDisplayName(meta);
    }

    public static String getItemName(ItemMeta meta) {
        return ADAPTER.getItemName(meta);
    }

    public static void setStringLore(ItemMeta meta, List<String> lore) {
        ADAPTER.setLore(meta, lore);
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
