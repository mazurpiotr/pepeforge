package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public final class SolarShieldDefinition {

    public static final String ITEM_ID = ItemIds.SOLAR_SHIELD;
    public static final Material BASE_MATERIAL = Material.SHIELD;
    public static final String LANG_PATH = "solar_shield";
    public static final String TRANSLATION_KEY_BASE = "item.pepeforge.solar_shield";
    public static final int LORE_LINE_COUNT = 6;
    public static final ItemRarity RARITY = ItemRarity.LEGENDARY;
    public static final ItemNameColor NAME_COLOR = ItemNameColor.CRESCENT;

    public static final String CHARGES_KEY_STRING = "solar_shield_charges";
    
    // Model Keys per charge state
    public static final NamespacedKey MODEL_KEY_0 = new NamespacedKey("pepeforge", "solar_shield_0");
    public static final NamespacedKey MODEL_KEY_1 = new NamespacedKey("pepeforge", "solar_shield_1");
    public static final NamespacedKey MODEL_KEY_2 = new NamespacedKey("pepeforge", "solar_shield_2");
    public static final NamespacedKey MODEL_KEY_3 = new NamespacedKey("pepeforge", "solar_shield_3");

    // Custom Model Data fallbacks
    public static final int CUSTOM_MODEL_DATA_0 = 311000;
    public static final int CUSTOM_MODEL_DATA_1 = 311001;
    public static final int CUSTOM_MODEL_DATA_2 = 311002;
    public static final int CUSTOM_MODEL_DATA_3 = 311003;

    public static final int MAX_CHARGES = 3;
    public static final int CHARGE_TICKS = 300; // 15 seconds (1 tick = 50ms)
    public static final int DISCHARGE_TICKS = 400; // 20 seconds
    public static final double OVERCHARGE_BUFFER = 0.20; // 20% extra buffer to prevent flickering

    private SolarShieldDefinition() {
    }
}
