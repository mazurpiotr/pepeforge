package pepin.pepeforge.item;

public enum ItemNameColor {
    IRON(ItemColorPalette.IRON_NAME),
    DIAMOND(ItemColorPalette.DIAMOND_NAME),
    CRESCENT(ItemColorPalette.CRESCENT_NAME),
    NETHERITE(ItemColorPalette.NETHERITE_NAME);

    private final ItemColorPalette color;

    ItemNameColor(ItemColorPalette color) {
        this.color = color;
    }

    public String colorName() {
        return color.colorName();
    }
}
