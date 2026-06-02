package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.Material;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class CrimsonSwordDefinition {

    public static final String ITEM_ID = ItemIds.CRIMSON_SWORD;
    public static final String LANG_PATH = "crimson_sword";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.crimson_sword";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRIMSON;
    public static final int LORE_LINE_COUNT = 8;
    public static final ItemRarity RARITY = ItemRarity.LEGENDARY;

    public static final Material BASE_MATERIAL = Material.DIAMOND_SWORD;
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.CRIMSON_SWORD;
    public static final double ATTACK_DAMAGE = 7.0D;
    public static final double ATTACK_SPEED = 1.6D;

    public static final String XP_KEY = "crimson_sword_xp";
    public static final String LEVEL_KEY = "crimson_sword_level";
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 30;
    public static final double BASE_XP = 100.0D;
    public static final double XP_CURVE_MULTIPLIER = 1.15D;

    public static final double LEVEL_10_DAMAGE_BONUS = 0.10D;
    public static final double LEVEL_20_DAMAGE_BONUS = 0.15D;
    public static final double LEVEL_30_DAMAGE_BONUS = 0.20D;
    public static final double LEVEL_10_LIFESTEAL = 0.05D;
    public static final double LEVEL_20_LIFESTEAL = 0.075D;
    public static final double LEVEL_30_LIFESTEAL = 0.10D;
    public static final int LEVEL_10_AURA_TICKS = 10 * 20;
    public static final int LEVEL_20_AURA_TICKS = 20 * 20;
    public static final int LEVEL_30_AURA_TICKS = 30 * 20;
    public static final double CHAIN_DAMAGE_BONUS_PER_STACK = 0.02D;
    public static final int CHAIN_MAX_STACKS = 5;

    private CrimsonSwordDefinition() {
    }
}
