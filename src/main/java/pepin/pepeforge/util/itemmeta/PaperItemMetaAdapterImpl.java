package pepin.pepeforge.util.itemmeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public final class PaperItemMetaAdapterImpl implements ItemMetaAdapter {

    @Override
    public void setDisplayName(ItemMeta meta, String name) {
        meta.displayName(parse(name));
    }

    @Override
    public void setItemName(ItemMeta meta, String name) {
        meta.itemName(parse(name));
    }

    @Override
    public String getDisplayName(ItemMeta meta) {
        Component cmp = meta.displayName();
        return cmp == null ? null : serialize(cmp);
    }

    @Override
    public String getItemName(ItemMeta meta) {
        Component cmp = meta.itemName();
        return cmp == null ? null : serialize(cmp);
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lore) {
        if (lore == null) {
            meta.lore(null);
            return;
        }
        meta.lore(lore.stream().map(this::parse).collect(Collectors.toList()));
    }

    private Component parse(String text) {
        if (text == null) return null;
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private String serialize(Component component) {
        if (component == null) return null;
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
