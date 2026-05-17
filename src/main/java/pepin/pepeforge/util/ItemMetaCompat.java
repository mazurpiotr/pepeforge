package pepin.pepeforge.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ItemMetaCompat {

    private ItemMetaCompat() {
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        meta.setDisplayName(name);
    }

    public static void setItemName(ItemMeta meta, String name) {
        meta.setItemName(name);
    }

    public static String getDisplayName(ItemMeta meta) {
        return meta.getDisplayName();
    }

    public static String getItemName(ItemMeta meta) {
        return meta.getItemName();
    }

    public static void setLore(ItemMeta meta, List<String> lore) {
        meta.setLore(lore);
    }

    public static void setCustomModelData(ItemMeta meta, int value) {
        meta.setCustomModelData(value);
    }

    public static String readCustomModelData(ItemMeta meta) {
        try {
            Method hasComponent = meta.getClass().getMethod("hasCustomModelDataComponent");
            boolean present = (boolean) hasComponent.invoke(meta);
            if (!present) {
                return meta.hasCustomModelData() ? String.valueOf(meta.getCustomModelData()) : "-";
            }
            Method getComponent = meta.getClass().getMethod("getCustomModelDataComponent");
            Object component = getComponent.invoke(meta);
            Method getFloats = component.getClass().getMethod("getFloats");
            Object floats = getFloats.invoke(component);
            return String.valueOf(floats);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return meta.hasCustomModelData() ? String.valueOf(meta.getCustomModelData()) : "-";
        }
    }

    public static String readItemModel(ItemMeta meta) {
        try {
            Method hasItemModel = meta.getClass().getMethod("hasItemModel");
            boolean present = (boolean) hasItemModel.invoke(meta);
            if (!present) {
                return "-";
            }
            Method getItemModel = meta.getClass().getMethod("getItemModel");
            Object value = getItemModel.invoke(meta);
            return value == null ? "-" : value.toString();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return "unsupported-by-runtime";
        }
    }

    public static void addMainHandAttribute(ItemMeta meta, Attribute attribute, String name, double amount) {
        meta.addAttributeModifier(
                attribute,
                createMainHandAttributeModifier(name, amount)
        );
    }

    public static void setItemModelIfSupported(ItemMeta meta, NamespacedKey itemModelKey) {
        try {
            Method method = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
            method.invoke(meta, itemModelKey);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public static void setTranslatableDisplayNameIfSupported(ItemMeta meta, String key, String colorName) {
        Object component = createTranslatableComponent(key, colorName);
        if (component == null) {
            return;
        }
        invokeComponentMethod(meta, "displayName", component);
    }

    public static void setTranslatableItemNameIfSupported(ItemMeta meta, String key, String colorName) {
        Object component = createTranslatableComponent(key, colorName);
        if (component == null) {
            return;
        }
        invokeComponentMethod(meta, "itemName", component);
    }

    public static void setTranslatableLoreIfSupported(ItemMeta meta, List<String> keys, List<String> colorNames) {
        if (keys.isEmpty()) {
            return;
        }
        List<Object> components = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String colorName = colorNames != null && i < colorNames.size() ? colorNames.get(i) : null;
            Object component = createTranslatableComponent(key, colorName);
            if (component == null) {
                return;
            }
            components.add(component);
        }

        try {
            Method method = meta.getClass().getMethod("lore", List.class);
            method.invoke(meta, components);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
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

    private static void invokeComponentMethod(ItemMeta meta, String methodName, Object component) {
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method method = meta.getClass().getMethod(methodName, componentClass);
            method.invoke(meta, component);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public static void applyTranslatableItemTextDataIfSupported(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames) {
        Object nameComponent = createTranslatableComponent(nameTranslationKey, nameColorName);
        if (nameComponent != null) {
            setItemStackDataComponent(item, "ITEM_NAME", nameComponent);
        }

        if (loreTranslationKeys == null || loreTranslationKeys.isEmpty()) {
            return;
        }

        List<Object> loreComponents = new ArrayList<>();
        for (int i = 0; i < loreTranslationKeys.size(); i++) {
            String translationKey = loreTranslationKeys.get(i);
            String colorName = loreColorNames != null && i < loreColorNames.size() ? loreColorNames.get(i) : null;
            Object component = createTranslatableComponent(translationKey, colorName);
            if (component == null) {
                return;
            }
            loreComponents.add(component);
        }

        try {
            Class<?> itemLoreClass = Class.forName("io.papermc.paper.datacomponent.item.ItemLore");
            Method loreFactory = itemLoreClass.getMethod("lore", List.class);
            Object loreValue = loreFactory.invoke(null, loreComponents);
            setItemStackDataComponent(item, "LORE", loreValue);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public static boolean supportsItemTextDataComponents() {
        try {
            Class<?> dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            dataComponentTypesClass.getField("ITEM_NAME");
            Class<?> valuedTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            ItemStack.class.getMethod("setData", valuedTypeClass, Object.class);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException ignored) {
            return false;
        }
    }

    private static Object createTranslatableComponent(String key, String colorName) {
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatableMethod = componentClass.getMethod("translatable", String.class);
            Object component = translatableMethod.invoke(null, key);
            if (colorName == null || colorName.isBlank()) {
                return component;
            }

            Class<?> textColorClass = Class.forName("net.kyori.adventure.text.format.TextColor");
            Class<?> namedTextColorClass = Class.forName("net.kyori.adventure.text.format.NamedTextColor");
            Object color = namedTextColorClass.getField(colorName).get(null);
            Method colorMethod = component.getClass().getMethod("color", textColorClass);
            return colorMethod.invoke(component, color);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void setItemStackDataComponent(ItemStack item, String fieldName, Object value) {
        try {
            Class<?> dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = dataComponentTypesClass.getField(fieldName).get(null);
            Class<?> valuedTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            Method setData = item.getClass().getMethod("setData", valuedTypeClass, Object.class);
            setData.invoke(item, type, value);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                 | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
