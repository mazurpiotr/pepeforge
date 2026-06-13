package pepin.pepeforge.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PaperDataComponentAdapter {

    private PaperDataComponentAdapter() {
    }


    public static void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames) {
        Component nameComponent = createTranslatableComponent(nameTranslationKey, nameColorName);
        if (nameComponent != null) {
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }

        if (loreTranslationKeys == null || loreTranslationKeys.isEmpty()) {
            return;
        }

        List<Component> loreComponents = new ArrayList<>();
        for (int i = 0; i < loreTranslationKeys.size(); i++) {
            String translationKey = loreTranslationKeys.get(i);
            String colorName = loreColorNames != null && i < loreColorNames.size() ? loreColorNames.get(i) : null;
            Component component = createTranslatableComponent(translationKey, colorName);
            if (component != null) {
                loreComponents.add(component);
            }
        }

        item.setData(DataComponentTypes.LORE, ItemLore.lore(loreComponents));
    }

    public static void applyRawComponents(ItemStack item, Component name, List<Component> lore) {
        if (name != null) {
            item.setData(DataComponentTypes.ITEM_NAME, name);
        }
        if (lore != null) {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }
    }

    private static Component createTranslatableComponent(String key, String colorName) {
        Component component = Component.translatable(key);
        if (colorName == null || colorName.isBlank()) {
            return component;
        }

        TextColor color;
        if (colorName.startsWith("#")) {
            color = TextColor.fromHexString(colorName);
        } else {
            color = NamedTextColor.NAMES.value(colorName);
        }
        if (color != null) {
            component = component.color(color);
        }
        return component;
    }
}
