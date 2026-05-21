package pepin.pepeforge.item;

public enum ItemRarity {
    COMMON("WHITE"),
    RARE("BLUE"),
    EPIC("DARK_PURPLE");

    private final String colorName;

    ItemRarity(String colorName) {
        this.colorName = colorName;
    }

    public String colorName() {
        return colorName;
    }
}
