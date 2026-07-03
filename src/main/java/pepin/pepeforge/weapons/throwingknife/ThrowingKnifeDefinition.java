package pepin.pepeforge.weapons.throwingknife;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class ThrowingKnifeDefinition {

    public static final String ITEM_ID = ItemIds.THROWING_KNIFE;
    public static final String LANG_PATH = "throwing_knife";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.throwing_knife";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.IRON;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.COMMON;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "throwing_knife");
    public static final int CUSTOM_MODEL_DATA = CustomModelDataIds.THROWING_KNIFE;
    public static final Material BASE_MATERIAL = Material.IRON_NUGGET;
    public static final double DAMAGE = 5.0D; // slightly lower than a bow
    public static final long COOLDOWN_MILLIS = 400L;

    public static final double ROTATION_YAW = 90.0D;
    public static final double ROTATION_PITCH = 0.0D;
    public static final double ROTATION_ROLL = 45.0D;

    private ThrowingKnifeDefinition() {
    }
}
