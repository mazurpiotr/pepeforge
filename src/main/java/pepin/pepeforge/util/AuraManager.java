package pepin.pepeforge.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import pepin.pepeforge.item.ItemFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuraManager {

    private static final long TICK_MILLIS = 50L;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final Map<UUID, Long> windAuraUntil = new ConcurrentHashMap<>();
    private ScheduledTaskCompat task;

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
            cleanupExpired(now);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                Long auraUntil = windAuraUntil.get(player.getUniqueId());
                if (auraUntil == null || auraUntil <= now) {
                    continue;
                }

                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (itemFactory.getWindBladeTier(held) == null || !player.hasPotionEffect(PotionEffectType.SPEED)) {
                        return;
                    }

                    playWindAura(player);
                });
            }
        }, 1L, 2L);
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
}