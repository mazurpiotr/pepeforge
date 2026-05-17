package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Material;
import pepin.pepeforge.item.ItemIds;

public final class CrescentSpearDefinition {

    public static final String ITEM_ID = ItemIds.CRESCENT_SPEAR;
    public static final String LANG_PATH = "crescent_spear";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crescent_spear";
    public static final String NAME_COLOR_NAME = "AQUA";
    public static final int LORE_LINE_COUNT = 6;
    public static final int RARITY_LORE_LINE_INDEX = 5;
    public static final String RARITY_COLOR_NAME = "DARK_PURPLE";

    public static final Material BASE_MATERIAL = Material.IRON_SPEAR;
    public static final int CUSTOM_MODEL_DATA = 1001;

    public static final Material BLADE_MATERIAL = Material.AMETHYST_SHARD;
    public static final Material SHAFT_MATERIAL = Material.STICK;
    public static final Material HANDLE_MATERIAL = Material.STICK;

    public static final int HIT_COMBO_TRIGGER = 3;
    public static final int COMBO_WINDOW_TICKS = 5 * 20;
    public static final int PROC_FEEDBACK_TICKS = 20;
    public static final double PROC_DAMAGE_BONUS = 3.0D;
    public static final double LAUNCH_UPWARD_VELOCITY = 1.0D;
    public static final int SPEED_DURATION_TICKS = 15 * 20;
    public static final int SPEED_AMPLIFIER = 0;

    private CrescentSpearDefinition() {
    }
}
