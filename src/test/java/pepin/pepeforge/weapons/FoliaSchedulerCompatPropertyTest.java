package pepin.pepeforge.weapons;

import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Bug Condition Exploration Property Test - Legacy Bukkit Scheduler Usage in
 * Weapon Listeners.
 *
 * This test encodes the EXPECTED behavior: none of the three affected listeners
 * should
 * use legacy Bukkit scheduler patterns. On UNFIXED code, this test MUST FAIL —
 * failure
 * confirms the bug exists (legacy scheduler usage is still present).
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5
 */
class FoliaSchedulerCompatPropertyTest {

        private static final Path SOURCE_ROOT = Path.of("src/main/java/pepin/pepeforge/weapons");

        private static final Map<String, Path> LISTENER_PATHS = Map.of(
                        "SolarShieldListener", SOURCE_ROOT.resolve("solarshield/SolarShieldListener.java"),
                        "GreatswordListener", SOURCE_ROOT.resolve("greatsword/GreatswordListener.java"),
                        "KatanaListener", SOURCE_ROOT.resolve("katana/KatanaListener.java"));

        // Legacy patterns that indicate the bug condition
        private static final Pattern BUKKIT_TASK_IMPORT = Pattern.compile(
                        "^\\s*import\\s+org\\.bukkit\\.scheduler\\.BukkitTask\\s*;",
                        Pattern.MULTILINE);

        private static final Pattern BUKKIT_TASK_FIELD = Pattern.compile(
                        "\\bBukkitTask\\b");

        private static final Pattern LEGACY_SCHEDULER_CALL = Pattern.compile(
                        "plugin\\.getServer\\(\\)\\.getScheduler\\(\\)\\.(runTaskTimer|runTask)\\(");

        private static final Pattern SCHEDULED_TASK_COMPAT_FIELD = Pattern.compile(
                        "\\bScheduledTaskCompat\\b");

        private static final Pattern SCHEDULER_COMPAT_CALL = Pattern.compile(
                        "\\bSchedulerCompat\\.(runTimer|runTimerForEntity|runForPlayer)\\(");

        @Provide
        Arbitrary<String> listenerNames() {
                return Arbitraries.of("SolarShieldListener", "GreatswordListener", "KatanaListener");
        }

        /**
         * Property 1: Bug Condition — No Legacy Bukkit Scheduler Usage.
         *
         * For all affected listeners, the source code SHALL NOT contain:
         * - imports of org.bukkit.scheduler.BukkitTask
         * - field declarations using BukkitTask type
         * - direct calls to plugin.getServer().getScheduler().runTaskTimer() or
         * runTask()
         *
         * Instead, all task fields MUST use ScheduledTaskCompat type and scheduling
         * MUST go through SchedulerCompat methods.
         *
         * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5
         */
        @Property
        void noLegacyBukkitSchedulerUsage(@ForAll("listenerNames") String listenerName) throws IOException {
                Path filePath = LISTENER_PATHS.get(listenerName);
                String sourceCode = Files.readString(filePath);

                // Assert: No BukkitTask import
                boolean hasBukkitTaskImport = BUKKIT_TASK_IMPORT.matcher(sourceCode).find();
                assert !hasBukkitTaskImport : listenerName + " contains legacy import: org.bukkit.scheduler.BukkitTask";

                // Assert: No BukkitTask field references
                boolean hasBukkitTaskField = BUKKIT_TASK_FIELD.matcher(sourceCode).find();
                assert !hasBukkitTaskField : listenerName + " contains BukkitTask field declaration(s)";

                // Assert: No direct legacy scheduler calls
                boolean hasLegacySchedulerCall = LEGACY_SCHEDULER_CALL.matcher(sourceCode).find();
                assert !hasLegacySchedulerCall
                                : listenerName + " contains direct plugin.getServer().getScheduler() call(s)";

                // Assert: Uses ScheduledTaskCompat for task fields
                // (If there are task fields, they should be ScheduledTaskCompat)
                boolean usesScheduledTaskCompat = SCHEDULED_TASK_COMPAT_FIELD.matcher(sourceCode).find();
                boolean hasTaskFields = sourceCode.contains("Task ") || sourceCode.contains("Tasks ");
                if (hasTaskFields) {
                        assert usesScheduledTaskCompat
                                        : listenerName + " has task fields but does not use ScheduledTaskCompat type";
                }

                // Assert: Uses SchedulerCompat methods for scheduling
                boolean usesSchedulerCompat = SCHEDULER_COMPAT_CALL.matcher(sourceCode).find();
                assert usesSchedulerCompat : listenerName + " does not use SchedulerCompat methods for scheduling";
        }
}
