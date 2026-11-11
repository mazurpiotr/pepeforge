package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class ObsidianGlaiveDefinition {
    public static final String ITEM_ID = ItemIds.OBSIDIAN_GLAIVE;
    public static final String LANG_PATH = ITEM_ID;
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.obsidian_glaive";
    public static final Material BASE_MATERIAL = Material.DIAMOND_SWORD;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", ITEM_ID);
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.OBSIDIAN_GLAIVE;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.RARE;
    public static final ItemNameColor NAME_COLOR = ItemNameColor.NETHERITE;
    public static final double ATTACK_SPEED = 1.6D;
    public static final int COMBO_TIMEOUT_TICKS = 160;
    public static final double WHACK_RANGE = 4.0D;
    public static final double CLOSE_RANGE = 1.5D;
    public static final double CLOSE_DAMAGE_MULTIPLIER = 0.30D;
    public static final double CLOSE_KNOCKBACK = 0.35D;
    public static final double SWEEP_KNOCKBACK = 0.4D;
    public static final double STAB_MIN_RANGE = 0.0D;
    public static final double STAB_RANGE = 4.5D;
    public static final double STAB_CREATIVE_RANGE = 6.5D;
    public static final double STAB_LIFT = 0.18D;

    private ObsidianGlaiveDefinition() {
    }
}
