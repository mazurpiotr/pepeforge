package pepin.pepeforge.util.protection;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

public final class ProtectionUtil {

    private static Plugin pluginInstance;

    private ProtectionUtil() {
    }

    /**
     * Initializes the protection utility.
     *
     * @param plugin the plugin instance
     */
    public static void initialize(Plugin plugin) {
        pluginInstance = plugin;
    }

    /**
     * Checks if a player is allowed to damage/affect an entity according to server
     * protections
     * (e.g. WorldGuard, GriefPrevention, Towny, Lands, etc.) without actually
     * causing damage.
     *
     * @param player the player performing the action
     * @param target the target entity
     * @return true if the action is allowed, false if it is blocked/cancelled
     */
    public static boolean canDamage(Player player, Entity target) {
        if (pluginInstance == null || player == null || target == null) {
            return true;
        }

        if (!(target instanceof org.bukkit.entity.Damageable)) {
            return true;
        }

        try {
            try {
                DamageSource source = DamageSource.builder(DamageType.PLAYER_ATTACK)
                        .withDirectEntity(player)
                        .withCausingEntity(player)
                        .build();

                // Try modern constructor first (expects base modifier if maps are passed, so we
                // construct it carefully or let it fail to fallback)
                @SuppressWarnings({ "unchecked", "rawtypes" })
                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                        player,
                        target,
                        DamageCause.ENTITY_ATTACK,
                        source,
                        new java.util.HashMap(),
                        new java.util.HashMap(),
                        false);

                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } catch (Throwable t) {
                // Fallback to classic constructor supported by all silniks/forks (including
                // CraftBukkit/Spigot/Paper)
                @SuppressWarnings("removal")
                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                        player,
                        target,
                        DamageCause.ENTITY_ATTACK,
                        1.0D);
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            }
        } catch (Throwable t2) {
            // Fallback to true if event creation/execution encounters any errors
            pluginInstance.getLogger().warning("Error checking damage protection: " + t2.getMessage());
            return true;
        }
    }
}
