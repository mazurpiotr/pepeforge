package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class SpigotItemMetaAdapterImpl implements ItemMetaAdapter {

    @Override
    public void setDisplayName(ItemMeta meta, String name) {
        ItemMetaCompat.setDisplayName(meta, name);
    }

    @Override
    public void setItemName(ItemMeta meta, String name) {
        ItemMetaCompat.setItemName(meta, name);
    }

    @Override
    public String getDisplayName(ItemMeta meta) {
        return ItemMetaCompat.getDisplayName(meta);
    }

    @Override
    public String getItemName(ItemMeta meta) {
        return ItemMetaCompat.getItemName(meta);
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lore) {
        ItemMetaCompat.setStringLore(meta, lore);
    }
}
