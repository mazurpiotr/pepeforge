package pepin.pepeforge.util.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SchedulerCompat {

    private static final boolean REGIONIZED;
    private static java.lang.reflect.Method teleportAsyncMethod;
    private static final Method serverStoppingMethod;
    private static final Method asyncSchedulerGetter;
    private static final Method asyncRunDelayedMethod;
    private static final Method asyncTaskCancelMethod;

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

        Method stoppingMethod;
        try {
            stoppingMethod = Bukkit.class.getMethod("isStopping");
        } catch (NoSuchMethodException ignored) {
            stoppingMethod = null;
        }
        serverStoppingMethod = stoppingMethod;

        Method asyncGetter = null;
        Method asyncRunDelayed = null;
        Method asyncTaskCancel = null;
        try {
            asyncGetter = Bukkit.class.getMethod("getAsyncScheduler");
            asyncRunDelayed = asyncGetter.getReturnType().getMethod(
                    "runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
            asyncTaskCancel = asyncRunDelayed.getReturnType().getMethod("cancel");
        } catch (NoSuchMethodException ignored) {
            // Older Bukkit implementations do not provide the Folia async scheduler API.
        }
        asyncSchedulerGetter = asyncGetter;
        asyncRunDelayedMethod = asyncRunDelayed;
        asyncTaskCancelMethod = asyncTaskCancel;
    }

    private SchedulerCompat() {
    }

    public static boolean isRegionized() {
        return REGIONIZED;
    }

    public static boolean isServerStopping() {
        if (serverStoppingMethod == null) {
            return false;
        }
        try {
            return (boolean) serverStoppingMethod.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to determine whether the server is stopping", exception);
        }
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        return !REGIONIZED || Bukkit.isOwnedByCurrentRegion(entity);
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

    public static ScheduledTaskCompat runLaterAsync(
            Plugin plugin,
            Runnable runnable,
            long delayTicks
    ) {
        if (!REGIONIZED) {
            var task = Bukkit.getScheduler().runTaskLaterAsynchronously(
                    plugin,
                    runnable,
                    delayTicks
            );
            return task::cancel;
        }

        if (asyncSchedulerGetter == null || asyncRunDelayedMethod == null || asyncTaskCancelMethod == null) {
            throw new IllegalStateException("Folia async scheduler API is unavailable");
        }

        long delayMillis = Math.multiplyExact(Math.max(1L, delayTicks), 50L);
        try {
            Object asyncScheduler = asyncSchedulerGetter.invoke(null);
            Object task = asyncRunDelayedMethod.invoke(
                    asyncScheduler,
                    plugin,
                    (Consumer<Object>) ignored -> runnable.run(),
                    delayMillis,
                    TimeUnit.MILLISECONDS
            );
            return () -> invokeAsyncTaskCancel(task);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocationException
                    ? invocationException.getCause()
                    : exception;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Could not schedule a Folia async task", cause);
        }
    }

    private static void invokeAsyncTaskCancel(Object task) {
        try {
            asyncTaskCancelMethod.invoke(task);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocationException
                    ? invocationException.getCause()
                    : exception;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Could not cancel a Folia async task", cause);
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
        runForPlayer(player, plugin, runnable, null);
    }

    public static void runForPlayer(
        Player player,
        JavaPlugin plugin,
        Runnable runnable,
        Runnable retired
    ) {
        if (REGIONIZED) {
            player.getScheduler().run(
                    plugin,
                    task -> runnable.run(),
                    retired
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

    public static void runAtLocation(
            Location location,
            JavaPlugin plugin,
            Runnable runnable
    ) {
        if (REGIONIZED) {
            Bukkit.getRegionScheduler().run(
                    plugin,
                    location,
                    task -> runnable.run()
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
        return runTimerForEntity(entity, plugin, runnable, null, delayTicks, periodTicks);
    }

    public static ScheduledTaskCompat runTimerForEntity(
        org.bukkit.entity.Entity entity,
        JavaPlugin plugin,
        Runnable runnable,
        Runnable retired,
        long delayTicks,
        long periodTicks
    ) {
        if (REGIONIZED) {
            long foliaDelay = Math.max(1L, delayTicks);
            var task = entity.getScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> runnable.run(),
                    retired,
                    foliaDelay,
                    periodTicks
            );
            if (task == null && retired != null) {
                retired.run();
            }
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

    public static ScheduledTaskCompat runLaterForEntity(
            org.bukkit.entity.Entity entity,
            JavaPlugin plugin,
            Runnable runnable,
            long delayTicks
    ) {
        if (isRegionized()) {
            long foliaDelay = Math.max(1L, delayTicks);
            var task = entity.getScheduler().runDelayed(
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
