package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;

public interface AttackRangeAdapter {
    boolean apply(ItemStack item, double minReach, double maxReach,
                  double minCreativeReach, double maxCreativeReach, double hitboxMargin);
}
