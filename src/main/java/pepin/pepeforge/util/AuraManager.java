package pepin.pepeforge.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.weapons.crescent.CrescentMoonPower;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuraManager {

    private static final long TICK_MILLIS = 50L;
    private static final long AURA_INTERVAL_TICKS = 2L;
    private static final long CRESCENT_AURA_INTERVAL_TICKS = 4L;
    private static final double CRESCENT_FORWARD_OFFSET = 0.45D;
    private static final double CRESCENT_SIDE_OFFSET = 0.18D;
    private static final double CRESCENT_VERTICAL_OFFSET = -0.35D;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final Map<UUID, Long> windAuraUntil = new ConcurrentHashMap<>();
    private ScheduledTaskCompat task;
    private long taskTick;

    public AuraManager(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void startTask() {
        if (task != null) {
            return;
        }

        task = SchedulerCompat.runTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            taskTick += AURA_INTERVAL_TICKS;
            boolean crescentPulse = taskTick % CRESCENT_AURA_INTERVAL_TICKS == 0L;
            cleanupExpired(now);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                Long auraUntil = windAuraUntil.get(player.getUniqueId());
                if ((auraUntil == null || auraUntil <= now) && !crescentPulse) {
                    continue;
                }

                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (auraUntil != null
                            && auraUntil > now
                            && itemFactory.getWindBladeTier(held) != null
                            && player.hasPotionEffect(PotionEffectType.SPEED)) {
                        playWindAura(player);
                    }

                    if (crescentPulse && isHoldingCrescentWeapon(held) && CrescentMoonPower.isMoonlit(player)) {
                        playCrescentMoonAura(player);
                    }
                });
            }
        }, 1L, AURA_INTERVAL_TICKS);
    }

    public void activateWindAura(Player player, int durationTicks) {
        windAuraUntil.put(player.getUniqueId(), System.currentTimeMillis() + (durationTicks * TICK_MILLIS));
    }

    public void clearPlayer(Player player) {
        windAuraUntil.remove(player.getUniqueId());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        windAuraUntil.clear();
        taskTick = 0L;
    }

    private void cleanupExpired(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = windAuraUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private void playWindAura(Player player) {
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
                    base.clone().add(x, y, z),
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    0.0D
            );
        }
    }

    private boolean isHoldingCrescentWeapon(ItemStack item) {
        return itemFactory.isCrescentBow(item) || itemFactory.isCrescentSpear(item);
    }

    private void playCrescentMoonAura(Player player) {
        Location hand = resolveMainHandPoint(player);
        Vector forward = horizontalForward(player);
        Vector right = new Vector(forward.getZ(), 0.0D, -forward.getX()).normalize();
        long time = player.getTicksLived();

        for (int point = 0; point < 2; point++) {
            double phase = (time * 0.22D) + (point * Math.PI);
            Location orbitPoint = hand.clone()
                    .add(right.clone().multiply(Math.cos(phase) * 0.16D))
                    .add(forward.clone().multiply(Math.sin(phase) * 0.07D))
                    .add(0.0D, Math.sin(phase + (Math.PI * 0.35D)) * 0.09D, 0.0D);

            player.getWorld().spawnParticle(
                    Particle.WAX_OFF,
                    orbitPoint,
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    0.0D
            );
        }

        if (time % 8L == 0L) {
            Location mote = hand.clone().add(0.0D, 0.08D + (Math.sin(time * 0.08D) * 0.04D), 0.0D);
            player.getWorld().spawnParticle(
                    Particle.END_ROD,
                    mote,
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.002D
            );
        }
    }

    private Location resolveMainHandPoint(Player player) {
        Vector forward = horizontalForward(player);
        Vector right = new Vector(forward.getZ(), 0.0D, -forward.getX()).normalize();

        return player.getEyeLocation()
                .add(forward.multiply(CRESCENT_FORWARD_OFFSET))
                .add(right.multiply(CRESCENT_SIDE_OFFSET))
                .add(0.0D, CRESCENT_VERTICAL_OFFSET, 0.0D);
    }

    private Vector horizontalForward(Player player) {
        Vector forward = player.getEyeLocation().getDirection().clone().setY(0.0D);
        if (forward.lengthSquared() < 1.0E-6D) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }
}
