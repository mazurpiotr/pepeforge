package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class StormcleaverDefinition {

    public static final String ITEM_ID = ItemIds.STORMCLEAVER;
    public static final String LANG_PATH = "stormcleaver";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.stormcleaver";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRESCENT;
    public static final int LORE_LINE_COUNT = 8;
    public static final ItemRarity RARITY = ItemRarity.LEGENDARY;
    public static final Material BASE_MATERIAL = Material.MACE;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "stormcleaver");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.STORMCLEAVER;

    public static final double ATTACK_DAMAGE = 9.0D;
    public static final double ATTACK_SPEED = -3.0D;
    public static final String CHARGES_KEY_STRING = "stormcleaver_charges";
    public static final String ACTIVE_DIVE_KEY_STRING = "stormcleaver_active_dive";
    public static final int DEFAULT_CHARGES_REQUIRED = 5;
    public static final double DEFAULT_JUMP_VELOCITY_MULTIPLIER = 1.5D;
    public static final double CHARGE_JUMP_VELOCITY = 0.42D;
    public static final double DIVE_HORIZONTAL_SPEED = 0.45D;
    public static final double DIVE_VERTICAL_SPEED = -1.35D;
    public static final int DIVE_ARMING_TICKS = 2;
    public static final double SHOCKWAVE_RADIUS = 4.0D;
    public static final double SHOCKWAVE_DAMAGE = 6.0D;
    public static final double SHOCKWAVE_KNOCKBACK = 1.1D;
    public static final double SHOCKWAVE_LIFT = 0.35D;

    private StormcleaverDefinition() {
    }
}
