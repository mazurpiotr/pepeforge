package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

/** Removes native absorption capacity left by the former Moonshield implementation. */
public final class Moonshield implements Listener {
    private static final String KEY_PREFIX = "crescent_khopesh_moonshield_";
    private final JavaPlugin plugin;
    private final String namespace;
    private volatile boolean stopped;

    public Moonshield(JavaPlugin plugin) {
        this.plugin = plugin;
        namespace = new NamespacedKey(plugin, KEY_PREFIX + "migration").getNamespace();
    }

    public void cleanupOnlinePlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::cleanup);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        cleanup(event.getPlayer());
    }

    public void cleanup(Player player) {
        if (stopped) {
            return;
        }
        if (!SchedulerCompat.isOwnedByCurrentRegion(player)) {
            SchedulerCompat.runForPlayer(player, plugin, () -> cleanup(player));
            return;
        }
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_ABSORPTION);
        if (attribute == null) {
            return;
        }
        boolean removed = false;
        for (AttributeModifier modifier : new ArrayList<>(attribute.getModifiers())) {
            NamespacedKey key = modifier.getKey();
            if (namespace.equals(key.getNamespace()) && key.getKey().startsWith(KEY_PREFIX)) {
                attribute.removeModifier(modifier);
                removed = true;
            }
        }
        if (removed) {
            // The shared native pool has no source attribution; preserve all foreign capacity.
            double remainingCapacity = Math.max(0.0D, attribute.getValue());
            if (player.getAbsorptionAmount() > remainingCapacity) {
                player.setAbsorptionAmount(remainingCapacity);
            }
        }
    }

    public void stop() {
        stopped = true;
    }
}
