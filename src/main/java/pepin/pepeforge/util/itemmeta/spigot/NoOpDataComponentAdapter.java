package pepin.pepeforge.util.itemmeta.spigot;

import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.itemmeta.DataComponentAdapter;

import java.util.List;

public final class NoOpDataComponentAdapter implements DataComponentAdapter {

    @Override
    public void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName,
                                              List<String> loreTranslationKeys, List<String> loreColorNames) {
    }

    @Override
    public void applyMaxStackSize(ItemStack item, int maxStackSize) {
    }
}