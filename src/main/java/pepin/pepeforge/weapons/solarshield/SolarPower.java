package pepin.pepeforge.weapons.solarshield;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SolarPower {

    private SolarPower() {
    }

    public static boolean isSunlit(org.bukkit.Location location) {
        World world = location.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }

        long time = world.getTime();
        // Day time is generally 0 to 13000
        if (time >= 13000L && time <= 23000L) {
            return false;
        }

        if (world.hasStorm()) {
            return false;
        }

        return location.getBlock().getLightFromSky() == 15;
    }

    public static boolean isSunlit(Player player) {
        return isSunlit(player.getLocation());
    }
}
