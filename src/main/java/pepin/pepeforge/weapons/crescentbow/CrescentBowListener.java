package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Color;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;

public final class CrescentBowListener implements Listener {

    private final ItemFactory itemFactory;

    public CrescentBowListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack bow = event.getBow();
        if (!itemFactory.isCrescentBow(bow)) {
            return;
        }

        Entity projectile = event.getProjectile();
        if (!(projectile instanceof AbstractArrow baseArrow)) {
            return;
        }

        spawnSideArrow(player, baseArrow, CrescentBowDefinition.SIDE_ARROW_YAW_DEGREES);
        spawnSideArrow(player, baseArrow, -CrescentBowDefinition.SIDE_ARROW_YAW_DEGREES);
        damageBow(bow, 1);
    }

    private void spawnSideArrow(Player player, AbstractArrow baseArrow, double yawDegrees) {
        Vector baseVelocity = baseArrow.getVelocity();
        Vector velocity = rotateAroundY(baseVelocity, yawDegrees);
        AbstractArrow extraArrow = launchMatchingArrow(player, baseArrow, velocity);
        extraArrow.setVelocity(velocity);
        copyArrowProperties(baseArrow, extraArrow, player);
    }

    private AbstractArrow launchMatchingArrow(Player player, AbstractArrow baseArrow, Vector velocity) {
        if (baseArrow instanceof SpectralArrow) {
            return player.launchProjectile(SpectralArrow.class, velocity);
        }
        return player.launchProjectile(Arrow.class, velocity);
    }

    private void copyArrowProperties(AbstractArrow source, AbstractArrow target, Player shooter) {
        target.setShooter(shooter);
        target.setDamage(source.getDamage());
        target.setCritical(source.isCritical());
        target.setFireTicks(source.getFireTicks());
        target.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        target.setPierceLevel(source.getPierceLevel());

        if (source instanceof Arrow sourceArrow && target instanceof Arrow targetArrow) {
            Color color = sourceArrow.getColor();
            targetArrow.setColor(color != null ? color : CrescentBowDefinition.SIDE_ARROW_COLOR);
            for (org.bukkit.potion.PotionEffect effect : sourceArrow.getCustomEffects()) {
                targetArrow.addCustomEffect(effect, true);
            }
        }

        if (source instanceof SpectralArrow && target instanceof SpectralArrow targetSpectral) {
            targetSpectral.setGlowingTicks(((SpectralArrow) source).getGlowingTicks());
        }
    }

    private Vector rotateAroundY(Vector vector, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }

    private void damageBow(ItemStack bow, int amount) {
        ItemMeta meta = bow.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int maxDamage = bow.getType().getMaxDurability();
        int nextDamage = damageable.getDamage() + amount;
        if (nextDamage >= maxDamage) {
            bow.setAmount(0);
            return;
        }

        damageable.setDamage(nextDamage);
        bow.setItemMeta(meta);
    }
}
