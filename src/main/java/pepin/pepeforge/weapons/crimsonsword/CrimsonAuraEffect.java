package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pepin.pepeforge.util.aura.TimedAuraEffect;

import java.util.UUID;

public final class CrimsonAuraEffect implements TimedAuraEffect {

    private static final long TICK_MILLIS = 50L;

    private final CrimsonSwordListener listener;
    private final int level;
    private long expiresAtMillis;
    private long lastDrainTick;

    public CrimsonAuraEffect(CrimsonSwordListener listener, int level, int durationTicks, long currentTick) {
        this.listener = listener;
        this.level = level;
        this.expiresAtMillis = System.currentTimeMillis() + (durationTicks * TICK_MILLIS);
        this.lastDrainTick = currentTick;
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
        if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        if (!listener.isHoldingCrimsonSword(player)) {
            return;
        }

        playCrimsonAura(player);

        long currentTick = player.getTicksLived();
        if (currentTick - lastDrainTick >= CrimsonSwordDefinition.AURA_DRAIN_INTERVAL_TICKS) {
            drainCrimsonAura(player);
            lastDrainTick = currentTick;
        }
    }

    private void drainCrimsonAura(Player player) {
        double drainAmount = listener.getManager().auraDrainAmount(level);
        double totalDrained = 0.0D;
        Location base = player.getLocation();
        double radius = CrimsonSwordDefinition.AURA_RADIUS;
        UUID playerId = player.getUniqueId();

        listener.setAuraDraining(playerId, true);
        try {
            for (Entity entity : player.getWorld().getNearbyEntities(base, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target) || target == player || target.isDead() || !target.isValid()) {
                    continue;
                }

                double beforeHealth = target.getHealth();
                target.damage(drainAmount, player);
                double drained = Math.max(0.0D, beforeHealth - Math.max(0.0D, target.getHealth()));
                if (drained <= 0.0D) {
                    continue;
                }

                totalDrained += drained;
                playDrainEffects(player, target);
            }
        } finally {
            listener.setAuraDraining(playerId, false);
        }

        listener.getManager().heal(player, totalDrained);
    }



    private void playCrimsonAura(Player player) {
        Location base = player.getLocation();
        long time = player.getTicksLived();
        for (int i = 0; i < 5; i++) {
            double angle = (time * 0.22D) + (i * 1.35D);
            double radius = 0.35D + ((time + i) % 7) * 0.035D;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle * 0.85D) * radius;
            double y = 0.15D + (i * 0.18D) + Math.sin((time + i) * 0.2D) * 0.08D;

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    base.getX() + x, base.getY() + y, base.getZ() + z,
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    new Particle.DustOptions(Color.fromRGB(185, 8, 24), 0.8f)
            );
            player.getWorld().spawnParticle(
                    Particle.ENTITY_EFFECT,
                    base.getX() + (x * 0.55D), base.getY() + y + 0.05D, base.getZ() + (z * 0.55D),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    Color.fromRGB(105, 0, 12)
            );
        }
    }

    private void playDrainEffects(Player player, LivingEntity target) {
        Location targetLoc = target.getLocation();
        target.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR,
                targetLoc.getX(), targetLoc.getY() + 0.9D, targetLoc.getZ(),
                2,
                0.18D,
                0.18D,
                0.18D,
                0.02D
        );
        Location playerLoc = player.getLocation();
        player.getWorld().spawnParticle(
                Particle.DUST,
                playerLoc.getX(), playerLoc.getY() + 1.0D, playerLoc.getZ(),
                4,
                0.28D,
                0.25D,
                0.28D,
                new Particle.DustOptions(Color.fromRGB(155, 0, 20), 0.75f)
        );
    }
}
