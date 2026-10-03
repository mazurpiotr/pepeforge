package pepin.pepeforge.util.aura;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuraManager {

    private static final long AURA_INTERVAL_TICKS = 2L;

    private final JavaPlugin plugin;
    private final Object lifecycleLock = new Object();
    private final List<AuraEffect> passiveAuras = new CopyOnWriteArrayList<>();
    private final Map<UUID, Map<Class<? extends TimedAuraEffect>, ActiveAura>> activeAuras =
            new ConcurrentHashMap<>();

    private ScheduledTaskCompat task;
    private volatile long lifecycleGeneration;
    private volatile boolean started;
    private long taskTick;

    public AuraManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerPassiveAura(AuraEffect effect) {
        passiveAuras.add(effect);
    }

    public void startTask() {
        synchronized (lifecycleLock) {
            if (task != null) {
                return;
            }

            long generation = ++lifecycleGeneration;
            ScheduledTaskCompat scheduledTask = SchedulerCompat.runTimer(
                    plugin,
                    () -> tickPassiveAuras(generation),
                    1L,
                    AURA_INTERVAL_TICKS);
            task = scheduledTask;
            started = true;
        }
    }

    private void tickPassiveAuras(long generation) {
        long currentTick;
        synchronized (lifecycleLock) {
            if (!started || generation != lifecycleGeneration) {
                return;
            }
            taskTick += AURA_INTERVAL_TICKS;
            currentTick = taskTick;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (generation != lifecycleGeneration) {
                return;
            }

            SchedulerCompat.runForPlayer(player, plugin, () -> {
                if (generation != lifecycleGeneration || !player.isOnline()) {
                    return;
                }
                for (AuraEffect effect : passiveAuras) {
                    if (generation != lifecycleGeneration) {
                        return;
                    }
                    if (currentTick % effect.getTickInterval() == 0L) {
                        effect.tick(player);
                    }
                }
            });
        }
    }

    public void addOrExtendActiveAura(Player player, TimedAuraEffect effect, int durationTicks) {
        UUID playerId = player.getUniqueId();
        Class<? extends TimedAuraEffect> effectType = effect.getClass();

        synchronized (lifecycleLock) {
            if (!started) {
                return;
            }

            Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras =
                    activeAuras.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
            ActiveAura existing = playerAuras.get(effectType);
            if (existing != null && !existing.effect.isExpired()) {
                existing.effect.extendDuration(durationTicks);
                return;
            }
            if (existing != null) {
                playerAuras.remove(effectType, existing);
                existing.cancel();
            }

            ActiveAura activeAura = new ActiveAura(effect, lifecycleGeneration);
            playerAuras.put(effectType, activeAura);
            try {
                ScheduledTaskCompat scheduledTask = SchedulerCompat.runTimerForEntity(
                        player,
                        plugin,
                        new ActiveAuraRunner(player, playerId, effectType, activeAura),
                        1L,
                        effect.getTickInterval());
                activeAura.setTask(scheduledTask);
            } catch (RuntimeException | Error failure) {
                playerAuras.remove(effectType, activeAura);
                if (playerAuras.isEmpty()) {
                    activeAuras.remove(playerId, playerAuras);
                }
                activeAura.cancel();
                throw failure;
            }
        }
    }

    public void clearPlayer(Player player) {
        Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras;
        synchronized (lifecycleLock) {
            playerAuras = activeAuras.remove(player.getUniqueId());
        }
        cancelAuras(playerAuras);
    }

    public void stop() {
        List<ActiveAura> aurasToCancel = new ArrayList<>();
        synchronized (lifecycleLock) {
            lifecycleGeneration++;
            started = false;
            if (task != null) {
                task.cancel();
                task = null;
            }
            for (Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras : activeAuras.values()) {
                aurasToCancel.addAll(playerAuras.values());
            }
            activeAuras.clear();
            passiveAuras.clear();
            taskTick = 0L;
        }
        aurasToCancel.forEach(ActiveAura::cancel);
    }

    private void cancelAuras(Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras) {
        if (playerAuras != null) {
            playerAuras.values().forEach(ActiveAura::cancel);
        }
    }

    private final class ActiveAuraRunner implements Runnable {

        private final Player player;
        private final UUID playerId;
        private final Class<? extends TimedAuraEffect> effectType;
        private final ActiveAura activeAura;

        private ActiveAuraRunner(
                Player player,
                UUID playerId,
                Class<? extends TimedAuraEffect> effectType,
                ActiveAura activeAura) {
            this.player = player;
            this.playerId = playerId;
            this.effectType = effectType;
            this.activeAura = activeAura;
        }

        @Override
        public void run() {
            if (!isCurrentAura(playerId, effectType, activeAura)) {
                activeAura.cancel();
                return;
            }
            if (!player.isOnline() || activeAura.effect.isExpired()) {
                removeAura(playerId, effectType, activeAura);
                return;
            }
            activeAura.effect.tick(player);
        }
    }

    private boolean isCurrentAura(
            UUID playerId,
            Class<? extends TimedAuraEffect> effectType,
            ActiveAura activeAura) {
        if (!started || activeAura.generation != lifecycleGeneration) {
            return false;
        }
        Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras = activeAuras.get(playerId);
        return playerAuras != null && playerAuras.get(effectType) == activeAura;
    }

    private void removeAura(
            UUID playerId,
            Class<? extends TimedAuraEffect> effectType,
            ActiveAura activeAura) {
        synchronized (lifecycleLock) {
            Map<Class<? extends TimedAuraEffect>, ActiveAura> playerAuras = activeAuras.get(playerId);
            if (playerAuras != null && playerAuras.remove(effectType, activeAura) && playerAuras.isEmpty()) {
                activeAuras.remove(playerId, playerAuras);
            }
        }
        activeAura.cancel();
    }

    private static final class ActiveAura {

        private final TimedAuraEffect effect;
        private final long generation;
        private ScheduledTaskCompat task;
        private boolean cancelled;

        private ActiveAura(TimedAuraEffect effect, long generation) {
            this.effect = effect;
            this.generation = generation;
        }

        private synchronized void setTask(ScheduledTaskCompat task) {
            if (cancelled) {
                task.cancel();
            } else {
                this.task = task;
            }
        }

        private synchronized void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }
}
