package pepin.pepeforge.util;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public final class ProtectionUtil implements Listener {

    private static final ThreadLocal<UUID> checkingPlayer = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> pvpCheckCancelled = new ThreadLocal<>();
    private static Plugin pluginInstance;

    private ProtectionUtil() {
    }

    /**
     * Initializes the protection utility and registers its listener.
     *
     * @param plugin the plugin instance
     */
    public static void initialize(Plugin plugin) {
        pluginInstance = plugin;
        plugin.getServer().getPluginManager().registerEvents(new ProtectionUtil(), plugin);
    }

    /**
     * Checks if a player is allowed to damage/affect an entity according to server protections
     * (e.g. WorldGuard, GriefPrevention, Towny, Lands, etc.) without using deprecated constructors.
     *
     * @param player the player performing the action
     * @param target the target entity
     * @return true if the action is allowed, false if it is blocked/cancelled
     */
    public static boolean canDamage(Player player, Entity target) {
        if (pluginInstance == null || player == null || target == null) {
            return true;
        }

        if (!(target instanceof org.bukkit.entity.Damageable damageable)) {
            return true;
        }

        UUID playerId = player.getUniqueId();
        checkingPlayer.set(playerId);
        pvpCheckCancelled.remove();

        try {
            // Triggers the standard Bukkit damage pipeline, which fires EntityDamageByEntityEvent internally.
            // Since this runs synchronously, our monitor listener will capture it on the same thread.
            damageable.damage(0.0D, player);
        } catch (Exception e) {
            // Fallback to true if damage execution encounters any errors (e.g. invalid target state)
            return true;
        } finally {
            checkingPlayer.remove();
        }

        Boolean cancelled = pvpCheckCancelled.get();
        pvpCheckCancelled.remove();

        return cancelled == null || !cancelled;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            UUID checking = checkingPlayer.get();
            if (checking != null && checking.equals(player.getUniqueId())) {
                pvpCheckCancelled.set(event.isCancelled());
            }
        }
    }
}
