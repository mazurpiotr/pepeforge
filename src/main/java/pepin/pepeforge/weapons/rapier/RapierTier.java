package pepin.pepeforge.weapons.rapier;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;
import pepin.pepeforge.item.CustomModelDataIds;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.item.ItemNameColor;
import pepin.pepeforge.item.ItemRarity;

public enum RapierTier {
    IRON(ItemIds.IRON_RAPIER, Material.IRON_SWORD, Material.IRON_INGOT,
            ItemNameColor.IRON, ItemRarity.COMMON, CustomModelDataIds.IRON_RAPIER),
    DIAMOND(ItemIds.DIAMOND_RAPIER, Material.DIAMOND_SWORD, Material.DIAMOND,
            ItemNameColor.DIAMOND, ItemRarity.RARE, CustomModelDataIds.DIAMOND_RAPIER),
    NETHERITE(ItemIds.NETHERITE_RAPIER, Material.NETHERITE_SWORD, Material.NETHERITE_INGOT,
            ItemNameColor.NETHERITE, ItemRarity.EPIC, CustomModelDataIds.NETHERITE_RAPIER),
    ICE(ItemIds.ICE_RAPIER, Material.IRON_SWORD, Material.BLUE_ICE,
            ItemNameColor.DIAMOND, ItemRarity.RARE, CustomModelDataIds.ICE_RAPIER);

    private final @NonNull String itemId;
    private final Material baseMaterial;
    private final Material bladeMaterial;
    private final ItemNameColor nameColor;
    private final ItemRarity rarity;
    private final int customModelData;
    private final NamespacedKey modelKey;

    RapierTier(@NonNull String itemId, Material baseMaterial, Material bladeMaterial,
            ItemNameColor nameColor, ItemRarity rarity, int customModelData) {
        this.itemId = itemId;
        this.baseMaterial = baseMaterial;
        this.bladeMaterial = bladeMaterial;
        this.nameColor = nameColor;
        this.rarity = rarity;
        this.customModelData = customModelData;
        this.modelKey = new NamespacedKey("pepeforge", itemId);
    }

    public static RapierTier fromItemId(String itemId) {
        for (RapierTier tier : values()) {
            if (tier.itemId.equals(itemId)) {
                return tier;
            }
        }
        return null;
    }

    public @NonNull String itemId() {
        return itemId;
    }

    public Material baseMaterial() {
        return baseMaterial;
    }

    public Material bladeMaterial() {
        return bladeMaterial;
    }

    public String langPath() {
        return itemId;
    }

    public String translationKeyBase() {
        return "item.pepeforge." + itemId;
    }

    public ItemNameColor nameColor() {
        return nameColor;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public int customModelData() {
        return customModelData;
    }

    public NamespacedKey modelKey() {
        return modelKey;
    }

    public int loreLineCount() {
        return 7;
    }
}
