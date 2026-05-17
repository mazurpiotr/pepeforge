package pepin.pepeforge.weapons.windblade;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pepin.pepeforge.item.ItemFactory;

public final class WindBladeListener implements Listener {

    private static final int HOLDING_SPEED_DURATION_TICKS = 40;
    private static final PotionEffect HOLDING_SPEED_EFFECT = new PotionEffect(
            PotionEffectType.SPEED,
            HOLDING_SPEED_DURATION_TICKS,
            0,
            true,
            false,
            true
    );

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public WindBladeListener(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void startHoldingTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                ItemStack held = player.getInventory().getItemInMainHand();
                WindBladeTier tier = itemFactory.getWindBladeTier(held);
                if (tier == null || !tier.grantsHoldingSpeed()) {
                    continue;
                }
                applySpeedEffectIfBetter(player, HOLDING_SPEED_EFFECT);
            }
        }, 1L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Entity target = event.getEntity();
        if (target == player) {
            return;
        }

        WindBladeTier tier = itemFactory.getWindBladeTier(player.getInventory().getItemInMainHand());
        if (tier == null || tier.hitSpeedDurationTicks() <= 0) {
            return;
        }

        applySpeedEffectIfBetter(player, new PotionEffect(
                PotionEffectType.SPEED,
                tier.hitSpeedDurationTicks(),
                tier.hitSpeedAmplifier(),
                true,
                false,
                true
        ));
    }

    private static void applySpeedEffectIfBetter(Player player, PotionEffect candidate) {
        // Wind Blade speed should feel persistent without stomping on stronger
        // potion effects or repeatedly refreshing an equal-or-better buff.
        PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
        if (current != null) {
            if (current.getAmplifier() > candidate.getAmplifier()) {
                return;
            }
            if (current.getAmplifier() == candidate.getAmplifier()
                    && current.getDuration() >= candidate.getDuration()) {
                return;
            }
        }
        player.addPotionEffect(candidate);
    }
}
