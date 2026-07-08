package pepin.pepeforge.weapons.solarshield;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SolarPower {

    private SolarPower() {
    }

    public static double getSolarIntensity(org.bukkit.Location location) {
        World world = location.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return 0.0;
        }

        long time = world.getTime();
        // Day time is generally 0 to 13000
        if (time >= 13000L && time <= 23000L) {
            return 0.0;
        }

        if (world.hasStorm()) {
            return 0.0;
        }

        int skyLight = location.getBlock().getLightFromSky();
        if (skyLight >= 15) {
            return 1.0;
        } else if (skyLight == 14) {
            return 0.5;
        } else if (skyLight == 13) {
            return 0.2;
        } else {
            return 0.0;
        }
    }

    public static double getSolarIntensity(Player player) {
        return getSolarIntensity(player.getLocation());
    }

    @Deprecated
    public static boolean isSunlit(org.bukkit.Location location) {
        return getSolarIntensity(location) > 0.0;
    }

    @Deprecated
    public static boolean isSunlit(Player player) {
        return getSolarIntensity(player) > 0.0;
    }
}
