package pepin.pepeforge.util.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SchedulerCompat {

    private static final boolean REGIONIZED;
    private static java.lang.reflect.Method teleportAsyncMethod;

    static {
        boolean regionized;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            regionized = true;
        } catch (ClassNotFoundException ignored) {
            regionized = false;
        }
        REGIONIZED = regionized;

        try {
            teleportAsyncMethod = Entity.class.getMethod("teleportAsync", Location.class);
        } catch (NoSuchMethodException ignored) {
            teleportAsyncMethod = null;
        }
    }

    private SchedulerCompat() {
    }

    public static boolean isRegionized() {
        return REGIONIZED;
    }

    public static void teleport(Entity entity, Location location) {
        if (teleportAsyncMethod != null) {
            try {
                teleportAsyncMethod.invoke(entity, location);
                return;
            } catch (Exception ignored) {
            }
        }
        entity.teleport(location);
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
            long foliaDelay = Math.max(1L, delayTicks);
            Bukkit.getGlobalRegionScheduler().runDelayed(
                    plugin,
                    task -> runnable.run(),
                    foliaDelay
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
            long foliaDelay = Math.max(1L, delayTicks);
            var task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    foliaDelay,
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

    public static void runForEntity(
            org.bukkit.entity.Entity entity,
            JavaPlugin plugin,
            Runnable runnable
    ) {
        if (REGIONIZED) {
            entity.getScheduler().run(
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
            long foliaDelay = Math.max(1L, delayTicks);
            var task = entity.getScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    null,
                    foliaDelay,
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

    public static ScheduledTaskCompat runLaterForPlayer(
            Player player,
            JavaPlugin plugin,
            Runnable runnable,
            long delayTicks
    ) {
        if (isRegionized()) {
            long foliaDelay = Math.max(1L, delayTicks);
            var task = player.getScheduler().runDelayed(
                    plugin,
                    taskRef -> runnable.run(),
                    null,
                    foliaDelay
            );
            return task != null ? task::cancel : () -> {};
        }

        var task = Bukkit.getScheduler().runTaskLater(
                plugin,
                runnable,
                delayTicks
        );
        return task::cancel;
    }
}