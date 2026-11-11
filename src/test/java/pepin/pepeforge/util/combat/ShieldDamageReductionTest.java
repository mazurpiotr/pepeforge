package pepin.pepeforge.util.combat;

import com.google.common.base.Function;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
class ShieldDamageReductionTest {

    @Test
    void shieldProtectsBeforeGoldenAppleAbsorption() {
        EntityDamageEvent event = event(mock(Player.class), 5.0D, 4.0D);

        assertEquals(2.0D, ShieldDamageReduction.reduce(event, 2.0D));

        assertEquals(-2.0D, event.getDamage(DamageModifier.MAGIC));
        assertEquals(-3.0D, event.getDamage(DamageModifier.ABSORPTION));
        assertEquals(0.0D, event.getFinalDamage());
        assertFalse(event.isCancelled());
    }

    @Test
    void partialAndFullProtectionPreserveAllExistingReductions() {
        EntityDamageEvent event = event(mock(Player.class), 20.0D, 2.0D,
                Map.of(DamageModifier.ARMOR, -5.0D, DamageModifier.RESISTANCE, -3.0D,
                        DamageModifier.MAGIC, -2.0D));

        assertEquals(3.0D, ShieldDamageReduction.reduce(event, 3.0D));
        assertEquals(20.0D, event.getDamage());
        assertEquals(-5.0D, event.getDamage(DamageModifier.ARMOR));
        assertEquals(-3.0D, event.getDamage(DamageModifier.RESISTANCE));
        assertEquals(-5.0D, event.getDamage(DamageModifier.MAGIC));
        assertEquals(-2.0D, event.getDamage(DamageModifier.ABSORPTION));
        assertEquals(5.0D, event.getFinalDamage());

        assertEquals(7.0D, ShieldDamageReduction.reduce(event, 20.0D));
        assertEquals(0.0D, event.getDamage(DamageModifier.ABSORPTION), 1.0E-9D);
        assertEquals(0.0D, event.getFinalDamage());
        assertFalse(event.isCancelled());
    }

    @Test
    void unsupportedAndNonFiniteDamageAreUnchanged() {
        EntityDamageEvent unsupported = new EntityDamageEvent(mock(Player.class), DamageCause.FIRE,
                mock(DamageSource.class), 4.0D);
        assertEquals(0.0D, ShieldDamageReduction.reduce(unsupported, 2.0D));
        assertEquals(4.0D, unsupported.getFinalDamage());

        EntityDamageEvent invalid = event(mock(Player.class), Double.NaN, 0.0D);
        assertEquals(0.0D, ShieldDamageReduction.reduce(invalid, 2.0D));
        assertEquals(0.0D, ShieldDamageReduction.reduce(event(mock(Player.class), 0.0D, 0.0D), 2.0D));
        assertEquals(0.0D, ShieldDamageReduction.reduce(event(mock(Player.class), 4.0D, 0.0D), Double.NaN));
    }

    @Test
    void shieldRunsAfterPrimaryDamageRecalculatesVanillaModifiers() {
        Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
        modifiers.put(DamageModifier.BASE, 5.0D);
        modifiers.put(DamageModifier.MAGIC, 0.0D);
        modifiers.put(DamageModifier.ABSORPTION, -4.0D);
        Map<DamageModifier, Function<Double, Double>> functions = new EnumMap<>(DamageModifier.class);
        functions.put(DamageModifier.BASE, ignored -> 0.0D);
        functions.put(DamageModifier.MAGIC, ignored -> 0.0D);
        functions.put(DamageModifier.ABSORPTION, damage -> -Math.min(4.0D, damage));
        EntityDamageEvent event = new EntityDamageEvent(mock(Player.class), DamageCause.ENTITY_ATTACK,
                mock(DamageSource.class), modifiers, functions);

        event.setDamage(7.0D);
        assertEquals(2.0D, ShieldDamageReduction.reduce(event, 2.0D));
        assertEquals(-2.0D, event.getDamage(DamageModifier.MAGIC));
        assertEquals(-4.0D, event.getDamage(DamageModifier.ABSORPTION));
        assertEquals(1.0D, event.getFinalDamage());
    }

    @Test
    void overflowingMagicReductionDoesNotMutateEvent() {
        EntityDamageEvent event = event(mock(Player.class), Double.MAX_VALUE, 0.0D,
                Map.of(DamageModifier.MAGIC, -Double.MAX_VALUE, DamageModifier.ARMOR, Double.MAX_VALUE));
        assertEquals(0.0D, ShieldDamageReduction.reduce(event, Double.MAX_VALUE));
        assertEquals(-Double.MAX_VALUE, event.getDamage(DamageModifier.MAGIC));
    }

    static EntityDamageEvent event(Player player, double damage, double absorption) {
        return event(player, damage, absorption, Map.of());
    }

    static EntityDamageEvent event(Player player, double damage, double absorption,
            Map<DamageModifier, Double> reductions) {
        Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
        modifiers.put(DamageModifier.BASE, damage);
        modifiers.put(DamageModifier.MAGIC, 0.0D);
        modifiers.putAll(reductions);
        double afterReductions = modifiers.values().stream().mapToDouble(Double::doubleValue).sum();
        modifiers.put(DamageModifier.ABSORPTION, -Math.min(absorption, Math.max(0.0D, afterReductions)));
        Map<DamageModifier, Function<Double, Double>> functions = new EnumMap<>(DamageModifier.class);
        modifiers.forEach((modifier, value) -> functions.put(modifier, ignored -> value));
        return new EntityDamageEvent(player, DamageCause.ENTITY_ATTACK, mock(DamageSource.class), modifiers, functions);
    }
}
