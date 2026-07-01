package pepin.pepeforge.util.itemmeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public final class PaperItemMetaAdapter {

    private PaperItemMetaAdapter() {
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        meta.displayName(parse(name));
    }

    public static void setItemName(ItemMeta meta, String name) {
        meta.itemName(parse(name));
    }

    public static String getDisplayName(ItemMeta meta) {
        Component cmp = meta.displayName();
        return cmp == null ? null : serialize(cmp);
    }

    public static String getItemName(ItemMeta meta) {
        Component cmp = meta.itemName();
        return cmp == null ? null : serialize(cmp);
    }

    public static void setLore(ItemMeta meta, List<String> lore) {
        if (lore == null) {
            meta.lore(null);
            return;
        }
        meta.lore(lore.stream().map(PaperItemMetaAdapter::parse).collect(Collectors.toList()));
    }

    public static List<String> getLore(ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            return null;
        }
        return lore.stream().map(PaperItemMetaAdapter::serialize).collect(Collectors.toList());
    }

    private static Component parse(String text) {
        if (text == null) return null;
        // Use legacySection to properly parse strings that already went through ChatColor translation
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private static String serialize(Component component) {
        if (component == null) return null;
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
