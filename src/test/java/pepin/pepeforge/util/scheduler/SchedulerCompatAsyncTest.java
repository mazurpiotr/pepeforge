package pepin.pepeforge.util.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulerCompatAsyncTest {

    @Test
    void nonFoliaPathUsesCancellableBukkitAsyncTask() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskLaterAsynchronously(plugin, runnable, 60L)).thenReturn(task);

            ScheduledTaskCompat scheduledTask = SchedulerCompat.runLaterAsync(plugin, runnable, 60L);

            assertNotNull(scheduledTask);
            scheduledTask.cancel();
            verify(scheduler).runTaskLaterAsynchronously(plugin, runnable, 60L);
            verify(task).cancel();
        }
    }
}
