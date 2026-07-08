package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.env.AdventureReflect;
import pepin.pepeforge.util.env.ServerEnv;

import java.util.List;

public final class DataComponentManager {

    private static final DataComponentAdapter ADAPTER;

    static {
        DataComponentAdapter temp = null;
        if (ServerEnv.hasDataComponentApi() && AdventureReflect.isSupported()) {
            try {
                temp = (DataComponentAdapter) Class.forName("pepin.pepeforge.util.itemmeta.PaperDataComponentAdapterImpl")
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        ADAPTER = temp != null ? temp : new FallbackDataComponentAdapterImpl();
    }

    private DataComponentManager() {
    }

    public static void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName, List<String> loreTranslationKeys, List<String> loreColorNames) {
        ADAPTER.applyTranslatableItemTextData(item, nameTranslationKey, nameColorName, loreTranslationKeys, loreColorNames);
    }

    public static void applyMaxStackSize(ItemStack item, int maxStackSize) {
        ADAPTER.applyMaxStackSize(item, maxStackSize);
    }
}
