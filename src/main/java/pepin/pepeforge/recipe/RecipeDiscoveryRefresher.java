package pepin.pepeforge.recipe;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class RecipeDiscoveryRefresher implements Listener {

    private final JavaPlugin plugin;
    private final Consumer<Player> discoverer;
    private final Object taskLock = new Object();
    private final Map<UUID, Set<RefreshTask>> tasksByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, RefreshTask> pendingTasks = new ConcurrentHashMap<>();
    private volatile long lifecycleGeneration;
    private volatile boolean stopped;

    public RecipeDiscoveryRefresher(JavaPlugin plugin, Consumer<Player> discoverer) {
        this.plugin = plugin;
        this.discoverer = discoverer;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refreshLater(event.getPlayer(), 1L);
        refreshLater(event.getPlayer(), 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelPlayerTasks(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        refreshSoon(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        refreshSoon(event.getPlayer());
    }

    public void refreshAllOnlinePlayers() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            refreshLater(player, 1L);
            refreshLater(player, 40L);
        });
    }

    private void refreshSoon(Player player) {
        schedule(player, 1L, true);
    }

    private void refreshLater(Player player, long delayTicks) {
        schedule(player, delayTicks, false);
    }

    private void schedule(Player player, long delayTicks, boolean coalesce) {
        UUID playerId = player.getUniqueId();
        synchronized (taskLock) {
            if (stopped) {
                return;
            }

            if (coalesce) {
                RefreshTask previousTask = pendingTasks.remove(playerId);
                if (previousTask != null) {
                    removeTask(previousTask);
                    previousTask.cancel();
                }
            }

            RefreshTask task = new RefreshTask(player, lifecycleGeneration, coalesce);
            task.task = SchedulerCompat.runLaterForPlayer(player, plugin, task, delayTicks);
            tasksByPlayer.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(task);
            if (coalesce) {
                pendingTasks.put(playerId, task);
            }
        }
    }

    private void cancelPlayerTasks(UUID playerId) {
        synchronized (taskLock) {
            Set<RefreshTask> tasks = tasksByPlayer.remove(playerId);
            pendingTasks.remove(playerId);
            if (tasks != null) {
                tasks.forEach(RefreshTask::cancel);
            }
        }
    }

    public void stop() {
        synchronized (taskLock) {
            stopped = true;
            lifecycleGeneration++;
            tasksByPlayer.values().forEach(tasks -> tasks.forEach(RefreshTask::cancel));
            tasksByPlayer.clear();
            pendingTasks.clear();
        }
    }

    private void refresh(Player player) {
        if (player.isOnline()) {
            discoverer.accept(player);
        }
    }

    private final class RefreshTask implements Runnable {

        private final Player player;
        private final UUID playerId;
        private final long generation;
        private final boolean coalesced;
        private ScheduledTaskCompat task;

        private RefreshTask(Player player, long generation, boolean coalesced) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.generation = generation;
            this.coalesced = coalesced;
        }

        @Override
        public void run() {
            try {
                if (!stopped && generation == lifecycleGeneration) {
                    refresh(player);
                }
            } finally {
                synchronized (taskLock) {
                    removeTask(this);
                    if (coalesced) {
                        pendingTasks.remove(playerId, this);
                    }
                }
            }
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
    }

    private void removeTask(RefreshTask task) {
        Set<RefreshTask> tasks = tasksByPlayer.get(task.playerId);
        if (tasks != null && tasks.remove(task) && tasks.isEmpty()) {
            tasksByPlayer.remove(task.playerId, tasks);
        }
    }
}
