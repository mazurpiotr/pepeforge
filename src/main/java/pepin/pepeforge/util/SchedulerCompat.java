package pepin.pepeforge.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SchedulerCompat {

    private static final boolean REGIONIZED = SchedulerCompat.class.getClassLoader()
            .getResource("io/papermc/paper/threadedregions/RegionizedServer.class") != null;

    private SchedulerCompat() {
    }

    public static boolean isRegionized() {
        return REGIONIZED;
    }

    public static void run(
            JavaPlugin plugin,
            Runnable runnable
    ) {
        if (REGIONIZED) {
            Bukkit.getGlobalRegionScheduler().execute(
                    plugin,
                    runnable
            );
        } else {
            Bukkit.getScheduler().runTask(
                    plugin,
                    runnable
            );
        }
    }

    public static void runLater(
            JavaPlugin plugin,
            Runnable runnable,
            long delayTicks
    ) {
        if (REGIONIZED) {
            Bukkit.getGlobalRegionScheduler().runDelayed(
                    plugin,
                    task -> runnable.run(),
                    delayTicks
            );
        } else {
            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    runnable,
                    delayTicks
            );
        }
    }

    public static ScheduledTaskCompat runTimer(
        JavaPlugin plugin,
        Runnable runnable,
        long delayTicks,
        long periodTicks
    ) {
        if (REGIONIZED) {
            var task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    delayTicks,
                    periodTicks
            );

            return task::cancel;
        }

        var task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                runnable,
                delayTicks,
                periodTicks
        );

        return task::cancel;
    }

    public static void runForPlayer(
            Player player,
            JavaPlugin plugin,
            Runnable runnable
    ) {
        if (REGIONIZED) {
            player.getScheduler().run(
                    plugin,
                    task -> runnable.run(),
                    null
            );
        } else {
            Bukkit.getScheduler().runTask(
                    plugin,
                    runnable
            );
        }
    }

    public static ScheduledTaskCompat runTimerForEntity(
        org.bukkit.entity.Entity entity,
        JavaPlugin plugin,
        Runnable runnable,
        long delayTicks,
        long periodTicks
    ) {
        if (REGIONIZED) {
            var task = entity.getScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    null,
                    delayTicks,
                    periodTicks
            );
            // If player logs out, the task will be cancelled by Folia, so we return a no-op cancel function in that case
            return task != null ? task::cancel : () -> {};
        }

        var task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                runnable,
                delayTicks,
                periodTicks
        );

        return task::cancel;
    }
}