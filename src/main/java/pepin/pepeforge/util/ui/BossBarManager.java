package pepin.pepeforge.util.ui;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, BossBar>> playerBars = new ConcurrentHashMap<>();
    private volatile boolean acceptingUpdates = true;

    public BossBarManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Updates or creates a BossBar for a specific player.
     *
     * @param player   The player to show the bar to.
     * @param barId    A unique identifier for this bar (e.g., "solar_shield").
     * @param title    The text to display on the bar.
     * @param progress The progress from 0.0 to 1.0.
     * @param color    The color of the bar.
     */
    public void updateBar(Player player, String barId, String title, double progress, BarColor color) {
        if (!acceptingUpdates) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Map<String, BossBar> bars = playerBars.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());

        BossBar bar = bars.get(barId);
        if (bar == null) {
            bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
            bar.addPlayer(player);
            bars.put(barId, bar);
        } else {
            bar.setTitle(title);
            bar.setColor(color);
        }

        // Clamp progress to 0.0 - 1.0 to prevent exceptions
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    /**
     * Removes a specific BossBar for a player.
     *
     * @param player The player.
     * @param barId  The unique identifier of the bar.
     */
    public void removeBar(Player player, String barId) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBar> bars = playerBars.get(playerId);
        if (bars != null) {
            BossBar bar = bars.remove(barId);
            if (bar != null) {
                bar.removePlayer(player);
            }
            if (bars.isEmpty()) {
                playerBars.remove(playerId);
            }
        }
    }

    /**
     * Removes all BossBars for a specific player.
     */
    public void clearBars(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, BossBar> bars = playerBars.remove(playerId);
        if (bars != null) {
            for (BossBar bar : bars.values()) {
                bar.removePlayer(player);
            }
            bars.clear();
        }
    }

    /**
     * Clears all BossBars for all players. Useful during plugin disable.
     */
    public void clearAll() {
        acceptingUpdates = false;
        for (UUID playerId : playerBars.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                Map<String, BossBar> bars = playerBars.remove(playerId);
                if (bars != null) {
                    bars.clear();
                }
                continue;
            }

            SchedulerCompat.runForPlayer(player, plugin, () -> clearBars(player));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearBars(event.getPlayer());
    }
}
