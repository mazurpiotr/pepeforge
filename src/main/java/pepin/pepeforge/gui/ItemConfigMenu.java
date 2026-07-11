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
            ItemStack resetBtn = new ItemStack(Material.REDSTONE_BLOCK);
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
            ItemStack hookEnabledBtn = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta hookEnabledMeta = hookEnabledBtn.getItemMeta();
            if (hookEnabledMeta != null) {
                boolean hookEnabled = plugin.getConfig().getBoolean("items.anchor.hook_enabled", true);
                hookEnabledMeta.setDisplayName(ColorUtil.GOLD + "Hook Ability: " + (hookEnabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
                hookEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle active ability"
                ));
                hookEnabledBtn.setItemMeta(hookEnabledMeta);
            }
            inventory.setItem(19, hookEnabledBtn);

            // Slot 20: Hook Cooldown (Clock)
            ItemStack hookCdBtn = new ItemStack(Material.CLOCK);
            ItemMeta hookCdMeta = hookCdBtn.getItemMeta();
            if (hookCdMeta != null) {
                double hookCd = plugin.getConfig().getLong("items.anchor.ability_cooldown", 5000L) / 1000.0;
                hookCdMeta.setDisplayName(ColorUtil.GOLD + "Hook Cooldown: " + ColorUtil.GREEN + hookCd + "s");
                hookCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s",
                    ColorUtil.GRAY + "Right-Click: +0.5s"
                ));
                hookCdBtn.setItemMeta(hookCdMeta);
            }
            inventory.setItem(20, hookCdBtn);

            // Slot 21: Range (Spyglass)
            ItemStack rangeBtn = new ItemStack(Material.SPYGLASS);
            ItemMeta rangeMeta = rangeBtn.getItemMeta();
            if (rangeMeta != null) {
                double range = plugin.getConfig().getDouble("items.anchor.ability_range", 20.0);
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
                boolean snareEnabled = plugin.getConfig().getBoolean("items.anchor.snare_enabled", true);
                snareEnabledMeta.setDisplayName(ColorUtil.GOLD + "Snare Passive: " + (snareEnabled ? ColorUtil.GREEN + "ENABLED" : ColorUtil.RED + "DISABLED"));
                snareEnabledMeta.setLore(List.of(
                    ColorUtil.GRAY + "Click to toggle basic hit snare"
                ));
                snareEnabledBtn.setItemMeta(snareEnabledMeta);
            }
            inventory.setItem(23, snareEnabledBtn);

            // Slot 24: Snare Duration (Repeater)
            ItemStack snareDurBtn = new ItemStack(Material.REPEATER);
            ItemMeta snareDurMeta = snareDurBtn.getItemMeta();
            if (snareDurMeta != null) {
                int snareDur = plugin.getConfig().getInt("items.anchor.snare_duration", 40);
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
            ItemStack snareCdBtn = new ItemStack(Material.CLOCK);
            ItemMeta snareCdMeta = snareCdBtn.getItemMeta();
            if (snareCdMeta != null) {
                double snareCd = plugin.getConfig().getLong("items.anchor.snare_cooldown", 5000L) / 1000.0;
                snareCdMeta.setDisplayName(ColorUtil.GOLD + "Snare Cooldown: " + ColorUtil.GREEN + snareCd + "s");
                snareCdMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1.0s",
                    ColorUtil.GRAY + "Right-Click: +1.0s"
                ));
                snareCdBtn.setItemMeta(snareCdMeta);
            }
            inventory.setItem(25, snareCdBtn);
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
