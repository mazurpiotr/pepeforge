package pepin.pepeforge.util.itemmeta;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public final class AttributeModifierCompat {

    private AttributeModifierCompat() {
    }

    public static AttributeModifier createMainHandAttributeModifier(String name, double amount) {
        NamespacedKey key = NamespacedKey.fromString("pepeforge:" + normalizeKey(name));
        if (key != null) {
            try {
                Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                        NamespacedKey.class, double.class, AttributeModifier.Operation.class, EquipmentSlotGroup.class);
                return constructor.newInstance(key, amount, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        try {
            Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlotGroup.class);
            return constructor.newInstance(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name, amount,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to create attribute modifier for " + name, exception);
        }
    }

    private static String normalizeKey(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}