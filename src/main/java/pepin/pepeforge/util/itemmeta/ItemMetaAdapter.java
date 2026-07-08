package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public interface ItemMetaAdapter {
    void setDisplayName(ItemMeta meta, String name);
    void setItemName(ItemMeta meta, String name);
    String getDisplayName(ItemMeta meta);
    String getItemName(ItemMeta meta);
    void setLore(ItemMeta meta, List<String> lore);
}
