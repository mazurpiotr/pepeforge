package pepin.pepeforge.resourcepack;

import java.util.Locale;

public enum ResourcePackMode {
    MANAGER,
    GITHUB;

    public static ResourcePackMode parse(String value) {
        if (value == null) {
            return MANAGER;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return MANAGER;
        }
    }
}
