package pepin.pepeforge.util.combat;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

@SuppressWarnings("deprecation")
final class ShieldDamageReduction {

    private ShieldDamageReduction() {
    }

    static double damageBeforeAbsorption(EntityDamageEvent event) {
        if (!event.isApplicable(DamageModifier.MAGIC) || !event.isApplicable(DamageModifier.ABSORPTION)) {
            return 0.0D;
        }
        double magic = event.getDamage(DamageModifier.MAGIC);
        double absorption = event.getDamage(DamageModifier.ABSORPTION);
        double damage = event.getFinalDamage() - absorption;
        return Double.isFinite(magic) && Double.isFinite(absorption) && absorption <= 0.0D
                && Double.isFinite(damage) ? Math.max(0.0D, damage) : 0.0D;
    }

    static double reduce(EntityDamageEvent event, double availableShield) {
        if (!Double.isFinite(availableShield) || availableShield <= 0.0D) {
            return 0.0D;
        }
        double damage = damageBeforeAbsorption(event);
        double absorbed = Math.min(damage, availableShield);
        if (absorbed <= 0.0D) {
            return 0.0D;
        }
        double nativeAbsorption = Math.max(0.0D, -event.getDamage(DamageModifier.ABSORPTION));
        double magic = event.getDamage(DamageModifier.MAGIC) - absorbed;
        if (!Double.isFinite(magic)) {
            return 0.0D;
        }
        event.setDamage(DamageModifier.MAGIC, magic);
        event.setDamage(DamageModifier.ABSORPTION, -Math.min(nativeAbsorption, damage - absorbed));
        return absorbed;
    }
}
