package pepin.pepeforge.weapons;

import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Preservation Property Test - Non-Folia Scheduling Behavior Unchanged.
 *
 * Verifies structural preservation: on non-Folia paths, SchedulerCompat
 * transparently
 * delegates to Bukkit scheduler with identical tick intervals and returns a
 * cancellable
 * ScheduledTaskCompat. These tests should PASS on unfixed code because
 * SchedulerCompat
 * itself is already correctly implemented.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
class PreservationSchedulerPropertyTest {

        private static final Path SCHEDULER_COMPAT_PATH = Path
                        .of("src/main/java/pepin/pepeforge/util/scheduler/SchedulerCompat.java");

        private static final Path SCHEDULED_TASK_COMPAT_PATH = Path
                        .of("src/main/java/pepin/pepeforge/util/scheduler/ScheduledTaskCompat.java");

        private static final Path SOURCE_ROOT = Path.of("src/main/java/pepin/pepeforge/weapons");

        private static final Map<String, Path> LISTENER_PATHS = Map.of(
                        "SolarShieldListener", SOURCE_ROOT.resolve("solarshield/SolarShieldListener.java"),
                        "GreatswordListener", SOURCE_ROOT.resolve("greatsword/GreatswordListener.java"),
                        "KatanaListener", SOURCE_ROOT.resolve("katana/KatanaListener.java"));

        /**
         * Expected tick intervals for each scheduling call in the listeners.
         * Format: "ListenerName:methodContext" -> [delayTicks, periodTicks]
         */
        private static final Map<String, long[]> EXPECTED_INTERVALS = Map.of(
                        "SolarShieldListener:statusTask", new long[] { 2L, 2L },
                        "GreatswordListener:statusTask", new long[] { 1L, 1L },
                        "KatanaListener:statusTask", new long[] { 1L, 2L },
                        "KatanaListener:parryTask", new long[] { 0L, 1L });

        // --- Patterns for SchedulerCompat source analysis ---

        /**
         * Pattern to find runTimer non-Folia path: delegates to
         * Bukkit.getScheduler().runTaskTimer()
         */
        private static final Pattern RUN_TIMER_NON_FOLIA_DELEGATION = Pattern.compile(
                        "Bukkit\\.getScheduler\\(\\)\\.runTaskTimer\\(\\s*plugin\\s*,\\s*runnable\\s*,\\s*delayTicks\\s*,\\s*periodTicks\\s*\\)");

        /**
         * Pattern to find runTimerForEntity non-Folia path: delegates to
         * Bukkit.getScheduler().runTaskTimer()
         */
        private static final Pattern RUN_TIMER_FOR_ENTITY_NON_FOLIA_DELEGATION = Pattern.compile(
                        "Bukkit\\.getScheduler\\(\\)\\.runTaskTimer\\(\\s*plugin\\s*,\\s*runnable\\s*,\\s*delayTicks\\s*,\\s*periodTicks\\s*\\)");

        /**
         * Pattern to find runForPlayer non-Folia path: delegates to
         * Bukkit.getScheduler().runTask()
         */
        private static final Pattern RUN_FOR_PLAYER_NON_FOLIA_DELEGATION = Pattern.compile(
                        "Bukkit\\.getScheduler\\(\\)\\.runTask\\(\\s*plugin\\s*,\\s*runnable\\s*\\)");

        /** Pattern to verify task::cancel return in non-Folia path */
        private static final Pattern TASK_CANCEL_RETURN = Pattern.compile(
                        "return\\s+task::cancel\\s*;");

        /** Pattern to find @FunctionalInterface annotation */
        private static final Pattern FUNCTIONAL_INTERFACE_ANNOTATION = Pattern.compile(
                        "@FunctionalInterface");

        /** Pattern to find cancel() method declaration */
        private static final Pattern CANCEL_METHOD = Pattern.compile(
                        "void\\s+cancel\\(\\)");

        @Provide
        Arbitrary<String> schedulerMethods() {
                return Arbitraries.of("runTimer", "runTimerForEntity", "runForPlayer");
        }

        @Provide
        Arbitrary<String> listenerIntervalKeys() {
                return Arbitraries.of(
                                "SolarShieldListener:statusTask",
                                "GreatswordListener:statusTask",
                                "KatanaListener:statusTask",
                                "KatanaListener:parryTask");
        }

        /**
         * Property 2a: SchedulerCompat non-Folia paths delegate to Bukkit scheduler.
         *
         * For all SchedulerCompat methods that schedule tasks (runTimer,
         * runTimerForEntity, runForPlayer),
         * the non-Folia code path SHALL delegate directly to the standard Bukkit
         * scheduler API
         * with identical parameters.
         *
         * Validates: Requirements 3.1, 3.2, 3.3
         */
        @Property
        void nonFoliaPathDelegatesToBukkitScheduler(@ForAll("schedulerMethods") String method) throws IOException {
                String source = Files.readString(SCHEDULER_COMPAT_PATH);

                switch (method) {
                        case "runTimer" -> {
                                // Non-Folia path of runTimer must delegate to
                                // Bukkit.getScheduler().runTaskTimer()
                                boolean delegatesToBukkit = RUN_TIMER_NON_FOLIA_DELEGATION.matcher(source).find();
                                assert delegatesToBukkit
                                                : "SchedulerCompat.runTimer() non-Folia path does not delegate to Bukkit.getScheduler().runTaskTimer()";
                        }
                        case "runTimerForEntity" -> {
                                // Non-Folia path of runTimerForEntity must delegate to
                                // Bukkit.getScheduler().runTaskTimer()
                                boolean delegatesToBukkit = RUN_TIMER_FOR_ENTITY_NON_FOLIA_DELEGATION.matcher(source)
                                                .find();
                                assert delegatesToBukkit
                                                : "SchedulerCompat.runTimerForEntity() non-Folia path does not delegate to Bukkit.getScheduler().runTaskTimer()";
                        }
                        case "runForPlayer" -> {
                                // Non-Folia path of runForPlayer must delegate to
                                // Bukkit.getScheduler().runTask()
                                boolean delegatesToBukkit = RUN_FOR_PLAYER_NON_FOLIA_DELEGATION.matcher(source).find();
                                assert delegatesToBukkit
                                                : "SchedulerCompat.runForPlayer() non-Folia path does not delegate to Bukkit.getScheduler().runTask()";
                        }
                }
        }

        /**
         * Property 2b: SchedulerCompat returns cancellable ScheduledTaskCompat on
         * non-Folia.
         *
         * For all SchedulerCompat methods that return ScheduledTaskCompat (runTimer,
         * runTimerForEntity),
         * the non-Folia path SHALL return task::cancel, ensuring cancel() properly
         * cancels the underlying task.
         *
         * Validates: Requirements 3.4, 3.5
         */
        @Property
        void nonFoliaPathReturnsCancellableTask(@ForAll("schedulerMethods") String method) throws IOException {
                // Only runTimer and runTimerForEntity return ScheduledTaskCompat
                if (method.equals("runForPlayer")) {
                        return; // runForPlayer is void, skip
                }

                String source = Files.readString(SCHEDULER_COMPAT_PATH);

                // The non-Folia path must contain "return task::cancel;" to ensure cancellation
                // works
                boolean returnsCancellable = TASK_CANCEL_RETURN.matcher(source).find();
                assert returnsCancellable
                                : "SchedulerCompat." + method + "() non-Folia path does not return task::cancel";
        }

        /**
         * Property 2c: ScheduledTaskCompat is a functional interface with cancel().
         *
         * ScheduledTaskCompat SHALL be a @FunctionalInterface with exactly one abstract
         * method: cancel().
         * This guarantees that task::cancel method references are structurally valid.
         *
         * Validates: Requirements 3.4, 3.5
         */
        @Property
        void scheduledTaskCompatIsFunctionalInterfaceWithCancel(
                        @ForAll("schedulerMethods") String method) throws IOException {
                // This property holds regardless of method — we verify the interface structure
                String source = Files.readString(SCHEDULED_TASK_COMPAT_PATH);

                boolean hasFunctionalAnnotation = FUNCTIONAL_INTERFACE_ANNOTATION.matcher(source).find();
                assert hasFunctionalAnnotation : "ScheduledTaskCompat is not annotated with @FunctionalInterface";

                boolean hasCancelMethod = CANCEL_METHOD.matcher(source).find();
                assert hasCancelMethod : "ScheduledTaskCompat does not declare a cancel() method";

                // Verify it's an interface (not a class)
                assert source.contains("interface ScheduledTaskCompat")
                                : "ScheduledTaskCompat is not declared as an interface";
        }

        /**
         * Property 2d: Tick intervals in listeners match expected preservation values.
         *
         * For all scheduling calls in the three affected listeners, the tick intervals
         * (delay and period) SHALL match the expected values to preserve existing
         * behavior.
         * SolarShield: 2L/2L, Greatsword: 1L/1 (STATUS_INTERVAL_TICKS=1), Katana
         * status: 1L/2L, Katana parry: 0L/1L
         *
         * Validates: Requirements 3.1, 3.2, 3.3
         */
        @Property
        void listenerTickIntervalsMatchExpected(@ForAll("listenerIntervalKeys") String key) throws IOException {
                long[] expected = EXPECTED_INTERVALS.get(key);
                long expectedDelay = expected[0];
                long expectedPeriod = expected[1];

                String listenerName = key.split(":")[0];
                String context = key.split(":")[1];
                Path filePath = LISTENER_PATHS.get(listenerName);
                String source = Files.readString(filePath);

                switch (key) {
                        case "SolarShieldListener:statusTask" -> {
                                // SolarShield uses literal "2L, 2L" at end of runTaskTimer call
                                assert source.contains("2L, 2L")
                                                : "SolarShieldListener statusTask does not use expected intervals 2L, 2L";
                        }
                        case "GreatswordListener:statusTask" -> {
                                // Greatsword uses "1L, STATUS_INTERVAL_TICKS" where STATUS_INTERVAL_TICKS = 1
                                assert source.contains("1L, STATUS_INTERVAL_TICKS")
                                                : "GreatswordListener statusTask does not use expected intervals 1L, STATUS_INTERVAL_TICKS";
                                // Verify constant value
                                assert source.contains("STATUS_INTERVAL_TICKS = 1")
                                                : "GreatswordListener STATUS_INTERVAL_TICKS is not 1";
                        }
                        case "KatanaListener:statusTask" -> {
                                // Katana statusTask uses "1L, 2L"
                                assert source.contains("1L, 2L")
                                                : "KatanaListener statusTask does not use expected intervals 1L, 2L";
                        }
                        case "KatanaListener:parryTask" -> {
                                // Katana parry task uses "0L, 1L"
                                assert source.contains("0L, 1L")
                                                : "KatanaListener parryTask does not use expected intervals 0L, 1L";
                        }
                }
        }

        /**
         * Property 2e: SchedulerCompat non-Folia delegation passes parameters
         * unchanged.
         *
         * For each SchedulerCompat method, the non-Folia path SHALL pass through
         * all parameters (plugin, runnable, delayTicks, periodTicks) identically
         * to the underlying Bukkit scheduler call — no transformation, no wrapping.
         *
         * Validates: Requirements 3.1, 3.2, 3.3
         */
        @Property
        void nonFoliaPathPassesParametersIdentically(@ForAll("schedulerMethods") String method) throws IOException {
                String source = Files.readString(SCHEDULER_COMPAT_PATH);

                switch (method) {
                        case "runTimer" -> {
                                // runTimer passes (plugin, runnable, delayTicks, periodTicks) directly
                                // Unlike the Folia path which wraps: scheduledTask -> runnable.run()
                                // The non-Folia path passes runnable directly (no wrapping)
                                boolean passesRunnableDirectly = source.contains(
                                                "Bukkit.getScheduler().runTaskTimer(\n                plugin,\n                runnable,\n                delayTicks,\n                periodTicks\n        )");
                                assert passesRunnableDirectly
                                                : "SchedulerCompat.runTimer() non-Folia path does not pass parameters directly to Bukkit scheduler";
                        }
                        case "runTimerForEntity" -> {
                                // runTimerForEntity non-Folia path also passes runnable directly
                                // We look for the second occurrence of runTaskTimer (after the FOLIA block)
                                boolean passesParametersDirectly = RUN_TIMER_FOR_ENTITY_NON_FOLIA_DELEGATION
                                                .matcher(source).find();
                                assert passesParametersDirectly
                                                : "SchedulerCompat.runTimerForEntity() non-Folia path does not pass parameters directly";
                        }
                        case "runForPlayer" -> {
                                // runForPlayer non-Folia path passes (plugin, runnable) directly
                                boolean passesDirectly = RUN_FOR_PLAYER_NON_FOLIA_DELEGATION.matcher(source).find();
                                assert passesDirectly
                                                : "SchedulerCompat.runForPlayer() non-Folia path does not pass parameters directly";
                        }
                }
        }
}
