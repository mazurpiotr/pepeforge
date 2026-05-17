package pepin.pepeforge.weapons.windblade;

import org.bukkit.Material;
import pepin.pepeforge.item.ItemIds;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WindBladeTier {
    IRON(
            ItemIds.IRON_WIND_BLADE,
            "iron_wind_blade",
            "item.pepeforge.iron_wind_blade",
            "WHITE",
            5,
            4,
            "WHITE",
            Material.IRON_SWORD,
            Material.IRON_INGOT,
            1001,
            5.0,
            2.1,
            false,
            0,
            40
    ),
    DIAMOND(
            ItemIds.DIAMOND_WIND_BLADE,
            "diamond_wind_blade",
            "item.pepeforge.diamond_wind_blade",
            "AQUA",
            5,
            4,
            "BLUE",
            Material.DIAMOND_SWORD,
            Material.DIAMOND,
            1002,
            6.0,
            2.2,
            true,
            0,
            0
    ),
    NETHERITE(
            ItemIds.NETHERITE_WIND_BLADE,
            "netherite_wind_blade",
            "item.pepeforge.netherite_wind_blade",
            "DARK_GRAY",
            6,
            5,
            "DARK_PURPLE",
            Material.NETHERITE_SWORD,
            Material.NETHERITE_INGOT,
            1003,
            7.0,
            2.2,
            true,
            1,
            60
    );

    private static final Map<String, WindBladeTier> BY_ITEM_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(WindBladeTier::itemId, Function.identity()));

    private final String itemId;
    private final String langPath;
    private final String translationKeyBase;
    private final String nameColorName;
    private final int loreLineCount;
    private final int rarityLoreLineIndex;
    private final String rarityColorName;
    private final Material baseMaterial;
    private final Material recipeMaterial;
    private final int customModelData;
    private final double attackDamage;
    private final double attackSpeed;
    private final boolean grantsHoldingSpeed;
    private final int hitSpeedAmplifier;
    private final int hitSpeedDurationTicks;

    WindBladeTier(
            String itemId,
            String langPath,
            String translationKeyBase,
            String nameColorName,
            int loreLineCount,
            int rarityLoreLineIndex,
            String rarityColorName,
            Material baseMaterial,
            Material recipeMaterial,
            int customModelData,
            double attackDamage,
            double attackSpeed,
            boolean grantsHoldingSpeed,
            int hitSpeedAmplifier,
            int hitSpeedDurationTicks
    ) {
        this.itemId = itemId;
        this.langPath = langPath;
        this.translationKeyBase = translationKeyBase;
        this.nameColorName = nameColorName;
        this.loreLineCount = loreLineCount;
        this.rarityLoreLineIndex = rarityLoreLineIndex;
        this.rarityColorName = rarityColorName;
        this.baseMaterial = baseMaterial;
        this.recipeMaterial = recipeMaterial;
        this.customModelData = customModelData;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.grantsHoldingSpeed = grantsHoldingSpeed;
        this.hitSpeedAmplifier = hitSpeedAmplifier;
        this.hitSpeedDurationTicks = hitSpeedDurationTicks;
    }

    public String itemId() {
        return itemId;
    }

    public static WindBladeTier fromItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        return BY_ITEM_ID.get(itemId);
    }

    public String langPath() {
        return langPath;
    }

    public String translationKeyBase() {
        return translationKeyBase;
    }

    public String nameColorName() {
        return nameColorName;
    }

    public int loreLineCount() {
        return loreLineCount;
    }

    public int rarityLoreLineIndex() {
        return rarityLoreLineIndex;
    }

    public String rarityColorName() {
        return rarityColorName;
    }

    public Material baseMaterial() {
        return baseMaterial;
    }

    public Material recipeMaterial() {
        return recipeMaterial;
    }

    public int customModelData() {
        return customModelData;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public boolean grantsHoldingSpeed() {
        return grantsHoldingSpeed;
    }

    public int hitSpeedAmplifier() {
        return hitSpeedAmplifier;
    }

    public int hitSpeedDurationTicks() {
        return hitSpeedDurationTicks;
    }
}
