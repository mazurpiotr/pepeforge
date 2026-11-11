package pepin.pepeforge.util.combat;

import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.BossBarManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TemporaryShieldManager implements Listener {

    private final JavaPlugin plugin;
    private final PluginLang lang;
    private final BossBarManager bars;
    private final String barPrefix = "temporary_shield_" + UUID.randomUUID() + "_";
    private final Map<UUID, PlayerShields> players = new ConcurrentHashMap<>();
    private final Map<EntityDamageEvent, Reservation> reservations = new ConcurrentHashMap<>();
    private volatile boolean stopped;

    public TemporaryShieldManager(JavaPlugin plugin, PluginLang lang, BossBarManager bars) {
        this.plugin = plugin;
        this.lang = lang;
        this.bars = bars;
    }

    public void grant(Player player, String sourceId, double amountHp, double limitHp, int durationTicks) {
        if (stopped || sourceId == null || sourceId.isBlank() || !Double.isFinite(amountHp) || amountHp <= 0.0D
                || !Double.isFinite(limitHp) || limitHp <= 0.0D || durationTicks <= 0
                || !SchedulerCompat.isOwnedByCurrentRegion(player) || !player.isOnline() || player.isDead()) {
            return;
        }
        PlayerShields state = players.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerShields(player));
        synchronized (state) {
            if (stopped || players.get(player.getUniqueId()) != state) {
                discard(state);
                return;
            }
            expire(state);
            Pool pool = state.pools.computeIfAbsent(sourceId, ignored -> new Pool(sourceId));
            pool.amount = Math.min(limitHp, pool.amount + amountHp);
            pool.limit = limitHp;
            pool.expiresAt = state.tick + durationTicks;
            updateBar(state, pool);
            if (state.task == null) {
                ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin,
                        () -> tick(state), () -> retire(state), 1L, 1L);
                if (stopped || players.get(player.getUniqueId()) != state) {
                    task.cancel();
                } else {
                    state.task = task;
                }
            }
        }
    }

    public void clearSource(Player player, String sourceId) {
        PlayerShields state = players.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        synchronized (state) {
            Pool removed = state.pools.remove(sourceId);
            if (removed != null) {
                removeBar(state, removed);
            }
            discardIfEmpty(state);
        }
    }

    public void clearSource(String sourceId) {
        for (PlayerShields state : players.values()) {
            clearSource(state.player, sourceId);
        }
    }

    public void stop() {
        stopped = true;
        for (PlayerShields state : players.values()) {
            synchronized (state) {
                discard(state);
            }
        }
        reservations.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protect(EntityDamageEvent event) {
        if (stopped || event.isCancelled() || !(event.getEntity() instanceof Player player)
                || !SchedulerCompat.isOwnedByCurrentRegion(player) || excluded(event.getCause())) {
            return;
        }
        PlayerShields state = players.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        synchronized (state) {
            if (stopped || players.get(player.getUniqueId()) != state || reservations.containsKey(event)) {
                return;
            }
            expire(state);
            double remaining = ShieldDamageReduction.damageBeforeAbsorption(event);
            List<Allocation> allocations = new ArrayList<>();
            for (Pool pool : state.pools.values().stream().sorted(Comparator
                    .comparingLong((Pool pool) -> pool.expiresAt).thenComparing(pool -> pool.sourceId)).toList()) {
                double spent = Math.min(remaining, Math.max(0.0D, pool.amount - pool.reserved));
                if (spent > 0.0D) {
                    allocations.add(new Allocation(pool, spent));
                    remaining -= spent;
                }
                if (remaining <= 0.0D) {
                    break;
                }
            }
            double spent = allocations.stream().mapToDouble(Allocation::amount).sum();
            if (ShieldDamageReduction.reduce(event, spent) > 0.0D) {
                allocations.forEach(allocation -> allocation.pool.reserved += allocation.amount);
                reservations.put(event, new Reservation(state, allocations));
            }
            discardIfEmpty(state);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finishDamage(EntityDamageEvent event) {
        Reservation reservation = reservations.remove(event);
        if (reservation == null) {
            return;
        }
        PlayerShields state = reservation.state;
        synchronized (state) {
            if (stopped || players.get(state.player.getUniqueId()) != state) {
                return;
            }
            expire(state);
            for (Allocation allocation : reservation.allocations) {
                Pool pool = allocation.pool;
                if (state.pools.get(pool.sourceId) != pool) {
                    continue;
                }
                pool.reserved = Math.max(0.0D, pool.reserved - allocation.amount);
                if (!event.isCancelled()) {
                    pool.amount = Math.max(0.0D, pool.amount - allocation.amount);
                }
                if (pool.amount <= 0.0D) {
                    state.pools.remove(pool.sourceId, pool);
                    removeBar(state, pool);
                } else if (SchedulerCompat.isOwnedByCurrentRegion(state.player)) {
                    updateBar(state, pool);
                }
            }
            discardIfEmpty(state);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clearPlayer(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clearPlayer(event.getPlayer());
    }

    private boolean excluded(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.SUICIDE;
    }

    private void tick(PlayerShields state) {
        if (!SchedulerCompat.isOwnedByCurrentRegion(state.player)) {
            retire(state);
            return;
        }
        synchronized (state) {
            if (stopped || players.get(state.player.getUniqueId()) != state
                    || !state.player.isOnline() || state.player.isDead()) {
                discard(state);
                return;
            }
            state.tick++;
            expire(state);
            if (state.tick % 10L == 0L) {
                state.pools.values().forEach(pool -> updateBar(state, pool));
            }
            discardIfEmpty(state);
        }
    }

    private void expire(PlayerShields state) {
        state.pools.values().removeIf(pool -> {
            if (pool.expiresAt <= state.tick) {
                removeBar(state, pool);
                return true;
            }
            return false;
        });
    }

    private void clearPlayer(Player player) {
        PlayerShields state = players.get(player.getUniqueId());
        if (state != null) {
            synchronized (state) {
                discard(state);
            }
        }
    }

    private void retire(PlayerShields state) {
        synchronized (state) {
            discard(state);
        }
    }

    private void discardIfEmpty(PlayerShields state) {
        if (state.pools.isEmpty()) {
            discard(state);
        }
    }

    private void discard(PlayerShields state) {
        players.remove(state.player.getUniqueId(), state);
        if (state.task != null) {
            state.task.cancel();
            state.task = null;
        }
        state.pools.values().forEach(pool -> removeBar(state, pool));
        state.pools.clear();
        reservations.entrySet().removeIf(entry -> entry.getValue().state == state);
    }

    private void updateBar(PlayerShields state, Pool pool) {
        String title = lang.text("messages.temporary_shield.bar")
                .replace("{name}", lang.text("messages.temporary_shield.sources." + pool.sourceId))
                .replace("{current}", number(pool.amount))
                .replace("{limit}", number(pool.limit))
                .replace("{seconds}", String.format(Locale.ROOT, "%.1f", (pool.expiresAt - state.tick) / 20.0D));
        bars.updateBar(state.player, barId(state, pool), title, pool.amount / pool.limit, BarColor.BLUE);
    }

    private String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String barId(PlayerShields state, Pool pool) {
        return barPrefix + state.id + "_" + pool.id;
    }

    private void removeBar(PlayerShields state, Pool pool) {
        String id = barId(state, pool);
        if (plugin.isEnabled() && !SchedulerCompat.isServerStopping()
                && SchedulerCompat.isOwnedByCurrentRegion(state.player)) {
            bars.removeBar(state.player, id);
        } else {
            bars.removeBar(state.playerId, id);
        }
    }

    private static final class PlayerShields {
        private final Player player;
        private final UUID playerId;
        private final UUID id = UUID.randomUUID();
        private final Map<String, Pool> pools = new HashMap<>();
        private ScheduledTaskCompat task;
        private long tick;

        private PlayerShields(Player player) {
            this.player = player;
            this.playerId = player.getUniqueId();
        }
    }

    private static final class Pool {
        private final UUID id = UUID.randomUUID();
        private final String sourceId;
        private double amount;
        private double reserved;
        private double limit;
        private long expiresAt;

        private Pool(String sourceId) {
            this.sourceId = sourceId;
        }
    }

    private record Allocation(Pool pool, double amount) {
    }

    private record Reservation(PlayerShields state, List<Allocation> allocations) {
    }
}
