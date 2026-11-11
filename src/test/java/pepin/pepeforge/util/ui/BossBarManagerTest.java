package pepin.pepeforge.util.ui;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BossBarManagerTest {

    @Test
    void disabledPluginHidesBarsWithoutSchedulingOrAccessingPlayers() {
        verifyShutdown(false, false);
    }

    @Test
    void enabledReloadHidesBarsWithoutSchedulingOrAccessingPlayers() {
        verifyShutdown(true, false);
    }

    @Test
    void serverShutdownHidesBarsWithoutSchedulingOrAccessingPlayers() {
        verifyShutdown(true, true);
    }

    @Test
    void uuidCleanupRemovesOnlyTheRequestedBar() {
        JavaPlugin plugin = plugin(true);
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        BossBar first = mock(BossBar.class);
        BossBar second = mock(BossBar.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.BLUE), eq(BarStyle.SOLID)))
                    .thenReturn(first, second);
            BossBarManager manager = new BossBarManager(plugin);
            manager.updateBar(player, "first", "First", 1.0D, BarColor.BLUE);
            manager.updateBar(player, "second", "Second", 1.0D, BarColor.BLUE);
            manager.removeBar(playerId, "first");
            manager.removeBar(playerId, "first");
            verify(first).setVisible(false);
            verify(first, never()).removePlayer(any());
            verify(second, never()).setVisible(false);
            manager.updateBar(player, "second", "Remaining", 0.5D, BarColor.BLUE);
            verify(second).setTitle("Remaining");
        }
    }

    private void verifyShutdown(boolean enabled, boolean stopping) {
        JavaPlugin plugin = plugin(enabled);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        BossBar bar = mock(BossBar.class);
        try (var bukkit = mockStatic(Bukkit.class); var scheduler = mockStatic(SchedulerCompat.class)) {
            bukkit.when(() -> Bukkit.createBossBar(anyString(), eq(BarColor.BLUE), eq(BarStyle.SOLID)))
                    .thenReturn(bar);
            scheduler.when(SchedulerCompat::isServerStopping).thenReturn(stopping);
            BossBarManager manager = new BossBarManager(plugin);
            manager.updateBar(player, "shield", "Shield", 1.0D, BarColor.BLUE);
            manager.clearAll();
            manager.clearAll();
            manager.updateBar(player, "shield", "Late update", 1.0D, BarColor.BLUE);
            verify(bar).setVisible(false);
            verify(bar, never()).removePlayer(any());
            verify(bar, never()).setTitle("Late update");
            scheduler.verify(() -> SchedulerCompat.runForPlayer(any(), any(), any()), never());
            bukkit.verify(() -> Bukkit.getPlayer(any(UUID.class)), never());
        }
    }

    private JavaPlugin plugin(boolean enabled) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(enabled);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        return plugin;
    }
}
