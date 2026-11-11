package pepin.pepeforge.gui;

import java.util.List;
import java.util.Objects;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jspecify.annotations.NonNull;
import pepin.pepeforge.util.ColorUtil;

public final class ConfigIconography {

    public static final @NonNull Material RESET = Material.REDSTONE_BLOCK;
    public static final @NonNull Material TOGGLE = Material.LEVER;
    public static final @NonNull Material COOLDOWN = Material.CLOCK;
    public static final @NonNull Material STRENGTH = Material.FEATHER;
    public static final @NonNull Material RANGE = Material.SPYGLASS;
    public static final @NonNull Material DURATION = Material.REPEATER;
    public static final @NonNull Material CHARGES = Material.LIGHTNING_ROD;
    public static final @NonNull Material DECAY = Material.CLOCK;

    private ConfigIconography() {
    }

    public static @NonNull ItemStack createStateButton(boolean enabled) {
        DyeColor color = enabled ? DyeColor.GREEN : DyeColor.RED;
        ItemStack button = new ItemStack(Material.BLACK_BANNER);
        BannerMeta meta = (BannerMeta) Objects.requireNonNull(button.getItemMeta());
        meta.setPatterns(List.of(
                new Pattern(color, PatternType.HALF_HORIZONTAL),
                new Pattern(color, PatternType.GRADIENT),
                new Pattern(DyeColor.BLACK, PatternType.BORDER)));
        meta.setDisplayName(enabled ? ColorUtil.GREEN + "Enabled" : ColorUtil.RED + "Disabled");
        button.setItemMeta(meta);
        return button;
    }

    public static @NonNull ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.WHITE_BANNER);
        BannerMeta meta = (BannerMeta) Objects.requireNonNull(button.getItemMeta());
        meta.setPatterns(List.of(
                new Pattern(DyeColor.RED, PatternType.RHOMBUS),
                new Pattern(DyeColor.WHITE, PatternType.SQUARE_TOP_RIGHT),
                new Pattern(DyeColor.WHITE, PatternType.SQUARE_BOTTOM_RIGHT),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT)));
        meta.setDisplayName(ColorUtil.YELLOW + "Back");
        button.setItemMeta(meta);
        return button;
    }

    public static @NonNull ItemStack createNextButton() {
        ItemStack button = new ItemStack(Material.BLUE_BANNER);
        BannerMeta meta = (BannerMeta) Objects.requireNonNull(button.getItemMeta());
        meta.setPatterns(List.of(
                new Pattern(DyeColor.WHITE, PatternType.RHOMBUS),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.BLUE, PatternType.SQUARE_TOP_LEFT),
                new Pattern(DyeColor.BLUE, PatternType.SQUARE_BOTTOM_LEFT),
                new Pattern(DyeColor.BLUE, PatternType.BORDER)));
        meta.setDisplayName(ColorUtil.YELLOW + "Next");
        button.setItemMeta(meta);
        return button;
    }
}
