package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public interface DataComponentAdapter {
    void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames);
    void applyMaxStackSize(ItemStack item, int maxStackSize);
}
