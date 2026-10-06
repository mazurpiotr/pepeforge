package pepin.pepeforge.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class StatisticsManagerTest {

    @TempDir
    Path dataFolder;

    @Test
    void retriesSchedulingAfterSchedulerRejectsInitialSave() {
        Plugin plugin = createPlugin();
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<Runnable> pendingSave = new AtomicReference<>();

        try (MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.runLaterAsync(
                    any(Plugin.class), any(Runnable.class), anyLong()))
                    .thenAnswer(invocation -> {
                        if (attempts.incrementAndGet() == 1) {
                            throw new IllegalStateException("Scheduler rejected task");
                        }
                        pendingSave.set(invocation.getArgument(1));
                        return (ScheduledTaskCompat) () -> { };
                    });

            StatisticsManager manager = new StatisticsManager(plugin);
            manager.incrementGiven("anchor");
            assertNull(pendingSave.get());

            manager.incrementCrafted("anchor");
            assertNotNull(pendingSave.get());
            pendingSave.get().run();

            YamlConfiguration saved = YamlConfiguration.loadConfiguration(
                    dataFolder.resolve("stats.yml").toFile());
            assertEquals(1, saved.getInt("given.anchor"));
            assertEquals(1, saved.getInt("crafted.anchor"));
        }
    }

    @Test
    void forceSaveCancelsPendingTaskAndWritesCurrentCounts() {
        Plugin plugin = createPlugin();
        AtomicReference<Runnable> pendingSave = new AtomicReference<>();
        AtomicInteger cancellations = new AtomicInteger();

        try (MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.runLaterAsync(
                    any(Plugin.class), any(Runnable.class), anyLong()))
                    .thenAnswer(invocation -> {
                        pendingSave.set(invocation.getArgument(1));
                        return (ScheduledTaskCompat) cancellations::incrementAndGet;
                    });

            StatisticsManager manager = new StatisticsManager(plugin);
            manager.incrementGiven("anchor");
            manager.forceSave();

            YamlConfiguration saved = YamlConfiguration.loadConfiguration(
                    dataFolder.resolve("stats.yml").toFile());
            assertEquals(1, saved.getInt("given.anchor"));
            assertEquals(1, cancellations.get());
        }
    }

    private Plugin createPlugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Logger logger = Logger.getLogger("StatisticsManagerTest");
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        when(plugin.getLogger()).thenReturn(logger);
        return plugin;
    }
}
