package pepin.pepeforge.util.aura;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuraManager {

    private static final long AURA_INTERVAL_TICKS = 2L;

    private final JavaPlugin plugin;
    private final List<AuraEffect> passiveAuras = new CopyOnWriteArrayList<>();
    private final Map<UUID, Map<Class<? extends TimedAuraEffect>, TimedAuraEffect>> activeAuras = new ConcurrentHashMap<>();
    private ScheduledTaskCompat task;
    private long taskTick;

    public AuraManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerPassiveAura(AuraEffect effect) {
        passiveAuras.add(effect);
    }

    public void startTask() {
        if (task != null) {
            return;
        }

        task = SchedulerCompat.runTimer(plugin, () -> {
            taskTick += AURA_INTERVAL_TICKS;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    for (AuraEffect effect : passiveAuras) {
                        if (taskTick % effect.getTickInterval() == 0L) {
                            effect.tick(player);
                        }
                    }
                });
            }
        }, 1L, AURA_INTERVAL_TICKS);
    }

    public void addOrExtendActiveAura(Player player, TimedAuraEffect effect, int durationTicks) {
        UUID playerId = player.getUniqueId();
        activeAuras.putIfAbsent(playerId, new ConcurrentHashMap<>());
        Map<Class<? extends TimedAuraEffect>, TimedAuraEffect> playerAuras = activeAuras.get(playerId);

        TimedAuraEffect existing = playerAuras.get(effect.getClass());
        if (existing != null && !existing.isExpired()) {
            existing.extendDuration(durationTicks);
        } else {
            effect.extendDuration(durationTicks);
            playerAuras.put(effect.getClass(), effect);

            class AuraRunner implements Runnable {
                ScheduledTaskCompat taskRef;
                @Override
                public void run() {
                    if (!player.isOnline() || effect.isExpired()) {
                        if (taskRef != null) {
                            taskRef.cancel();
                        }
                        playerAuras.remove(effect.getClass());
                        return;
                    }
                    effect.tick(player);
                }
            }

            AuraRunner runner = new AuraRunner();
            ScheduledTaskCompat scheduledTask = SchedulerCompat.runTimerForEntity(player, plugin, runner, 1L, effect.getTickInterval());
            runner.taskRef = scheduledTask;
        }
    }

    public void clearPlayer(Player player) {
        activeAuras.remove(player.getUniqueId());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        activeAuras.clear();
        passiveAuras.clear();
        taskTick = 0L;
    }
}
