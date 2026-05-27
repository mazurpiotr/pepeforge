package pepin.pepeforge.item;

public enum ItemColorPalette {
    IRON_NAME("#BEBEBE"),
    DIAMOND_NAME("#33EBCB"),
    CRESCENT_NAME("#667DB4"),
    NETHERITE_NAME("DARK_GRAY"),
    
    COMMON_RARITY("#A0AEC0"),
    RARE_RARITY("#3182CE"),
    EPIC_RARITY("#805AD5"),
    LEGENDARY_RARITY("#D69E2E");

    private final String colorName;

    ItemColorPalette(String colorName) {
        this.colorName = colorName;
    }

    public String colorName() {
        return colorName;
    }
}
