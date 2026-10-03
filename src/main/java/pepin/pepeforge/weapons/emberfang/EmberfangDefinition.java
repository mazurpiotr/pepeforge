package pepin.pepeforge.weapons.emberfang;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class EmberfangDefinition {

    public static final String ITEM_ID = ItemIds.EMBERFANG;
    public static final String LANG_PATH = "emberfang";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.emberfang";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRIMSON;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.EPIC;
    public static final Material BASE_MATERIAL = Material.DIAMOND_SWORD;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "emberfang");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.EMBERFANG;
    public static final String FIRE_DAMAGE_CONFIG_PATH = "mechanics.emberfang.fire_damage";
    public static final double FIRE_DAMAGE = 2.0D;
    public static final double MIN_FIRE_DAMAGE = 0.0D;
    public static final double MAX_FIRE_DAMAGE = 6.0D;
    public static final double FIRE_DAMAGE_STEP = 0.5D;
    public static final double SELF_BURN_DAMAGE = 0.5D;
    public static final int SELF_BURN_INTERVAL_TICKS = 10;
    public static final double FIRE_PROTECTION_REDUCTION_PER_LEVEL = 0.08D;
    public static final double MAX_FIRE_PROTECTION_REDUCTION = 0.8D;
    public static final double IGNITE_CHANCE = 0.05D;
    public static final int IGNITE_DURATION_TICKS = 40;

    private EmberfangDefinition() {
    }
}
