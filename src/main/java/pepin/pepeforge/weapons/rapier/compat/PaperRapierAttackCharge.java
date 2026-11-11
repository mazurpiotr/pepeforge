package pepin.pepeforge.weapons.rapier.compat;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperRapierAttackCharge implements RapierAttackCharge {
    private final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();
    private boolean running = true;

    @EventHandler(priority = EventPriority.MONITOR)
    public synchronized void onPreAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!running || !SchedulerCompat.isOwnedByCurrentRegion(player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        snapshots.remove(playerId);
        if (!event.isCancelled() && event.willAttack()) {
            snapshots.put(playerId, new Snapshot(event.getAttacked(), player.getTicksLived(), player.getAttackCooldown()));
        }
    }

    @Override
    public float consume(Player attacker, LivingEntity target) {
        Snapshot snapshot = snapshots.remove(attacker.getUniqueId());
        if (snapshot != null && snapshot.target == target && snapshot.tick == attacker.getTicksLived()) {
            return snapshot.charge;
        }
        return attacker.getAttackCooldown();
    }

    @Override
    public void clear(Player player) {
        snapshots.remove(player.getUniqueId());
    }

    @Override
    public synchronized void stop() {
        running = false;
        snapshots.clear();
    }

    private record Snapshot(Entity target, int tick, float charge) {
    }
}
