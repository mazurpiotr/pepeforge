package pepin.pepeforge.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SchedulerCompat {

    private static final boolean FOLIA;

    static {
        boolean folia;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }

        FOLIA = folia;
    }

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void run(
            JavaPlugin plugin,
            Runnable runnable
    ) {
        if (FOLIA) {
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
        if (FOLIA) {
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
        if (FOLIA) {
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
        if (FOLIA) {
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
        Player player,
        JavaPlugin plugin,
        Runnable runnable,
        long delayTicks,
        long periodTicks
    ) {
        if (FOLIA) {
            var task = player.getScheduler().runAtFixedRate(
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