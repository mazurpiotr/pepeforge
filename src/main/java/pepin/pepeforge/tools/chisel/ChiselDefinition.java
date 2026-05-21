package pepin.pepeforge.tools.chisel;

import org.bukkit.Material;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class ChiselDefinition {

    public static final String ITEM_ID = ItemIds.CHISEL;
    public static final String LANG_PATH = "chisel";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.chisel";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.AQUA;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.COMMON;

    public static final Material BASE_MATERIAL = Material.SHEARS;
    public static final int CUSTOM_MODEL_DATA = 1001;
    public static final int DURABILITY_COST = 1;

    public static final Material TOP_MATERIAL = Material.IRON_INGOT;
    public static final Material CORE_MATERIAL = Material.COPPER_INGOT;
    public static final Material HANDLE_MATERIAL = Material.STICK;

    private ChiselDefinition() {
    }
}
