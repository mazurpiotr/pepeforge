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
import pepin.pepeforge.weapons.windblade.WindBladeTier;

import java.util.Map;

public final class SmithingUpgradeListener implements Listener {

    private final ItemFactory itemFactory;
    private final NamespacedKey itemIdKey;

    public SmithingUpgradeListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
        // UWAGA: Sztywno ustawiamy "pepeforge", aby idealnie pasowało do Twojego NBT!
        this.itemIdKey = new NamespacedKey("pepeforge", "item_id"); 
    }

    // 1. EVENT: Podgląd w kowadle (gdy gracz wkłada przedmioty)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = calculateCustomSmithingResult(event.getInventory());
        if (result != null) {
            event.setResult(result);
        }
    }

    // 2. EVENT: Faktyczne wyciągnięcie przedmiotu (zabezpieczenie przed nadpisaniem przez Vanillę)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        if (event.getInventory() instanceof SmithingInventory inventory) {
            ItemStack result = calculateCustomSmithingResult(inventory);
            
            if (result != null) {
                // Podmieniamy przedmiot, który gracz ma na kursorze
                event.setCurrentItem(result); 
            }
        }
    }

    // --- Główna logika sprawdzająca ---

    private ItemStack calculateCustomSmithingResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(0);
        ItemStack baseItem = inventory.getItem(1);
        ItemStack addition = inventory.getItem(2);

        if (baseItem == null || baseItem.getType().isAir()) return null;
        if (template == null || template.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) return null;
        if (addition == null || addition.getType() != Material.NETHERITE_INGOT) return null;

        String customId = getCustomItemId(baseItem);
        if (customId == null) return null; // To nie jest nasz customowy przedmiot

        ItemStack properResult = null;

        // Dopasowanie do ID
        switch (customId) {
            case "diamond_greatsword":
                properResult = itemFactory.createGreatsword(GreatswordTier.NETHERITE);
                break;
            case "diamond_wind_blade":
                properResult = itemFactory.createWindBlade(WindBladeTier.NETHERITE);
                break;
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

        // Kopiowanie zaklinowań (flaga 'true' pozwala ominąć waniliowe restrykcje przy kopiowaniu)
        if (sourceMeta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : sourceMeta.getEnchants().entrySet()) {
                targetMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        // Kopiowanie uszkodzeń
        if (sourceMeta instanceof Damageable sourceDamage && targetMeta instanceof Damageable targetDamage) {
            targetDamage.setDamage(sourceDamage.getDamage());
        }

        target.setItemMeta(targetMeta);
    }
}