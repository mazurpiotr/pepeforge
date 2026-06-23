package pepin.pepeforge.weapons.anchor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class AnchorDefinition {

    public static final String ITEM_ID = ItemIds.ANCHOR;
    public static final String LANG_PATH = "anchor";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.anchor";
    public static final ItemNameColor NAME_COLOR = ItemNameColor.IRON;
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.COMMON;

    public static final Material BASE_MATERIAL = Material.IRON_AXE;
    public static final NamespacedKey MODEL_KEY = new NamespacedKey("pepeforge", "anchor");

    public static final double ATTACK_DAMAGE = 8.0D;
    public static final double ATTACK_SPEED = -3.5D;

    public static final long SNARE_COOLDOWN_MILLIS = 5_000L;
    public static final int SNARE_DURATION_TICKS = 40;

    public static final long ABILITY_COOLDOWN_MILLIS = 1_000L;
    public static final double ABILITY_RANGE = 20.0D;
    public static final double PULL_FORCE = 1.2D;
    public static final double PULL_LIFT = 0.35D;
    public static final int FLIGHT_SPEED_TICKS = 10;
    public static final double THROW_SPEED = 1.25D;

    private AnchorDefinition() {
    }
}
