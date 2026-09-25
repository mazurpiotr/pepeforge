package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.env.AdventureReflect;
import pepin.pepeforge.util.env.ServerEnv;

import java.util.List;

public final class DataComponentCompat {

    private static final DataComponentAdapter ADAPTER = createAdapter();

    private DataComponentCompat() {
    }

    public static void applyTranslatableItemTextData(ItemStack item, String nameTranslationKey, String nameColorName,
                                                     List<String> loreTranslationKeys, List<String> loreColorNames) {
        ADAPTER.applyTranslatableItemTextData(item, nameTranslationKey, nameColorName, loreTranslationKeys, loreColorNames);
    }

    public static void applyMaxStackSize(ItemStack item, int maxStackSize) {
        ADAPTER.applyMaxStackSize(item, maxStackSize);
    }

    private static DataComponentAdapter createAdapter() {
        if (ServerEnv.hasDataComponentApi() && AdventureReflect.isSupported()) {
            try {
                return (DataComponentAdapter) Class.forName(
                        "pepin.pepeforge.util.itemmeta.paper.PaperDataComponentAdapter"
                ).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return new pepin.pepeforge.util.itemmeta.spigot.NoOpDataComponentAdapter();
    }
}