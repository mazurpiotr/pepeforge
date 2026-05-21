package pepin.pepeforge.item;

public enum ItemNameColor {
    WHITE("WHITE"),
    AQUA("AQUA"),
    BLUE("BLUE"),
    DARK_GRAY("DARK_GRAY");

    private final String colorName;

    ItemNameColor(String colorName) {
        this.colorName = colorName;
    }

    public String colorName() {
        return colorName;
    }
}
