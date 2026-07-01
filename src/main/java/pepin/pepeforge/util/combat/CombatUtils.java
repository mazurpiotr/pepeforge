package pepin.pepeforge.util.combat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class CombatUtils {

    private CombatUtils() {
    }

    /**
     * Checks if the player's off-hand is empty.
     *
     * @param player the player to check
     * @return true if the off-hand is empty or air, false otherwise
     */
    public static boolean hasEmptyOffHand(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        return offHandItem == null || offHandItem.getType().isAir();
    }

    /**
     * Checks if the target position is in front of the player within the given dot threshold.
     *
     * @param player         the player
     * @param targetPosition the target position to check
     * @param threshold      the minimum dot product threshold
     * @return true if in front, false otherwise
     */
    public static boolean isInFront(Player player, Vector targetPosition, double threshold) {
        Vector facing = player.getEyeLocation().getDirection().normalize();
        Vector toTarget = targetPosition.clone().subtract(player.getEyeLocation().toVector());
        if (toTarget.lengthSquared() < 0.001D) {
            return true;
        }
        return facing.dot(toTarget.normalize()) >= threshold;
    }
}
