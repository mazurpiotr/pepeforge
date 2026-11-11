package pepin.pepeforge.util.combat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

public final class TwoHandedEquipmentObserver implements Listener {
    private static final long RECONCILE_INTERVAL_TICKS = 40L;

    private final JavaPlugin plugin;
    private final Object taskLock = new Object();
    private final Set<TwoHandedEquipmentHandler> handlers = new CopyOnWriteArraySet<>();
    private final Map<UUID, Refresh> pending = new ConcurrentHashMap<>();
    private ScheduledTaskCompat compatibilityTask;
    private volatile boolean running;
    private volatile long generation;

    public TwoHandedEquipmentObserver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(TwoHandedEquipmentHandler handler) {
        handlers.add(handler);
    }

    public void unregister(TwoHandedEquipmentHandler handler) {
        handlers.remove(handler);
    }

    public void start() {
        synchronized (taskLock) {
            if (running) {
                return;
            }
            running = true;
            long currentGeneration = ++generation;
            compatibilityTask = SchedulerCompat.runTimer(plugin, () -> {
                if (running && generation == currentGeneration) {
                    refreshOnlinePlayers();
                }
            }, RECONCILE_INTERVAL_TICKS, RECONCILE_INTERVAL_TICKS);
        }
        refreshOnlinePlayers();
    }

    public void stop() {
        synchronized (taskLock) {
            running = false;
            generation++;
            if (compatibilityTask != null) {
                compatibilityTask.cancel();
                compatibilityTask = null;
            }
            pending.values().forEach(Refresh::cancel);
            pending.clear();
            handlers.clear();
        }
    }

    public void requestRefresh(Player player) {
        requestRefresh(player, true);
    }

    private void refreshOnlinePlayers() {
        plugin.getServer().getOnlinePlayers().forEach(player -> requestRefresh(player, false));
    }

    private void requestRefresh(Player player, boolean notify) {
        UUID playerId = player.getUniqueId();
        synchronized (taskLock) {
            if (!running) {
                return;
            }
            Refresh existing = pending.get(playerId);
            if (existing != null && existing.player == player) {
                existing.notify |= notify;
                return;
            }
            if (existing != null) {
                existing.cancel();
            }
            Refresh refresh = new Refresh(player, generation, notify);
            pending.put(playerId, refresh);
            try {
                refresh.task = SchedulerCompat.runLaterForPlayer(
                        player, plugin, refresh,
                        () -> pending.remove(playerId, refresh), 1L);
            } catch (RuntimeException | Error failure) {
                pending.remove(playerId, refresh);
                refresh.cancel();
                throw failure;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        requestRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        requestRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        requestRefresh(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        requestRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldItem(PlayerItemHeldEvent event) {
        requestRefresh(event.getPlayer(), !event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        requestRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            requestRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            requestRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            requestRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            requestRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        requestRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        synchronized (taskLock) {
            Refresh refresh = pending.remove(event.getPlayer().getUniqueId());
            if (refresh != null) {
                refresh.cancel();
            }
        }
    }

    private final class Refresh implements Runnable {
        private final Player player;
        private final UUID playerId;
        private final long expectedGeneration;
        private volatile boolean notify;
        private volatile boolean cancelled;
        private ScheduledTaskCompat task;

        private Refresh(Player player, long expectedGeneration, boolean notify) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.expectedGeneration = expectedGeneration;
            this.notify = notify;
        }

        @Override
        public void run() {
            if (!isCurrent() || !pending.remove(playerId, this) || !player.isOnline()) {
                return;
            }
            for (TwoHandedEquipmentHandler handler : handlers) {
                if (!isCurrent()) {
                    return;
                }
                handler.refreshEquipment(player, notify);
            }
        }

        private boolean isCurrent() {
            return !cancelled && running && generation == expectedGeneration;
        }

        private void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
            }
        }
    }
}
