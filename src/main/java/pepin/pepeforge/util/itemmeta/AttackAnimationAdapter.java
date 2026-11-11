package pepin.pepeforge.util.itemmeta;

import org.bukkit.inventory.ItemStack;

public interface AttackAnimationAdapter {
    boolean apply(ItemStack item, boolean stab);
}
