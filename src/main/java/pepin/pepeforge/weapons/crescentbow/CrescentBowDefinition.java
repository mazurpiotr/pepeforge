package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class CrescentBowDefinition {

    public static final String ITEM_ID = ItemIds.CRESCENT_BOW;
    public static final String LANG_PATH = "crescent_bow";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crescent_bow";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRESCENT;
    public static final int LORE_LINE_COUNT = 7;
    public static final ItemRarity RARITY = ItemRarity.EPIC;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "crescent_bow");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.CRESCENT_BOW;
    public static final Material BASE_MATERIAL = Material.BOW;
    public static final double SIDE_ARROW_YAW_DEGREES = 6.0D;
    public static final Color SIDE_ARROW_COLOR = Color.fromRGB(153, 235, 255);

    private CrescentBowDefinition() {
    }
}
