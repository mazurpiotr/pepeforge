package pepin.pepeforge.stats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class StatisticsManager {

    private final Plugin plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;

    private final Map<String, Integer> craftedCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> givenCounts = new ConcurrentHashMap<>();

    private final AtomicLong changeVersion = new AtomicLong();
    private final AtomicLong savedVersion = new AtomicLong();
    private final Object saveLock = new Object();
    private final Object scheduleLock = new Object();
    private boolean saveScheduled;
    private ScheduledTaskCompat pendingSaveTask;

    public StatisticsManager(Plugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create stats.yml", e);
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        if (statsConfig.isConfigurationSection("crafted")) {
            for (String key : statsConfig.getConfigurationSection("crafted").getKeys(false)) {
                craftedCounts.put(key, statsConfig.getInt("crafted." + key));
            }
        }
        if (statsConfig.isConfigurationSection("given")) {
            for (String key : statsConfig.getConfigurationSection("given").getKeys(false)) {
                givenCounts.put(key, statsConfig.getInt("given." + key));
            }
        }
    }

    public void incrementCrafted(String itemId) {
        if (itemId == null || itemId.isEmpty())
            return;
        craftedCounts.merge(itemId, 1, (Integer a, Integer b) -> Integer.sum(a, b));
        markDirty();
    }

    public void incrementGiven(String itemId) {
        if (itemId == null || itemId.isEmpty())
            return;
        givenCounts.merge(itemId, 1, (Integer a, Integer b) -> Integer.sum(a, b));
        markDirty();
    }

    public Map<String, Integer> getCraftedCounts() {
        return new HashMap<>(craftedCounts);
    }

    public Map<String, Integer> getGivenCounts() {
        return new HashMap<>(givenCounts);
    }

    private void markDirty() {
        changeVersion.incrementAndGet();
        scheduleSave();
    }

    private void scheduleSave() {
        synchronized (scheduleLock) {
            if (saveScheduled) {
                return;
            }
            saveScheduled = true;
            try {
                pendingSaveTask = SchedulerCompat.runLaterAsync(plugin, this::runScheduledSave, 60L);
            } catch (RuntimeException exception) {
                saveScheduled = false;
                pendingSaveTask = null;
                plugin.getLogger().log(Level.SEVERE, "Could not schedule a delayed statistics save", exception);
            }
        }
    }

    private void runScheduledSave() {
        synchronized (scheduleLock) {
            pendingSaveTask = null;
            saveScheduled = false;
        }

        if (saveSnapshot() && changeVersion.get() != savedVersion.get()) {
            scheduleSave();
        }
    }

    private boolean saveSnapshot() {
        synchronized (saveLock) {
            long snapshotVersion = changeVersion.get();
            FileConfiguration configToSave = new YamlConfiguration();

            Map<String, Integer> craftedCopy = new HashMap<>(craftedCounts);
            for (Map.Entry<String, Integer> entry : craftedCopy.entrySet()) {
                configToSave.set("crafted." + entry.getKey(), entry.getValue());
            }

            Map<String, Integer> givenCopy = new HashMap<>(givenCounts);
            for (Map.Entry<String, Integer> entry : givenCopy.entrySet()) {
                configToSave.set("given." + entry.getKey(), entry.getValue());
            }

            try {
                configToSave.save(statsFile);
                savedVersion.set(snapshotVersion);
                return true;
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not save stats.yml", exception);
                return false;
            }
        }
    }

    public void forceSave() {
        synchronized (scheduleLock) {
            if (pendingSaveTask != null) {
                pendingSaveTask.cancel();
                pendingSaveTask = null;
            }
            saveScheduled = false;
        }

        while (changeVersion.get() != savedVersion.get()) {
            long previousSavedVersion = savedVersion.get();
            if (!saveSnapshot() || savedVersion.get() == previousSavedVersion) {
                return;
            }
        }
    }
}
