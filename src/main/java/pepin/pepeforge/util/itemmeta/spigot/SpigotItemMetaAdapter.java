package pepin.pepeforge.util.itemmeta.spigot;

import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.util.itemmeta.ItemMetaAdapter;

import java.util.List;

public final class SpigotItemMetaAdapter implements ItemMetaAdapter {

    @Override
    @SuppressWarnings("deprecation")
    public void setDisplayName(ItemMeta meta, String name) {
        meta.setDisplayName(name);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setItemName(ItemMeta meta, String name) {
        meta.setItemName(name);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getDisplayName(ItemMeta meta) {
        return meta.getDisplayName();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getItemName(ItemMeta meta) {
        return meta.getItemName();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setLore(ItemMeta meta, List<String> lore) {
        meta.setLore(lore);
    }
}