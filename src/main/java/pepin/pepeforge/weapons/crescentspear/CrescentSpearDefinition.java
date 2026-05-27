package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Material;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;
import pepin.pepeforge.item.ItemIds;

public final class CrescentSpearDefinition {

    public static final String ITEM_ID = ItemIds.CRESCENT_SPEAR;
    public static final String LANG_PATH = "crescent_spear";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crescent_spear";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRESCENT;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.EPIC;

    public static final Material BASE_MATERIAL = Material.DIAMOND_SPEAR;
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.CRESCENT_SPEAR;

    public static final int CHARGE_MAX = 100;
    public static final int CHARGE_PER_HIT = 20;
    public static final int CHARGE_DECAY_PER_INTERVAL = 1;
    public static final int CHARGE_DECAY_DELAY_TICKS = 2 * 20;
    public static final int STATUS_INTERVAL_TICKS = 5;

    public static final int ACTIVE_HIT_COUNT = 2;
    public static final int ACTIVE_FIRST_HIT_DELAY_TICKS = 4;
    public static final int ACTIVE_HIT_INTERVAL_TICKS = 5;
    public static final double ACTIVE_TARGET_RANGE = 4.5D;
    public static final double ACTIVE_PARTICLE_DISTANCE = 1.5D;

    private CrescentSpearDefinition() {
    }
}
