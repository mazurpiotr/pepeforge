package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class KatanaDefinition {

    public static final String ITEM_ID = ItemIds.KATANA;
    public static final String LANG_PATH = "katana";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.katana";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.IRON;
    public static final int LORE_LINE_COUNT = 7;
    public static final ItemRarity RARITY = ItemRarity.RARE;

    public static final Material BASE_MATERIAL = Material.IRON_SWORD;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "katana");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.KATANA;
    public static final NamespacedKey PARRY_MODEL_A_KEY = new NamespacedKey("pepeforge", "katana_parry_a");
    public static final NamespacedKey PARRY_MODEL_B_KEY = new NamespacedKey("pepeforge", "katana_parry_b");
    public static final int PARRY_MODEL_A_DATA = CustomModelDataIds.KATANA_PARRY_A;
    public static final int PARRY_MODEL_B_DATA = CustomModelDataIds.KATANA_PARRY_B;

    public static final double ATTACK_DAMAGE = 5.0;
    public static final double ATTACK_SPEED = 2.4;
    public static final double ATTACK_RANGE_BONUS = 0.75;
    public static final double VISUAL_SCALE = 1.25;

    public static final int PARRY_DURATION_TICKS = 12;
    public static final int PARRY_EXTENSION_TICKS = 4;
    public static final int PARRY_MAX_DURATION_TICKS = 24;
    public static final int COOLDOWN_TICKS = 40;
    public static final double PROJECTILE_SCAN_RADIUS = 3.25D;
    public static final double PARRY_FRONT_DOT_THRESHOLD = 0.10D;
    public static final double PROJECTILE_TOWARD_PLAYER_DOT_THRESHOLD = 0.45D;
    public static final double REFLECT_MIN_SPEED = 1.6D;
    public static final double MELEE_KNOCKBACK_STRENGTH = 0.75D;
    public static final double COUNTERATTACK_DAMAGE = 1.0D;

    public enum VisualState {
        NORMAL,
        PARRY_A,
        PARRY_B
    }

    private KatanaDefinition() {
    }
}
