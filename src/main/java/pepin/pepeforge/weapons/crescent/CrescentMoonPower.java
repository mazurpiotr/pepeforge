package pepin.pepeforge.weapons.crescent;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class CrescentMoonPower {

    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;
    private static final double DAY_DAMAGE_PENALTY = 1.0D;
    private static final double MOONLIGHT_DAMAGE_BONUS = 2.0D;

    private CrescentMoonPower() {
    }

    public static double getDamageModifier(Player player) {
        return isMoonlit(player) ? MOONLIGHT_DAMAGE_BONUS : -DAY_DAMAGE_PENALTY;
    }

    public static boolean isMoonlit(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }

        long time = world.getTime();
        if (time < NIGHT_START || time > NIGHT_END) {
            return false;
        }

        return player.getLocation().getBlock().getLightFromSky() > 0;
    }
}
