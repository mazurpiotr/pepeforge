package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public enum GreatswordTier {
    IRON(
            ItemIds.IRON_GREATSWORD,
            "iron_greatsword",
            "item.pepeforge.iron_greatsword",
            ItemNameColor.IRON,
            7,
            ItemRarity.COMMON,
            Material.IRON_SWORD,
            Material.IRON_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "iron_greatsword"),
            CustomModelDataIds.IRON_GREATSWORD,
            6.0D,
            -0.5D),
    DIAMOND(
            ItemIds.DIAMOND_GREATSWORD,
            "diamond_greatsword",
            "item.pepeforge.diamond_greatsword",
            ItemNameColor.DIAMOND,
            7,
            ItemRarity.RARE,
            Material.DIAMOND_SWORD,
            Material.DIAMOND,
            Material.STICK,
            new NamespacedKey("pepeforge", "diamond_greatsword"),
            CustomModelDataIds.DIAMOND_GREATSWORD,
            7.0D,
            -0.5D),
    NETHERITE(
            ItemIds.NETHERITE_GREATSWORD,
            "netherite_greatsword",
            "item.pepeforge.netherite_greatsword",
            ItemNameColor.NETHERITE,
            7,
            ItemRarity.EPIC,
            Material.NETHERITE_SWORD,
            Material.NETHERITE_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "netherite_greatsword"),
            CustomModelDataIds.NETHERITE_GREATSWORD,
            8.0D,
            -0.5D);

    private static final @NonNull Map<@NonNull String, @NonNull GreatswordTier> BY_ITEM_ID = createItemIdMap();

    private static @NonNull Map<@NonNull String, @NonNull GreatswordTier> createItemIdMap() {
        Map<@NonNull String, @NonNull GreatswordTier> result = new HashMap<>();
        for (GreatswordTier tier : values()) {
            result.put(tier.itemId(), tier);
        }
        return Objects.requireNonNull(Map.copyOf(result));
    }

    private final @NonNull String itemId;
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

    GreatswordTier(
            @NonNull String itemId,
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
            double attackSpeed) {
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
    }

    public static GreatswordTier fromItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        return BY_ITEM_ID.get(itemId);
    }

    public @NonNull String itemId() {
        return itemId;
    }

    public String langPath() {
        return langPath;
    }

    public String translationKeyBase() {
        return translationKeyBase;
    }

    public ItemNameColor nameColor() {
        return nameColor;
    }

    public int loreLineCount() {
        return loreLineCount;
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
}
