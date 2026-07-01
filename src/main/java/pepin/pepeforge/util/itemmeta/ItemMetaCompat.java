package pepin.pepeforge.util.itemmeta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ItemMetaCompat {

    private ItemMetaCompat() {
    }

    @Deprecated
    public static void setDisplayName(ItemMeta meta, String name) {
        meta.setDisplayName(name);
    }


    @Deprecated
    public static void setItemName(ItemMeta meta, String name) {
        meta.setItemName(name);
    }


    @Deprecated
    public static String getDisplayName(ItemMeta meta) {
        return meta.getDisplayName();
    }


    @Deprecated
    public static String getItemName(ItemMeta meta) {
        return meta.getItemName();
    }


    @Deprecated
    public static void setStringLore(ItemMeta meta, List<String> lore) {
        meta.setLore(lore);
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
                createMainHandAttributeModifier(name, amount)
        );
    }

    public static void setItemModelIfSupported(ItemMeta meta, NamespacedKey itemModelKey) {
        meta.setItemModel(itemModelKey);
    }

    private static AttributeModifier createMainHandAttributeModifier(String name, double amount) {
        NamespacedKey key = NamespacedKey.fromString("pepeforge:" + normalizeKey(name));
        if (key != null) {
            try {
                Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                        NamespacedKey.class,
                        double.class,
                        AttributeModifier.Operation.class,
                        EquipmentSlotGroup.class
                );
                return constructor.newInstance(
                        key,
                        amount,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                );
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        try {
            Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class,
                    String.class,
                    double.class,
                    AttributeModifier.Operation.class,
                    EquipmentSlotGroup.class
            );
            return constructor.newInstance(
                    UUID.nameUUIDFromBytes(name.getBytes()),
                    name,
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to create attribute modifier for " + name, exception);
        }
    }

    private static String normalizeKey(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
