package pepin.pepeforge.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class RecipeDiscoveryRefresherTest {

    @Test
    void stopCancelsQueuedRefreshesAndFencesTheirCallbacks() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        Player player = mock(Player.class);
        when(plugin.getServer()).thenReturn(server);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask firstTask = mock(BukkitTask.class);
        BukkitTask secondTask = mock(BukkitTask.class);
        AtomicInteger discoveries = new AtomicInteger();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong()))
                    .thenReturn(firstTask, secondTask);

            RecipeDiscoveryRefresher refresher = new RecipeDiscoveryRefresher(
                    plugin,
                    ignored -> discoveries.incrementAndGet());
            refresher.refreshAllOnlinePlayers();

            ArgumentCaptor<Runnable> callbacks = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler, times(2)).runTaskLater(any(), callbacks.capture(), anyLong());

            refresher.stop();
            callbacks.getAllValues().forEach(Runnable::run);

            verify(firstTask).cancel();
            verify(secondTask).cancel();
            assertEquals(0, discoveries.get());

            refresher.refreshAllOnlinePlayers();
            verify(scheduler, times(2)).runTaskLater(any(), any(Runnable.class), anyLong());
        }
    }

    @Test
    void inventoryEventBurstRunsOneDiscoveryAfterTheInventoryChanges() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(queued.player.getInventory()).thenReturn(inventory);
            AtomicInteger discoveries = new AtomicInteger();
            RecipeDiscoveryRefresher refresher = queued.refresher(player -> {
                if (player.getInventory().contains(Material.IRON_INGOT)) {
                    discoveries.incrementAndGet();
                }
            });
            EntityPickupItemEvent pickup = mock(EntityPickupItemEvent.class);
            when(pickup.getEntity()).thenReturn(queued.player);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(queued.player);
            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getWhoClicked()).thenReturn(queued.player);
            CraftItemEvent craft = mock(CraftItemEvent.class);
            when(craft.getWhoClicked()).thenReturn(queued.player);
            PlayerDropItemEvent drop = mock(PlayerDropItemEvent.class);
            when(drop.getPlayer()).thenReturn(queued.player);
            PlayerSwapHandItemsEvent swap = mock(PlayerSwapHandItemsEvent.class);
            when(swap.getPlayer()).thenReturn(queued.player);

            refresher.onEntityPickupItem(pickup);
            refresher.onInventoryClick(click);
            refresher.onInventoryDrag(drag);
            refresher.onCraftItem(craft);
            refresher.onPlayerDropItem(drop);
            refresher.onPlayerSwapHandItems(swap);

            verifyNoInteractions(inventory);
            assertEquals(6, queued.tasks.size());
            assertTrue(queued.tasks.subList(0, 5).stream().allMatch(task -> task.cancelled));
            assertTrue(queued.tasks.stream().allMatch(task -> task.delay == 1L));
            when(inventory.contains(Material.IRON_INGOT)).thenReturn(true);
            queued.tasks.forEach(task -> task.callback.run());
            assertEquals(1, discoveries.get());
        }
    }

    @Test
    void joinRefreshesWaitOneAndFortyTicks() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            AtomicInteger discoveries = new AtomicInteger();
            RecipeDiscoveryRefresher refresher = queued.refresher(ignored -> discoveries.incrementAndGet());
            PlayerJoinEvent join = mock(PlayerJoinEvent.class);
            when(join.getPlayer()).thenReturn(queued.player);

            refresher.onPlayerJoin(join);

            assertEquals(0, discoveries.get());
            assertEquals(List.of(1L, 40L), queued.tasks.stream().map(task -> task.delay).toList());
            queued.tasks.forEach(task -> task.callback.run());
            assertEquals(2, discoveries.get());
        }
    }

    @Test
    void refreshingOnlinePlayersUsesTheSameDelayedDiscovery() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            AtomicInteger discoveries = new AtomicInteger();
            RecipeDiscoveryRefresher refresher = queued.refresher(ignored -> discoveries.incrementAndGet());

            refresher.refreshAllOnlinePlayers();

            assertEquals(0, discoveries.get());
            assertEquals(List.of(1L, 40L), queued.tasks.stream().map(task -> task.delay).toList());
            queued.tasks.forEach(task -> task.callback.run());
            assertEquals(2, discoveries.get());
        }
    }

    @Test
    void quitFencesCancelledCallbacksEvenIfThePlayerHasReconnected() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            AtomicInteger discoveries = new AtomicInteger();
            RecipeDiscoveryRefresher refresher = queued.refresher(ignored -> discoveries.incrementAndGet());
            refresher.refreshAllOnlinePlayers();
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(queued.player);

            refresher.onPlayerQuit(quit);

            assertTrue(queued.tasks.stream().allMatch(task -> task.cancelled));
            queued.tasks.forEach(task -> task.callback.run());
            assertEquals(0, discoveries.get());
            refresher.refreshAllOnlinePlayers();
            queued.tasks.subList(2, 4).forEach(task -> task.callback.run());
            assertEquals(2, discoveries.get());
        }
    }

    @Test
    void queuedRefreshSkipsOfflinePlayers() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            AtomicInteger discoveries = new AtomicInteger();
            RecipeDiscoveryRefresher refresher = queued.refresher(ignored -> discoveries.incrementAndGet());
            refresher.refreshAllOnlinePlayers();
            when(queued.player.isOnline()).thenReturn(false);

            queued.tasks.forEach(task -> task.callback.run());

            assertEquals(0, discoveries.get());
        }
    }

    @Test
    void wearAndMendingReadFinalInventoryStateOnOneCoalescedPlayerCallback() {
        try (QueuedRefreshes queued = new QueuedRefreshes()) {
            AtomicInteger actualWear = new AtomicInteger();
            AtomicInteger observedWear = new AtomicInteger(-1);
            RecipeDiscoveryRefresher refresher = queued.refresher(ignored -> observedWear.set(actualWear.get()));
            PlayerItemDamageEvent damage = mock(PlayerItemDamageEvent.class);
            when(damage.getPlayer()).thenReturn(queued.player);
            PlayerItemMendEvent mend = mock(PlayerItemMendEvent.class);
            when(mend.getPlayer()).thenReturn(queued.player);
            refresher.onItemDamage(damage);
            actualWear.set(2);
            refresher.onItemMend(mend);
            actualWear.set(1);
            assertEquals(-1, observedWear.get());
            queued.tasks.forEach(task -> task.callback.run());
            assertEquals(1, observedWear.get());
            assertTrue(queued.tasks.getFirst().cancelled);
        }
    }

    private static final class QueuedRefreshes implements AutoCloseable {
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final Player player = mock(Player.class);
        private final List<QueuedTask> tasks = new ArrayList<>();
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);

        private QueuedRefreshes() {
            Server server = mock(Server.class);
            when(plugin.getServer()).thenReturn(server);
            doReturn(List.of(player)).when(server).getOnlinePlayers();
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(any(), any(), any(Runnable.class), anyLong()))
                    .thenAnswer(invocation -> {
                        QueuedTask task = new QueuedTask(invocation.getArgument(2), invocation.getArgument(3));
                        tasks.add(task);
                        return task;
                    });
        }

        private RecipeDiscoveryRefresher refresher(Consumer<Player> discoverer) {
            return new RecipeDiscoveryRefresher(plugin, discoverer);
        }

        @Override
        public void close() {
            scheduler.close();
        }
    }

    private static final class QueuedTask implements ScheduledTaskCompat {
        private final Runnable callback;
        private final long delay;
        private boolean cancelled;

        private QueuedTask(Runnable callback, long delay) {
            this.callback = callback;
            this.delay = delay;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
