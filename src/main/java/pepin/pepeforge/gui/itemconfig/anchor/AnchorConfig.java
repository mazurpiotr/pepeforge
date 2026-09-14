package pepin.pepeforge.gui.itemconfig.anchor;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.anchor.AnchorDefinition;

import java.util.List;

public class AnchorConfig {

    private final PepeForgePlugin plugin;

    public AnchorConfig(PepeForgePlugin plugin) {

        this.plugin = plugin;
    }

    public void build(Inventory inventory) {

        // Slot 18: Reset
        ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetBtn.getItemMeta();

        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset to Defaults");
            resetMeta.setLore(List.of(
                    ColorUtil.GRAY + "Reset all settings to default values"));
            resetBtn.setItemMeta(resetMeta);
        }

        inventory.setItem(18, resetBtn);

        // Slot 19: Hook Ability
        ItemStack hookEnabledBtn = new ItemStack(ConfigIconography.TOGGLE);
        ItemMeta hookEnabledMeta = hookEnabledBtn.getItemMeta();

        if (hookEnabledMeta != null) {
            boolean hookEnabled = plugin.getConfig()
                    .getBoolean("mechanics.anchor.hook_enabled", AnchorDefinition.DEFAULT_HOOK_ENABLED);

            hookEnabledMeta.setDisplayName(
                    ColorUtil.GOLD + "Hook Ability: " +
                            (hookEnabled
                                    ? ColorUtil.GREEN + "ENABLED"
                                    : ColorUtil.RED + "DISABLED"));

            hookEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle active ability"));

            hookEnabledBtn.setItemMeta(hookEnabledMeta);
        }

        inventory.setItem(19, hookEnabledBtn);

        // Slot 20: Hook Cooldown
        ItemStack hookCdBtn = new ItemStack(ConfigIconography.COOLDOWN);
        ItemMeta hookCdMeta = hookCdBtn.getItemMeta();

        if (hookCdMeta != null) {
            double hookCd = plugin.getConfig()
                    .getLong("mechanics.anchor.ability_cooldown", AnchorDefinition.DEFAULT_ABILITY_COOLDOWN_MILLIS)
                    / 1000.0;

            hookCdMeta.setDisplayName(
                    ColorUtil.GOLD + "Hook Cooldown: " +
                            ColorUtil.GREEN + hookCd + "s");

            hookCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s",
                    ColorUtil.GRAY + "Right-Click: +0.5s"));

            hookCdBtn.setItemMeta(hookCdMeta);
        }

        inventory.setItem(20, hookCdBtn);

        // Slot 21: Range
        ItemStack rangeBtn = new ItemStack(ConfigIconography.RANGE);
        ItemMeta rangeMeta = rangeBtn.getItemMeta();

        if (rangeMeta != null) {
            double range = plugin.getConfig()
                    .getDouble("mechanics.anchor.ability_range", AnchorDefinition.DEFAULT_ABILITY_RANGE);

            rangeMeta.setDisplayName(
                    ColorUtil.GOLD + "Range: " +
                            ColorUtil.GREEN + range + " blocks");

            rangeMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1.0 block",
                    ColorUtil.GRAY + "Right-Click: +1.0 block"));

            rangeBtn.setItemMeta(rangeMeta);
        }

        inventory.setItem(21, rangeBtn);

        // Slot 22: Separator
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        ItemMeta separatorMeta = separator.getItemMeta();

        if (separatorMeta != null) {
            separatorMeta.setDisplayName(" ");
            separator.setItemMeta(separatorMeta);
        }

        inventory.setItem(22, separator);

        // Slot 23: Snare Passive
        ItemStack snareEnabledBtn = new ItemStack(Material.COBWEB);

        ItemMeta snareEnabledMeta = snareEnabledBtn.getItemMeta();

        if (snareEnabledMeta != null) {
            boolean snareEnabled = plugin.getConfig()
                    .getBoolean("mechanics.anchor.snare_enabled", AnchorDefinition.DEFAULT_SNARE_ENABLED);

            snareEnabledMeta.setDisplayName(
                    ColorUtil.GOLD + "Snare Passive: " +
                            (snareEnabled
                                    ? ColorUtil.GREEN + "ENABLED"
                                    : ColorUtil.RED + "DISABLED"));

            snareEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle basic hit snare"));

            snareEnabledBtn.setItemMeta(snareEnabledMeta);
        }

        inventory.setItem(23, snareEnabledBtn);

        // Slot 24: Snare Duration
        ItemStack snareDurBtn = new ItemStack(ConfigIconography.DURATION);

        ItemMeta snareDurMeta = snareDurBtn.getItemMeta();

        if (snareDurMeta != null) {
            int snareDur = plugin.getConfig()
                    .getInt("mechanics.anchor.snare_duration", AnchorDefinition.DEFAULT_SNARE_DURATION_TICKS);

            double snareDurSec = snareDur / 20.0;

            snareDurMeta.setDisplayName(
                    ColorUtil.GOLD + "Snare Duration: " +
                            ColorUtil.GREEN + snareDurSec +
                            "s (" + snareDur + " ticks)");

            snareDurMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s (-10 ticks)",
                    ColorUtil.GRAY + "Right-Click: +0.5s (+10 ticks)"));

            snareDurBtn.setItemMeta(snareDurMeta);
        }

        inventory.setItem(24, snareDurBtn);

        // Slot 25: Snare Cooldown
        ItemStack snareCdBtn = new ItemStack(ConfigIconography.COOLDOWN);

        ItemMeta snareCdMeta = snareCdBtn.getItemMeta();

        if (snareCdMeta != null) {
            double snareCd = plugin.getConfig()
                    .getLong("mechanics.anchor.snare_cooldown", AnchorDefinition.DEFAULT_SNARE_COOLDOWN_MILLIS)
                    / 1000.0;

            snareCdMeta.setDisplayName(
                    ColorUtil.GOLD + "Snare Cooldown: " +
                            ColorUtil.GREEN + snareCd + "s");

            snareCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1.0s",
                    ColorUtil.GRAY + "Right-Click: +1.0s"));

            snareCdBtn.setItemMeta(snareCdMeta);
        }

        inventory.setItem(25, snareCdBtn);
    }
}