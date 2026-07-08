package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public final class PaperDataComponentAdapterImpl implements DataComponentAdapter {

    @Override
    public void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames) {
        PaperDataComponentAdapter.applyTranslatableItemTextData(item, nameTranslationKey, nameColorName, loreTranslationKeys, loreColorNames);
    }

    @Override
    public void applyMaxStackSize(ItemStack item, int maxStackSize) {
        PaperDataComponentAdapter.applyMaxStackSize(item, maxStackSize);
    }
}
