package pepin.pepeforge.weapons.solarshield;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SolarPower {

    private SolarPower() {
    }

    public static boolean isSunlit(Player player) {
        World world = player.getWorld();
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

        return player.getLocation().getBlock().getLightFromSky() == 15;
    }
}
