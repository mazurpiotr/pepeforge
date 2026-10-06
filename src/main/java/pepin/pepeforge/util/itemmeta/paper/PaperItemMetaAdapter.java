package pepin.pepeforge.util.itemmeta.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.itemmeta.ItemMetaAdapter;

import java.util.List;
import java.util.stream.Collectors;

public final class PaperItemMetaAdapter implements ItemMetaAdapter {

    @Override
    public void setDisplayName(ItemMeta meta, String name) {
        meta.customName(parse(name));
    }

    @Override
    public void setItemName(ItemMeta meta, String name) {
        meta.itemName(parse(name));
    }

    @Override
    public String getDisplayName(ItemMeta meta) {
        Component component = meta.customName();
        return component == null ? null : serialize(component);
    }

    @Override
    public String getItemName(ItemMeta meta) {
        Component component = meta.itemName();
        return component == null ? null : serialize(component);
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lore) {
        if (lore == null) {
            meta.lore(null);
            return;
        }
        meta.lore(lore.stream().map(PaperItemMetaAdapter::parse).collect(Collectors.toList()));
    }

    private static Component parse(String text) {
        return text == null ? null : LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private static String serialize(Component component) {
        return component == null ? null : LegacyComponentSerializer.legacySection().serialize(component);
    }
}