package pepin.pepeforge.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.ItemMetaManager;
import pepin.pepeforge.weapons.solarshield.SolarShieldDefinition;

import java.util.HashMap;
import java.util.Map;

public class ItemMigrator {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final Map<String, NamespacedKey> defaultModelKeys = new HashMap<>();
    private boolean active;

    public ItemMigrator(JavaPlugin plugin, ItemFactory itemFactory, boolean initialState) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.active = initialState;

        // Cache default item_models for each known custom item to avoid creating ItemStacks during hot migration
        for (String itemId : itemFactory.knownGiveNames()) {
            ItemStack reference = itemFactory.createByName(itemId);
            if (reference != null && reference.hasItemMeta()) {
                ItemMeta refMeta = reference.getItemMeta();
                if (refMeta.hasItemModel()) {
                    defaultModelKeys.put(itemId, refMeta.getItemModel());
                }
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean migrateItem(ItemStack item) {
        if (!active || item == null || !item.hasItemMeta()) {
            return false;
        }

        String itemId = itemFactory.getItemId(item);
        if (itemId == null) {
            return false;
        }

        ItemMeta oldMeta = item.getItemMeta();
        if (oldMeta.hasItemModel()) {
            return false; // Already modernized
        }

        NamespacedKey defaultModel = defaultModelKeys.get(itemId);
        if (defaultModel != null) {
            ItemMetaManager.setItemModelIfSupported(oldMeta, defaultModel);
            item.setItemMeta(oldMeta);

            // Re-apply any dynamic visuals that might depend on NBT state (e.g. Solar Shield charges)
            if (itemFactory.isSolarShield(item)) {
                NamespacedKey chargesKey = new NamespacedKey(plugin, SolarShieldDefinition.CHARGES_KEY_STRING);
                int charges = oldMeta.getPersistentDataContainer().getOrDefault(chargesKey, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
                itemFactory.updateSolarShieldVisuals(item, charges);
            }
            return true;
        }

        return false;
    }
}
