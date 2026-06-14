package pepin.pepeforge.weapons.windblade;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.aura.TimedAuraEffect;

public final class WindAuraEffect implements TimedAuraEffect {

    private static final long TICK_MILLIS = 50L;
    private final ItemFactory itemFactory;
    private long expiresAtMillis;

    public WindAuraEffect(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @Override
    public int getTickInterval() {
        return 2;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMillis;
    }

    @Override
    public void extendDuration(int ticks) {
        long targetMillis = System.currentTimeMillis() + (ticks * TICK_MILLIS);
        if (targetMillis > this.expiresAtMillis) {
            this.expiresAtMillis = targetMillis;
        }
    }

    @Override
    public void tick(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
            return;
        }

        if (itemFactory.getWindBladeTier(player.getInventory().getItemInMainHand()) == null) {
            return;
        }

        Location base = player.getLocation();
        long time = player.getTicksLived();

        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.4D + (ring * 0.2D);
            double speed = 1.45D - (ring * 0.25D);
            double angle = (time * 0.18D * speed) + (ring * Math.PI * 0.65D);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = 0.08D + (ring * 0.13D) + (Math.sin((time * 0.12D) + ring) * 0.05D);

            player.getWorld().spawnParticle(
                    Particle.WHITE_ASH,
                    base.getX() + x, base.getY() + y, base.getZ() + z,
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    0.0D
            );
        }
    }
}
