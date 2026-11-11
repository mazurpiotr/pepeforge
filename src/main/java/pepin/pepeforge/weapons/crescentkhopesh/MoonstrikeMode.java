package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.Locale;

import org.bukkit.configuration.file.FileConfiguration;

public enum MoonstrikeMode {
    WAVE,
    SWEEP;

    public static final String CONFIG_PATH = "mechanics.crescent_khopesh.moonstrike_mode";

    public static MoonstrikeMode fromConfig(FileConfiguration config) {
        String value = config.getString(CONFIG_PATH, SWEEP.name());
        return value != null && WAVE.name().equals(value.trim().toUpperCase(Locale.ROOT)) ? WAVE : SWEEP;
    }
}
