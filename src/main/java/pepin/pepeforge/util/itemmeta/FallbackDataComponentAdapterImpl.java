package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public final class FallbackDataComponentAdapterImpl implements DataComponentAdapter {

    @Override
    public void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames) {
        // No-op on Spigot/Bukkit
    }

    @Override
    public void applyMaxStackSize(ItemStack item, int maxStackSize) {
        // No-op on Spigot/Bukkit
    }
}
