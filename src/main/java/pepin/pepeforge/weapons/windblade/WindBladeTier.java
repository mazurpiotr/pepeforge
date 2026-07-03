package pepin.pepeforge.weapons.windblade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum WindBladeTier {
    IRON(
            ItemIds.IRON_WIND_BLADE,
            "iron_wind_blade",
            "item.pepeforge.iron_wind_blade",
            ItemNameColor.IRON,
            6,
            ItemRarity.COMMON,
            Material.IRON_SWORD,
            Material.IRON_INGOT,
            Material.BREEZE_ROD,
            new NamespacedKey("pepeforge", "iron_wind_blade"),
            CustomModelDataIds.IRON_WIND_BLADE,
            5.0,
            2.1,
            false,
            0,
            40),
    DIAMOND(
            ItemIds.DIAMOND_WIND_BLADE,
            "diamond_wind_blade",
            "item.pepeforge.diamond_wind_blade",
            ItemNameColor.DIAMOND,
            6,
            ItemRarity.RARE,
            Material.DIAMOND_SWORD,
            Material.DIAMOND,
            Material.BREEZE_ROD,
            new NamespacedKey("pepeforge", "diamond_wind_blade"),
            CustomModelDataIds.DIAMOND_WIND_BLADE,
            6.0,
            2.2,
            true,
            0,
            0),
    NETHERITE(
            ItemIds.NETHERITE_WIND_BLADE,
            "netherite_wind_blade",
            "item.pepeforge.netherite_wind_blade",
            ItemNameColor.NETHERITE,
            7,
            ItemRarity.EPIC,
            Material.NETHERITE_SWORD,
            Material.NETHERITE_INGOT,
            Material.BREEZE_ROD,
            new NamespacedKey("pepeforge", "netherite_wind_blade"),
            CustomModelDataIds.NETHERITE_WIND_BLADE,
            7.0,
            2.2,
            true,
            1,
            60);

    private static final Map<String, WindBladeTier> BY_ITEM_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(WindBladeTier::itemId, Function.identity()));

    private final String itemId;
    private final String langPath;
    private final String translationKeyBase;
    private final ItemNameColor nameColor;
    private final int loreLineCount;
    private final ItemRarity rarity;
    private final Material baseMaterial;
    private final Material bladeMaterial;
    private final Material handleMaterial;
    private final NamespacedKey modelKey;
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
            ItemNameColor nameColor,
            int loreLineCount,
            ItemRarity rarity,
            Material baseMaterial,
            Material bladeMaterial,
            Material handleMaterial,
            NamespacedKey modelKey,
            int customModelData,
            double attackDamage,
            double attackSpeed,
            boolean grantsHoldingSpeed,
            int hitSpeedAmplifier,
            int hitSpeedDurationTicks) {
        this.itemId = itemId;
        this.langPath = langPath;
        this.translationKeyBase = translationKeyBase;
        this.nameColor = nameColor;
        this.loreLineCount = loreLineCount;
        this.rarity = rarity;
        this.baseMaterial = baseMaterial;
        this.bladeMaterial = bladeMaterial;
        this.handleMaterial = handleMaterial;
        this.modelKey = modelKey;
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

    public int loreLineCount() {
        return loreLineCount;
    }

    public ItemNameColor nameColor() {
        return nameColor;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public Material baseMaterial() {
        return baseMaterial;
    }

    public Material bladeMaterial() {
        return bladeMaterial;
    }

    public Material handleMaterial() {
        return handleMaterial;
    }

    public NamespacedKey modelKey() {
        return modelKey;
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
