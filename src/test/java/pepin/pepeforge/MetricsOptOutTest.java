package pepin.pepeforge;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsOptOutTest {

    @Test
    void optingOutBeforeInitializationDoesNotStartReporting() {
        YamlConfiguration config = new YamlConfiguration();
        PepeForgePlugin plugin = plugin(config);
        try (MockedConstruction<Metrics> reporting = mockConstruction(Metrics.class)) {
            plugin.setMetricsEnabled(false);

            assertFalse(config.getBoolean("metrics.enabled"));
            assertTrue(reporting.constructed().isEmpty());
            verify(plugin).saveConfig();
        }
    }

    @Test
    void optingOutStopsTheReporterAndOptingInCreatesOneReplacement() {
        YamlConfiguration config = new YamlConfiguration();
        PepeForgePlugin plugin = plugin(config);
        try (MockedConstruction<Metrics> reporting = mockConstruction(Metrics.class)) {
            plugin.setMetricsEnabled(true);
            plugin.setMetricsEnabled(true);
            assertEquals(1, reporting.constructed().size());
            Metrics original = reporting.constructed().getFirst();
            verify(original, times(5)).addCustomChart(any());

            plugin.setMetricsEnabled(false);
            plugin.setMetricsEnabled(false);
            verify(original).shutdown();
            assertFalse(config.getBoolean("metrics.enabled"));

            plugin.setMetricsEnabled(true);
            assertTrue(config.getBoolean("metrics.enabled"));
            assertEquals(2, reporting.constructed().size());
            verify(reporting.constructed().get(1), never()).shutdown();
            verify(plugin, times(5)).saveConfig();
        }
    }

    private PepeForgePlugin plugin(YamlConfiguration config) {
        PepeForgePlugin plugin = mock(PepeForgePlugin.class, CALLS_REAL_METHODS);
        doReturn(config).when(plugin).getConfig();
        doNothing().when(plugin).saveConfig();
        return plugin;
    }
}
