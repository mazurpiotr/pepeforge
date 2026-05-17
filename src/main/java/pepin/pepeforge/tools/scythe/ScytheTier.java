package pepin.pepeforge.tools.scythe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import pepin.pepeforge.item.ItemIds;

public enum ScytheTier {
    IRON(
            ItemIds.IRON_SCYTHE,
            "iron_scythe",
            "item.pepeforge.iron_scythe",
            "WHITE",
            6,
            5,
            "WHITE",
            Material.IRON_HOE,
            Material.IRON_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "iron_scythe"),
            1001,
            0
    ),
    DIAMOND(
            ItemIds.DIAMOND_SCYTHE,
            "diamond_scythe",
            "item.pepeforge.diamond_scythe",
            "AQUA",
            6,
            5,
            "BLUE",
            Material.DIAMOND_HOE,
            Material.DIAMOND,
            Material.STICK,
            new NamespacedKey("pepeforge", "diamond_scythe"),
            1002,
            1
    ),
    NETHERITE(
            ItemIds.NETHERITE_SCYTHE,
            "netherite_scythe",
            "item.pepeforge.netherite_scythe",
            "DARK_GRAY",
            6,
            5,
            "DARK_PURPLE",
            Material.NETHERITE_HOE,
            Material.NETHERITE_INGOT,
            Material.STICK,
            new NamespacedKey("pepeforge", "netherite_scythe"),
            1003,
            2
    );

    private final String itemId;
    private final String langPath;
    private final String translationKeyBase;
    private final String nameColorName;
    private final int loreLineCount;
    private final int rarityLoreLineIndex;
    private final String rarityColorName;
    private final Material baseMaterial;
    private final Material bladeMaterial;
    private final Material handleMaterial;
    private final NamespacedKey modelKey;
    private final int customModelData;
    private final int radius;

    ScytheTier(
            String itemId,
            String langPath,
            String translationKeyBase,
            String nameColorName,
            int loreLineCount,
            int rarityLoreLineIndex,
            String rarityColorName,
            Material baseMaterial,
            Material bladeMaterial,
            Material handleMaterial,
            NamespacedKey modelKey,
            int customModelData,
            int radius
    ) {
        this.itemId = itemId;
        this.langPath = langPath;
        this.translationKeyBase = translationKeyBase;
        this.nameColorName = nameColorName;
        this.loreLineCount = loreLineCount;
        this.rarityLoreLineIndex = rarityLoreLineIndex;
        this.rarityColorName = rarityColorName;
        this.baseMaterial = baseMaterial;
        this.bladeMaterial = bladeMaterial;
        this.handleMaterial = handleMaterial;
        this.modelKey = modelKey;
        this.customModelData = customModelData;
        this.radius = radius;
    }

    public String itemId() {
        return itemId;
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
