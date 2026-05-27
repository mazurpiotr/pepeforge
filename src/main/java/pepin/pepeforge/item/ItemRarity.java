package pepin.pepeforge.item;

public enum ItemRarity {
    COMMON(ItemColorPalette.COMMON_RARITY),
    RARE(ItemColorPalette.RARE_RARITY),
    EPIC(ItemColorPalette.EPIC_RARITY),
    LEGENDARY(ItemColorPalette.LEGENDARY_RARITY);

    private final ItemColorPalette color;

    ItemRarity(ItemColorPalette color) {
        this.color = color;
    }

    public String colorName() {
        return color.colorName();
    }
}
