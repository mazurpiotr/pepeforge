package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Color;
import org.bukkit.Material;
import pepin.pepeforge.item.ItemIds;

public final class CrescentBowDefinition {

    public static final String ITEM_ID = ItemIds.CRESCENT_BOW;
    public static final String LANG_PATH = "crescent_bow";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crescent_bow";
    public static final String NAME_COLOR_NAME = "AQUA";
    public static final int LORE_LINE_COUNT = 6;
    public static final int RARITY_LORE_LINE_INDEX = 5;
    public static final String RARITY_COLOR_NAME = "DARK_PURPLE";
    public static final int CUSTOM_MODEL_DATA = 1001;
    public static final Material BASE_MATERIAL = Material.BOW;
    public static final Material FRAME_MATERIAL = Material.BOW;
    public static final Material CORE_MATERIAL = Material.BREEZE_ROD;
    public static final Material CURVE_MATERIAL = Material.PHANTOM_MEMBRANE;
    public static final double SIDE_ARROW_YAW_DEGREES = 8.0D;
    public static final Color SIDE_ARROW_COLOR = Color.fromRGB(153, 235, 255);

    private CrescentBowDefinition() {
    }
}
