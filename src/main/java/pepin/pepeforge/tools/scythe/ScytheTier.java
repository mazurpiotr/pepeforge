package pepin.pepeforge.tools.scythe;

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

public enum ScytheTier {
    IRON(
            ItemIds.IRON_SCYTHE,
            "iron_scythe",
            "item.pepeforge.iron_scythe",
            ItemNameColor.IRON,
            6,
            ItemRarity.COMMON,
            Material.IRON_HOE,
            Material.IRON_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "iron_scythe"),
            CustomModelDataIds.IRON_SCYTHE,
            1),
    DIAMOND(
            ItemIds.DIAMOND_SCYTHE,
            "diamond_scythe",
            "item.pepeforge.diamond_scythe",
            ItemNameColor.DIAMOND,
            6,
            ItemRarity.RARE,
            Material.DIAMOND_HOE,
            Material.DIAMOND,
            Material.STICK,
            new NamespacedKey("pepeforge", "diamond_scythe"),
            CustomModelDataIds.DIAMOND_SCYTHE,
            2),
    NETHERITE(
            ItemIds.NETHERITE_SCYTHE,
            "netherite_scythe",
            "item.pepeforge.netherite_scythe",
            ItemNameColor.NETHERITE,
            6,
            ItemRarity.EPIC,
            Material.NETHERITE_HOE,
            Material.NETHERITE_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "netherite_scythe"),
            CustomModelDataIds.NETHERITE_SCYTHE,
            3);

    private static final @NonNull Map<@NonNull String, @NonNull ScytheTier> BY_ITEM_ID = createItemIdMap();

    private static @NonNull Map<@NonNull String, @NonNull ScytheTier> createItemIdMap() {
        Map<@NonNull String, @NonNull ScytheTier> result = new HashMap<>();

        for (ScytheTier tier : values()) {
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
    private final int radius;

    ScytheTier(
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
            int radius) {
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
        this.radius = radius;
    }

    public @NonNull String itemId() {
        return itemId;
    }

    public static ScytheTier fromItemId(String itemId) {
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

    public int radius() {
        return radius;
    }
}
