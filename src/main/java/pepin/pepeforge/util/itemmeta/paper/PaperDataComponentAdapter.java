package pepin.pepeforge.util.itemmeta.paper;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import pepin.pepeforge.util.itemmeta.DataComponentAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PaperDataComponentAdapter implements DataComponentAdapter {

    @Override
    @SuppressWarnings("null")
    public void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName,
                                              List<String> loreTranslationKeys, List<String> loreColorNames) {
        Component nameComponent = createTranslatableComponent(nameTranslationKey, nameColorName);
        if (nameComponent != null) {
            item.setData(DataComponentTypes.ITEM_NAME, nameComponent);
        }
        if (loreTranslationKeys == null || loreTranslationKeys.isEmpty()) {
            return;
        }
        List<Component> loreComponents = new ArrayList<>();
        for (int i = 0; i < loreTranslationKeys.size(); i++) {
            String colorName = loreColorNames != null && i < loreColorNames.size() ? loreColorNames.get(i) : null;
            Component component = createTranslatableComponent(loreTranslationKeys.get(i), colorName);
            if (component != null) {
                loreComponents.add(component);
            }
        }
        @NonNull ItemLore itemLore = Objects.requireNonNull(ItemLore.lore(loreComponents));
        item.setData(DataComponentTypes.LORE, itemLore);
    }

    @SuppressWarnings("null")
    public static void applyRawComponents(ItemStack item, Component name, List<Component> lore) {
        if (name != null) {
            item.setData(DataComponentTypes.ITEM_NAME, name);
        }
        if (lore != null) {
            @NonNull ItemLore itemLore = Objects.requireNonNull(ItemLore.lore(lore));
            item.setData(DataComponentTypes.LORE, itemLore);
        }
    }

    @Override
    public void applyMaxStackSize(ItemStack item, int maxStackSize) {
        item.setData(DataComponentTypes.MAX_STACK_SIZE, maxStackSize);
    }

    private static Component createTranslatableComponent(String key, String colorName) {
        Component component = Component.translatable(Objects.requireNonNull(key));
        if (colorName == null || colorName.isBlank()) {
            return component;
        }
        TextColor color = colorName.startsWith("#")
                ? TextColor.fromHexString(colorName)
                : NamedTextColor.NAMES.value(colorName);
        return color == null ? component : component.color(color);
    }
}