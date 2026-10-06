package pepin.pepeforge.item;

import pepin.pepeforge.util.ColorUtil;

public enum ItemColorPalette {
    IRON_NAME("#BEBEBE", "&#BEBEBE"),
    DIAMOND_NAME("#33EBCB", "&#33EBCB"),
    CRESCENT_NAME("#667DB4", "&#667DB4"),
    CRIMSON_NAME("DARK_RED", "&4"),
    NETHERITE_NAME("DARK_GRAY", "&8"),

    COMMON_RARITY("#A0AEC0", "&#A0AEC0"),
    RARE_RARITY("#3182CE", "&#3182CE"),
    EPIC_RARITY("#805AD5", "&#805AD5"),
    LEGENDARY_RARITY("#D69E2E", "&#D69E2E");

    private final String colorName;
    private final String legacyPrefix;

    ItemColorPalette(String colorName, String legacyPrefix) {
        this.colorName = colorName;
        this.legacyPrefix = legacyPrefix;
    }

    public String colorName() {
        return colorName;
    }

    public String formatLegacy(String text) {
        return ColorUtil.translate(legacyPrefix + ColorUtil.stripLeadingColorCodes(text));
    }
}
