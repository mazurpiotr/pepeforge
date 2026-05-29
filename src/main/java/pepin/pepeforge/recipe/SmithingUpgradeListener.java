package pepin.pepeforge.recipe;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.ItemMetaCompat;
import pepin.pepeforge.weapons.greatsword.GreatswordRecipeKeys;
import pepin.pepeforge.weapons.greatsword.GreatswordTier;
import pepin.pepeforge.weapons.windblade.WindBladeRecipeKeys;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

public final class SmithingUpgradeListener implements Listener {

    private final ItemFactory itemFactory;

    public SmithingUpgradeListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }

        // Use reflection to obtain recipe key where available (compat across API versions)
        try {
            java.lang.reflect.Method getKey = event.getRecipe().getClass().getMethod("getKey");
            Object key = getKey.invoke(event.getRecipe());
            if (GreatswordRecipeKeys.NETHERITE_GREATSWORD.equals(key)) {
                ItemStack proper = itemFactory.createGreatsword(GreatswordTier.NETHERITE);
                    if (proper.getItemMeta() != null && result.getItemMeta() != null) {
                        Map<Enchantment, Integer> ench = result.getItemMeta().getEnchants();
                        var meta = proper.getItemMeta();
                        for (Map.Entry<Enchantment, Integer> e : ench.entrySet()) {
                            meta.addEnchant(e.getKey(), e.getValue(), true);
                        }
                        result.setItemMeta(meta);
                    }
                return;
            }
            if (WindBladeRecipeKeys.NETHERITE_WIND_BLADE.equals(key)) {
                ItemStack proper = itemFactory.createWindBlade(WindBladeTier.NETHERITE);
                if (proper.getItemMeta() != null && result.getItemMeta() != null) {
                    Map<Enchantment, Integer> ench = result.getItemMeta().getEnchants();
                    var meta = proper.getItemMeta();
                    for (Map.Entry<Enchantment, Integer> e : ench.entrySet()) {
                        meta.addEnchant(e.getKey(), e.getValue(), true);
                    }
                    result.setItemMeta(meta);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // If getKey() is unavailable, fall back to no-op; other compatibility checks could be added later
        }
    }
}
