package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.ColorUtil;

import java.util.List;

public final class ItemConfigMenu {

    private ItemConfigMenu() {
    }

    public static Inventory create(String itemId, ItemFactory itemFactory, PepeForgePlugin plugin) {
        Inventory inventory = Bukkit.createInventory(new Holder(itemId), 27, ColorUtil.DARK_GRAY + "Config: " + itemId);
        
        boolean isEnabled = itemFactory.isItemEnabled(itemId);
        boolean isRecipeEnabled = itemFactory.isRecipeEnabled(itemId);

        ItemStack enabledBtn = new ItemStack(isEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta enabledMeta = enabledBtn.getItemMeta();
        if (enabledMeta != null) {
            enabledMeta.setDisplayName(ColorUtil.WHITE + "Item Enabled: " + (isEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
            enabledBtn.setItemMeta(enabledMeta);
        }
        inventory.setItem(11, enabledBtn);

        ItemStack recipeBtn = new ItemStack(isRecipeEnabled ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta recipeMeta = recipeBtn.getItemMeta();
        if (recipeMeta != null) {
            recipeMeta.setDisplayName(ColorUtil.WHITE + "Recipe Enabled: " + (isRecipeEnabled ? ColorUtil.GREEN + "TRUE" : ColorUtil.RED + "FALSE"));
            recipeBtn.setItemMeta(recipeMeta);
        }
        inventory.setItem(15, recipeBtn);

        // Anchor-specific config buttons
        if ("anchor".equals(itemId)) {
            // Slot 18: Reset to Defaults (Redstone Block)
            ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
            ItemMeta resetMeta = resetBtn.getItemMeta();
            if (resetMeta != null) {
                resetMeta.setDisplayName(ColorUtil.RED + "Reset to Defaults");
                resetMeta.setLore(List.of(
                    ColorUtil.GRAY + "Reset all settings to default values"
                ));
                resetBtn.setItemMeta(resetMeta);
            }
            inventory.setItem(18, resetBtn);

            // Slot 19: Hook Ability (Tripwire Hook)
            ItemStack hookEnabledBtn = new ItemStack(ConfigIconography.TOGGLE);
            ItemMeta hookEnabledMeta = hookEnabledBtn.getItemMeta();
            if (hookEnabledMeta != null) {
                boolean hookEnabled = plugin.getConfig().getBoolean("mechanics.anchor.hook_enabled", true);
                hookEnabledMeta.setDisplayName(ColorUtil.GOLD + "Hook Ability: " + (hookEnabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
                hookEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle active ability"
                ));
                hookEnabledBtn.setItemMeta(hookEnabledMeta);
            }
            inventory.setItem(19, hookEnabledBtn);

            // Slot 20: Hook Cooldown (Clock)
            ItemStack hookCdBtn = new ItemStack(ConfigIconography.COOLDOWN);
            ItemMeta hookCdMeta = hookCdBtn.getItemMeta();
            if (hookCdMeta != null) {
                double hookCd = plugin.getConfig().getLong("mechanics.anchor.ability_cooldown", 5000L) / 1000.0;
                hookCdMeta.setDisplayName(ColorUtil.GOLD + "Hook Cooldown: " + ColorUtil.GREEN + hookCd + "s");
                hookCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s",
                    ColorUtil.GRAY + "Right-Click: +0.5s"
                ));
                hookCdBtn.setItemMeta(hookCdMeta);
            }
            inventory.setItem(20, hookCdBtn);

            // Slot 21: Range (Spyglass)
            ItemStack rangeBtn = new ItemStack(ConfigIconography.RANGE);
            ItemMeta rangeMeta = rangeBtn.getItemMeta();
            if (rangeMeta != null) {
                double range = plugin.getConfig().getDouble("mechanics.anchor.ability_range", 20.0);
                rangeMeta.setDisplayName(ColorUtil.GOLD + "Range: " + ColorUtil.GREEN + range + " blocks");
                rangeMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1.0 block",
                    ColorUtil.GRAY + "Right-Click: +1.0 block"
                ));
                rangeBtn.setItemMeta(rangeMeta);
            }
            inventory.setItem(21, rangeBtn);

            // Slot 22: Separator (Gray Stained Glass Pane)
            ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta separatorMeta = separator.getItemMeta();
            if (separatorMeta != null) {
                separatorMeta.setDisplayName(" ");
                separator.setItemMeta(separatorMeta);
            }
            inventory.setItem(22, separator);

            // Slot 23: Snare Passive (Cobweb)
            ItemStack snareEnabledBtn = new ItemStack(Material.COBWEB);
            ItemMeta snareEnabledMeta = snareEnabledBtn.getItemMeta();
            if (snareEnabledMeta != null) {
                boolean snareEnabled = plugin.getConfig().getBoolean("mechanics.anchor.snare_enabled", true);
                snareEnabledMeta.setDisplayName(ColorUtil.GOLD + "Snare Passive: " + (snareEnabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
                snareEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle basic hit snare"
                ));
                snareEnabledBtn.setItemMeta(snareEnabledMeta);
            }
            inventory.setItem(23, snareEnabledBtn);

            // Slot 24: Snare Duration (Repeater)
            ItemStack snareDurBtn = new ItemStack(ConfigIconography.DURATION);
            ItemMeta snareDurMeta = snareDurBtn.getItemMeta();
            if (snareDurMeta != null) {
                int snareDur = plugin.getConfig().getInt("mechanics.anchor.snare_duration", 40);
                double snareDurSec = snareDur / 20.0;
                snareDurMeta.setDisplayName(ColorUtil.GOLD + "Snare Duration: " + ColorUtil.GREEN + snareDurSec + "s (" + snareDur + " ticks)");
                snareDurMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s (-10 ticks)",
                    ColorUtil.GRAY + "Right-Click: +0.5s (+10 ticks)"
                ));
                snareDurBtn.setItemMeta(snareDurMeta);
            }
            inventory.setItem(24, snareDurBtn);

            // Slot 25: Snare Cooldown (Clock)
            ItemStack snareCdBtn = new ItemStack(ConfigIconography.COOLDOWN);
            ItemMeta snareCdMeta = snareCdBtn.getItemMeta();
            if (snareCdMeta != null) {
                double snareCd = plugin.getConfig().getLong("mechanics.anchor.snare_cooldown", 5000L) / 1000.0;
                snareCdMeta.setDisplayName(ColorUtil.GOLD + "Snare Cooldown: " + ColorUtil.GREEN + snareCd + "s");
                snareCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1.0s",
                    ColorUtil.GRAY + "Right-Click: +1.0s"
                ));
                snareCdBtn.setItemMeta(snareCdMeta);
            }
            inventory.setItem(25, snareCdBtn);
        }

        if (isWindBlade(itemId)) {
            ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
            ItemMeta resetMeta = resetBtn.getItemMeta();
            if (resetMeta != null) {
                resetMeta.setDisplayName(ColorUtil.RED + "Reset Wind Blade Defaults");
                resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset shared dash settings"));
                resetBtn.setItemMeta(resetMeta);
            }
            inventory.setItem(18, resetBtn);

            ItemStack cooldownBtn = new ItemStack(ConfigIconography.COOLDOWN);
            ItemMeta cooldownMeta = cooldownBtn.getItemMeta();
            if (cooldownMeta != null) {
                double cooldown = plugin.getConfig().getLong("mechanics.wind_blade.dash_cooldown", 5000L) / 1000.0D;
                cooldownMeta.setDisplayName(ColorUtil.GOLD + "Dash Cooldown: " + ColorUtil.GREEN + cooldown + "s");
                cooldownMeta.setLore(List.of(
                        ColorUtil.GRAY + "Left-Click: -0.5s",
                        ColorUtil.GRAY + "Right-Click: +0.5s"
                ));
                cooldownBtn.setItemMeta(cooldownMeta);
            }
            inventory.setItem(19, cooldownBtn);

            ItemStack strengthBtn = new ItemStack(ConfigIconography.STRENGTH);
            ItemMeta strengthMeta = strengthBtn.getItemMeta();
            if (strengthMeta != null) {
                double strength = plugin.getConfig().getDouble("mechanics.wind_blade.dash_strength", 1.5D);
                strengthMeta.setDisplayName(ColorUtil.GOLD + "Dash Strength: " + ColorUtil.GREEN + strength);
                strengthMeta.setLore(List.of(
                        ColorUtil.GRAY + "Left-Click: -0.1",
                        ColorUtil.GRAY + "Right-Click: +0.1"
                ));
                strengthBtn.setItemMeta(strengthMeta);
            }
            inventory.setItem(20, strengthBtn);

            ItemStack glidingBtn = new ItemStack(ConfigIconography.TOGGLE);
            ItemMeta glidingMeta = glidingBtn.getItemMeta();
            if (glidingMeta != null) {
                boolean enabled = plugin.getConfig().getBoolean("mechanics.wind_blade.dash_while_gliding", false);
                glidingMeta.setDisplayName(ColorUtil.GOLD + "Dash While Gliding: "
                        + (enabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
                glidingMeta.setLore(List.of(ColorUtil.GRAY + "Click to toggle Elytra dashing"));
                glidingBtn.setItemMeta(glidingMeta);
            }
            inventory.setItem(21, glidingBtn);
        }

        if ("stormcleaver".equals(itemId)) {
            ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
            ItemMeta resetMeta = resetBtn.getItemMeta();
            if (resetMeta != null) {
                resetMeta.setDisplayName(ColorUtil.RED + "Reset Stormcleaver Defaults");
                resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset shared dive settings"));
                resetBtn.setItemMeta(resetMeta);
            }
            inventory.setItem(18, resetBtn);

            ItemStack chargesBtn = new ItemStack(ConfigIconography.CHARGES);
            ItemMeta chargesMeta = chargesBtn.getItemMeta();
            if (chargesMeta != null) {
                int charges = plugin.getConfig().getInt("mechanics.stormcleaver.charges_required", 5);
                chargesMeta.setDisplayName(ColorUtil.GOLD + "Required Charges: " + ColorUtil.GREEN + charges);
                chargesMeta.setLore(List.of(
                        ColorUtil.GRAY + "Left-Click: -1",
                        ColorUtil.GRAY + "Right-Click: +1"
                ));
                chargesBtn.setItemMeta(chargesMeta);
            }
            inventory.setItem(19, chargesBtn);

            ItemStack multiplierBtn = new ItemStack(ConfigIconography.STRENGTH);
            ItemMeta multiplierMeta = multiplierBtn.getItemMeta();
            if (multiplierMeta != null) {
                double multiplier = plugin.getConfig().getDouble("mechanics.stormcleaver.jump_velocity_multiplier", 1.5D);
                multiplierMeta.setDisplayName(ColorUtil.GOLD + "Jump Multiplier: " + ColorUtil.GREEN + multiplier);
                multiplierMeta.setLore(List.of(
                        ColorUtil.GRAY + "Left-Click: -0.1",
                        ColorUtil.GRAY + "Right-Click: +0.1"
                ));
                multiplierBtn.setItemMeta(multiplierMeta);
            }
            inventory.setItem(20, multiplierBtn);

            ItemStack decayBtn = new ItemStack(ConfigIconography.DECAY);
            ItemMeta decayMeta = decayBtn.getItemMeta();
            if (decayMeta != null) {
                int decay = plugin.getConfig().getInt("mechanics.stormcleaver.charge_decay_interval", 40);
                decayMeta.setDisplayName(ColorUtil.GOLD + "Charge Decay: " + ColorUtil.GREEN
                        + (decay / 20.0D) + "s (" + decay + " ticks)");
                decayMeta.setLore(List.of(
                        ColorUtil.GRAY + "Left-Click: -0.5s (-10 ticks)",
                        ColorUtil.GRAY + "Right-Click: +0.5s (+10 ticks)"
                ));
                decayBtn.setItemMeta(decayMeta);
            }
            inventory.setItem(21, decayBtn);
        }

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ColorUtil.YELLOW + "Back");
            backBtn.setItemMeta(backMeta);
        }
        inventory.setItem(26, backBtn);

        return inventory;
    }

    public static boolean isItemConfigMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    public static String getItemId(Inventory inventory) {
        if (inventory.getHolder() instanceof Holder holder) {
            return holder.itemId;
        }
        return null;
    }

    public static boolean isWindBlade(String itemId) {
        return "iron_wind_blade".equals(itemId)
                || "diamond_wind_blade".equals(itemId)
                || "netherite_wind_blade".equals(itemId);
    }

    private static final class Holder implements InventoryHolder {
        private final String itemId;

        Holder(String itemId) {
            this.itemId = itemId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
