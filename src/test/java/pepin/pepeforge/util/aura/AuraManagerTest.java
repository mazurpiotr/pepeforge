package pepin.pepeforge.util.aura;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuraManagerTest {

    @Test
    void stopCancelsGlobalAndActiveAuraTasks() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask globalTask = mock(BukkitTask.class);
        BukkitTask auraTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(globalTask, auraTask);

            AuraManager manager = new AuraManager(plugin);
            manager.startTask();
            AtomicInteger auraTicks = new AtomicInteger();
            manager.addOrExtendActiveAura(player, new TestTimedAuraEffect(auraTicks), 20);

            ArgumentCaptor<Runnable> callbacks = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler, times(2)).runTaskTimer(any(), callbacks.capture(), anyLong(), anyLong());

            manager.stop();
            callbacks.getAllValues().get(1).run();

            verify(globalTask).cancel();
            verify(auraTask).cancel();
            assertEquals(0, auraTicks.get());
        }
    }

    @Test
    void clearPlayerCancelsOnlyThatPlayersActiveAuraTask() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask globalTask = mock(BukkitTask.class);
        BukkitTask auraTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(globalTask, auraTask);

            AuraManager manager = new AuraManager(plugin);
            manager.startTask();
            manager.addOrExtendActiveAura(player, new TestTimedAuraEffect(new AtomicInteger()), 20);

            manager.clearPlayer(player);

            verify(auraTask).cancel();
            verify(globalTask, never()).cancel();
        }
    }

    @Test
    void queuedPassiveAuraCallbackFromBeforeStopDoesNotRunAfterRestart() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        Player player = mock(Player.class);
        when(plugin.getServer()).thenReturn(server);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(player.isOnline()).thenReturn(true);

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask firstGlobalTask = mock(BukkitTask.class);
        BukkitTask playerTask = mock(BukkitTask.class);
        BukkitTask restartedGlobalTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(firstGlobalTask, restartedGlobalTask);
            when(scheduler.runTask(any(), any(Runnable.class))).thenReturn(playerTask);

            AuraManager manager = new AuraManager(plugin);
            AtomicInteger ticks = new AtomicInteger();
            manager.registerPassiveAura(new CountingAura(ticks));
            manager.startTask();

            ArgumentCaptor<Runnable> globalCallbacks = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTaskTimer(any(), globalCallbacks.capture(), anyLong(), anyLong());
            globalCallbacks.getValue().run();

            ArgumentCaptor<Runnable> playerCallbacks = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTask(any(), playerCallbacks.capture());

            manager.stop();
            manager.registerPassiveAura(new CountingAura(ticks));
            manager.startTask();

            playerCallbacks.getValue().run();

            assertEquals(0, ticks.get());
            verify(firstGlobalTask).cancel();
            verify(restartedGlobalTask, times(0)).cancel();
        }
    }

    private static final class TestTimedAuraEffect implements TimedAuraEffect {

        private final AtomicInteger ticks;
        private long expiresAtMillis = System.currentTimeMillis() + 10_000L;

        private TestTimedAuraEffect(AtomicInteger ticks) {
            this.ticks = ticks;
        }

        @Override
        public void tick(Player player) {
            ticks.incrementAndGet();
        }

        @Override
        public int getTickInterval() {
            return 2;
        }

        @Override
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMillis;
        }

        @Override
        public void extendDuration(int ticks) {
            expiresAtMillis = Math.max(expiresAtMillis, System.currentTimeMillis() + ticks * 50L);
        }
    }

    private static final class CountingAura implements AuraEffect {

        private final AtomicInteger ticks;

        private CountingAura(AtomicInteger ticks) {
            this.ticks = ticks;
        }

        @Override
        public void tick(Player player) {
            ticks.incrementAndGet();
        }

        @Override
        public int getTickInterval() {
            return 2;
        }
    }
}
