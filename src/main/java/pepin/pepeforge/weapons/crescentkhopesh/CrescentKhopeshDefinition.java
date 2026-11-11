package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class CrescentKhopeshDefinition {

    public static final String ITEM_ID = ItemIds.CRESCENT_KHOPESH;
    public static final String LANG_PATH = "crescent_khopesh";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crescent_khopesh";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRESCENT;
    public static final int LORE_LINE_COUNT = 7;
    public static final ItemRarity RARITY = ItemRarity.EPIC;

    public static final Material BASE_MATERIAL = Material.DIAMOND_SWORD;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "crescent_khopesh");
    public static final NamespacedKey WAVE_MODEL_KEY = new NamespacedKey("pepeforge", "moon_wave");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.CRESCENT_KHOPESH;
    public static final double BASE_ATTACK_DAMAGE = 6.0D;

    public static final int DEFAULT_HITS_REQUIRED = 3;
    public static final double WAVE_DAMAGE_MULTIPLIER = 0.4D;
    public static final double DEFAULT_WAVE_RANGE = 4.5D;
    public static final double DEFAULT_WAVE_WIDTH = 2.8D;
    public static final double DEFAULT_WAVE_SPEED = 0.65D;
    public static final double DEFAULT_SWEEP_WIDTH = 3.0D;
    public static final double DEFAULT_SWEEP_RANGE = 2.0D;
    public static final String SHIELD_SOURCE_ID = "moonshield";
    public static final int SHIELD_DURATION_TICKS = 80;
    public static final double SHIELD_ABSORPTION_PER_HIT = 0.5D;
    public static final double DEFAULT_SHIELD_ABSORPTION = 4.0D;

    private CrescentKhopeshDefinition() {
    }
}
