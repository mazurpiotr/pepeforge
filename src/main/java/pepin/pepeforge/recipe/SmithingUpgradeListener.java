package pepin.pepeforge.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.weapons.greatsword.GreatswordTier;
import pepin.pepeforge.tools.scythe.ScytheTier;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

import java.util.Map;

public final class SmithingUpgradeListener implements Listener {

    private final ItemFactory itemFactory;
    private final NamespacedKey itemIdKey;

    public SmithingUpgradeListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
        // Hardcoded key for storing custom item ID in PersistentDataContainer
        this.itemIdKey = new NamespacedKey("pepeforge", "item_id"); 
    }

    // 1. EVENT: Show the custom result in the smithing table UI
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = calculateCustomSmithingResult(event.getInventory());
        if (result != null) {
            event.setResult(result);
        }
    }

    // 2. EVENT: Replace the item being taken with our custom result
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        if (event.getInventory() instanceof SmithingInventory inventory) {
            ItemStack result = calculateCustomSmithingResult(inventory);
            
            if (result != null) {
                // Replace the item being taken with our custom result
                event.setCurrentItem(result); 
            }
        }
    }

    // --- Main logic ---

    private ItemStack calculateCustomSmithingResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(0);
        ItemStack baseItem = inventory.getItem(1);
        ItemStack addition = inventory.getItem(2);

        if (baseItem == null || baseItem.getType().isAir()) return null;
        if (template == null || template.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return null;
        if (addition == null || addition.getType() != Material.NETHERITE_INGOT) return null;

        String customId = getCustomItemId(baseItem);
        if (customId == null) return null; // Not our custom item, let vanilla handle it

        ItemStack properResult = null;

        // Match based on custom ID to determine the correct upgrade result
        switch (customId) {
            case "diamond_greatsword":
                properResult = itemFactory.createGreatsword(GreatswordTier.NETHERITE);
                break;
            case "diamond_wind_blade":
                properResult = itemFactory.createWindBlade(WindBladeTier.NETHERITE);
                break;
            case "diamond_scythe":
                properResult = itemFactory.createScythe(ScytheTier.NETHERITE);
                break;
            case "crescent_spear":
                // Explicitly block upgrade to Netherite Spear to prevent losing model/identity
                return new ItemStack(Material.AIR);
            default:
                return null;
        }

        if (properResult != null) {
            applyEnchantmentsAndDamage(baseItem, properResult);
        }

        return properResult;
    }

    private String getCustomItemId(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        }
        return null;
    }

    private static void applyEnchantmentsAndDamage(ItemStack source, ItemStack target) {
        if (source == null || target == null || !source.hasItemMeta() || !target.hasItemMeta()) return;

        ItemMeta sourceMeta = source.getItemMeta();
        ItemMeta targetMeta = target.getItemMeta();

        // Copy enchantments, flag true to allow unsafe enchantments (in case of custom items)
        if (sourceMeta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : sourceMeta.getEnchants().entrySet()) {
                targetMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        // Copy damage
        if (sourceMeta instanceof Damageable sourceDamage && targetMeta instanceof Damageable targetDamage) {
            targetDamage.setDamage(sourceDamage.getDamage());
        }

        target.setItemMeta(targetMeta);
    }
}