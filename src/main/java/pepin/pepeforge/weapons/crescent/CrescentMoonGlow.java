package pepin.pepeforge.weapons.crescent;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;

public final class CrescentMoonGlow {

    private static final long GLOW_INTERVAL_TICKS = 10L;
    private static final double FORWARD_OFFSET = 0.45D;
    private static final double SIDE_OFFSET = 0.18D;
    private static final double VERTICAL_OFFSET = -0.35D;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public CrescentMoonGlow(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void startTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!CrescentMoonPower.isMoonlit(player)) {
                    continue;
                }

                ItemStack held = player.getInventory().getItemInMainHand();
                if (!itemFactory.isCrescentBow(held) && !itemFactory.isCrescentSpear(held)) {
                    continue;
                }

                spawnGlow(player);
            }
        }, 1L, GLOW_INTERVAL_TICKS);
    }

    private void spawnGlow(Player player) {
        Vector forward = player.getEyeLocation().getDirection().clone().setY(0.0D);
        if (forward.lengthSquared() < 1.0E-6D) {
            forward = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            forward.normalize();
        }

        Vector right = new Vector(forward.getZ(), 0.0D, -forward.getX()).normalize();
        Location point = player.getEyeLocation()
                .add(forward.multiply(FORWARD_OFFSET))
                .add(right.multiply(SIDE_OFFSET))
                .add(0.0D, VERTICAL_OFFSET, 0.0D);

        player.getWorld().spawnParticle(
                Particle.WAX_OFF,
                point,
                2,
                0.02D, 0.02D, 0.02D,
                0.001D
        );
    }
}
