package pepin.pepeforge.weapons.obsidianglaive.compat;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

public final class PaperGlaiveCombatAdapter extends BukkitGlaiveCombatAdapter {
    private final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();

    public PaperGlaiveCombatAdapter(JavaPlugin plugin, ItemFactory factory, NativeGlaiveCombat nativeCombat) {
        super(plugin, factory, nativeCombat, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public synchronized void onPreAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        if (!running || !SchedulerCompat.isOwnedByCurrentRegion(player)) {
            return;
        }
        snapshots.remove(player.getUniqueId());
        if (event.isCancelled() || !event.willAttack()
                || !(event.getAttacked() instanceof LivingEntity primary) || !canCapture(player, primary)) {
            return;
        }
        snapshots.put(player.getUniqueId(), new Snapshot(primary.getUniqueId(), player.getTicksLived(),
                snapshot(player, primary, true)));
    }

    @Override
    public Attack capture(Player player, LivingEntity primary) {
        Snapshot snapshot = snapshots.remove(player.getUniqueId());
        if (!canCapture(player, primary)) {
            return DENIED;
        }
        return snapshot != null && snapshot.tick() == player.getTicksLived()
                && snapshot.target().equals(primary.getUniqueId()) ? snapshot.attack() : DENIED;
    }

    @Override
    public void clear(Player player) {
        super.clear(player);
        snapshots.remove(player.getUniqueId());
    }

    @Override
    public synchronized void stop() {
        super.stop();
        snapshots.clear();
    }

    private record Snapshot(UUID target, int tick, Attack attack) {
    }
}
