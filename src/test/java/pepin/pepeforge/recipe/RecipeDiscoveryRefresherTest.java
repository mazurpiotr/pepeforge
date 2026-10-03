package pepin.pepeforge.recipe;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
