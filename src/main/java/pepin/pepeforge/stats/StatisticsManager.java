package pepin.pepeforge.stats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StatisticsManager {

    private final Plugin plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;

    private final Map<String, Integer> craftedCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> givenCounts = new ConcurrentHashMap<>();

    private boolean isDirty = false;

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
        if (itemId == null || itemId.isEmpty()) return;
        craftedCounts.merge(itemId, 1, Integer::sum);
        markDirty();
    }

    public void incrementGiven(String itemId) {
        if (itemId == null || itemId.isEmpty()) return;
        givenCounts.merge(itemId, 1, Integer::sum);
        markDirty();
    }

    public Map<String, Integer> getCraftedCounts() {
        return new HashMap<>(craftedCounts);
    }

    public Map<String, Integer> getGivenCounts() {
        return new HashMap<>(givenCounts);
    }

    private void markDirty() {
        if (!isDirty) {
            isDirty = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveAsync();
                }
            }.runTaskLaterAsynchronously(plugin, 60L); // save after 3 seconds asynchronously
        }
    }

    private void saveAsync() {
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
            isDirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats.yml", e);
            isDirty = true;
        }
    }
    
    public void forceSave() {
        if (isDirty) {
            saveAsync();
        }
    }
}
