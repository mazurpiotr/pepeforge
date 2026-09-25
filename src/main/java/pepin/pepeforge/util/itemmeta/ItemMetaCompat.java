package pepin.pepeforge.util.itemmeta;

import org.bukkit.attribute.Attribute;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import pepin.pepeforge.util.env.AdventureReflect;
import pepin.pepeforge.util.env.ServerEnv;

import java.util.List;

public final class ItemMetaCompat {

    private static final ItemMetaAdapter ADAPTER = createAdapter();

    private ItemMetaCompat() {
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


    public static void setLore(ItemMeta meta, List<String> lore) {
        ADAPTER.setLore(meta, lore);
    }


    public static void setCustomModelData(ItemMeta meta, int value) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) value));
        meta.setCustomModelDataComponent(component);
    }

    public static String readCustomModelData(ItemMeta meta) {
        if (!meta.hasCustomModelDataComponent()) {
            return "-";
        }
        return String.valueOf(meta.getCustomModelDataComponent().getFloats());
    }

    public static boolean hasCustomModelData(ItemMeta meta, int targetData) {
        if (!meta.hasCustomModelDataComponent()) {
            return false;
        }
        List<Float> floats = meta.getCustomModelDataComponent().getFloats();
        return !floats.isEmpty() && floats.get(0) == (float) targetData;
    }

    public static String readItemModel(ItemMeta meta) {
        if (!meta.hasItemModel()) {
            return "-";
        }
        NamespacedKey key = meta.getItemModel();
        return key == null ? "-" : key.toString();
    }

    public static void addMainHandAttribute(ItemMeta meta, Attribute attribute, String name, double amount) {
        meta.addAttributeModifier(
                attribute,
                AttributeModifierCompat.createMainHandAttributeModifier(name, amount)
        );
    }

    public static void setItemModelIfSupported(ItemMeta meta, NamespacedKey itemModelKey) {
        meta.setItemModel(itemModelKey);
    }

    private static ItemMetaAdapter createAdapter() {
        if (ServerEnv.hasDataComponentApi() && AdventureReflect.isSupported()) {
            try {
                return (ItemMetaAdapter) Class.forName(
                        "pepin.pepeforge.util.itemmeta.paper.PaperItemMetaAdapter"
                ).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return new pepin.pepeforge.util.itemmeta.spigot.SpigotItemMetaAdapter();
    }
}
