package pepin.pepeforge.util.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TwoHandedEquipmentObserverTest {
    @Test
    void startAndFallbackDispatchSilentRefreshesOnlyOnPlayerScheduler() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            fixture.observer.start();

            assertEquals(40L, fixture.fallback.delay);
            assertEquals(40L, fixture.fallback.period);
            assertEquals(1, fixture.queued.size());
            verifyNoInteractions(handler);
            fixture.queued.getFirst().callback.run();
            verify(handler).refreshEquipment(fixture.player, false);

            fixture.fallback.callback.run();
            verifyNoMoreInteractions(handler);
            assertEquals(2, fixture.queued.size());
            fixture.queued.getLast().callback.run();
            verify(handler, times(2)).refreshEquipment(fixture.player, false);
        }
    }

    @Test
    void equipmentEventsCoalesceAndReadInventoryAfterTheTransaction() {
        try (Fixture fixture = new Fixture()) {
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(fixture.player.getInventory()).thenReturn(inventory);
            AtomicReference<ItemStack> observed = new AtomicReference<>();
            fixture.observer.register((player, notify) -> {
                assertTrue(notify);
                observed.set(player.getInventory().getItemInMainHand());
            });
            fixture.observer.start();
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(fixture.player);
            InventoryDragEvent drag = mock(InventoryDragEvent.class);
            when(drag.getWhoClicked()).thenReturn(fixture.player);
            PlayerSwapHandItemsEvent swap = mock(PlayerSwapHandItemsEvent.class);
            when(swap.getPlayer()).thenReturn(fixture.player);

            fixture.observer.onClick(click);
            fixture.observer.onDrag(drag);
            fixture.observer.onSwap(swap);
            fixture.fallback.callback.run();

            assertEquals(1, fixture.queued.size());
            assertEquals(1L, fixture.queued.getFirst().delay);
            verifyNoInteractions(inventory);
            ItemStack replacement = mock(ItemStack.class);
            when(replacement.getType()).thenReturn(Material.IRON_SWORD);
            when(inventory.getItemInMainHand()).thenReturn(replacement);
            fixture.queued.getFirst().callback.run();
            assertSame(replacement, observed.get());
        }
    }

    @Test
    void quitAndRejoinDoNotReuseAQueuedCallbackFromTheOldSession() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            QueuedTask old = fixture.queued.getFirst();
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(fixture.player);
            fixture.observer.onQuit(quit);
            Player replacement = mock(Player.class);
            UUID playerId = fixture.player.getUniqueId();
            when(replacement.getUniqueId()).thenReturn(playerId);
            when(replacement.isOnline()).thenReturn(true);
            fixture.observer.requestRefresh(replacement);

            assertTrue(old.cancelled);
            old.callback.run();
            verifyNoInteractions(handler);
            fixture.queued.getLast().callback.run();
            verify(handler).refreshEquipment(replacement, true);
        }
    }

    @Test
    void cancelledSlotChangeStillReconcilesTheEffectiveEquipmentSilently() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            fixture.queued.getFirst().callback.run();
            clearInvocations(handler);
            PlayerItemHeldEvent event = mock(PlayerItemHeldEvent.class);
            when(event.getPlayer()).thenReturn(fixture.player);
            when(event.isCancelled()).thenReturn(true);

            fixture.observer.onHeldItem(event);
            fixture.queued.getLast().callback.run();

            verify(handler).refreshEquipment(fixture.player, false);
        }
    }

    @Test
    void retirementDropsPendingRefreshAndAllowsANewRequest() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            QueuedTask old = fixture.queued.getFirst();

            old.retired.run();
            old.callback.run();
            fixture.observer.requestRefresh(fixture.player);
            fixture.queued.getLast().callback.run();

            verify(handler).refreshEquipment(fixture.player, true);
            verifyNoMoreInteractions(handler);
        }
    }

    @Test
    void stopInvalidatesPendingAndFallbackCallbacksIncludingAfterRestart() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            QueuedTask old = fixture.queued.getFirst();
            QueuedTask oldFallback = fixture.fallback;
            fixture.observer.stop();

            assertTrue(old.cancelled);
            assertTrue(oldFallback.cancelled);
            fixture.observer.start();
            fixture.observer.register(handler);
            old.callback.run();
            oldFallback.callback.run();
            verifyNoInteractions(handler);
            assertEquals(2, fixture.queued.size());
            fixture.queued.getLast().callback.run();
            verify(handler).refreshEquipment(fixture.player, false);
        }
    }

    @Test
    void unregisterAndOfflinePlayerPreventRefresh() {
        try (Fixture fixture = new Fixture()) {
            TwoHandedEquipmentHandler handler = mock(TwoHandedEquipmentHandler.class);
            fixture.observer.register(handler);
            fixture.observer.start();
            fixture.observer.unregister(handler);
            fixture.queued.getFirst().callback.run();
            fixture.observer.register(handler);
            fixture.observer.requestRefresh(fixture.player);
            when(fixture.player.isOnline()).thenReturn(false);
            fixture.queued.getLast().callback.run();

            verifyNoInteractions(handler);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final Player player = mock(Player.class);
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private final List<QueuedTask> queued = new ArrayList<>();
        private final TwoHandedEquipmentObserver observer = new TwoHandedEquipmentObserver(plugin);
        private QueuedTask fallback;

        private Fixture() {
            Server server = mock(Server.class);
            when(plugin.getServer()).thenReturn(server);
            doReturn(List.of(player)).when(server).getOnlinePlayers();
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            scheduler.when(() -> SchedulerCompat.runTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(call -> {
                        fallback = new QueuedTask(call.getArgument(1), null, call.getArgument(2), call.getArgument(3));
                        return fallback;
                    });
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(
                    any(Player.class), eq(plugin), any(Runnable.class), any(Runnable.class), anyLong()))
                    .thenAnswer(call -> {
                        QueuedTask task = new QueuedTask(call.getArgument(2), call.getArgument(3), call.getArgument(4), 0L);
                        queued.add(task);
                        return task;
                    });
        }

        @Override
        public void close() {
            observer.stop();
            scheduler.close();
        }
    }

    private static final class QueuedTask implements ScheduledTaskCompat {
        private final Runnable callback;
        private final Runnable retired;
        private final long delay;
        private final long period;
        private boolean cancelled;

        private QueuedTask(Runnable callback, Runnable retired, long delay, long period) {
            this.callback = callback;
            this.retired = retired;
            this.delay = delay;
            this.period = period;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
