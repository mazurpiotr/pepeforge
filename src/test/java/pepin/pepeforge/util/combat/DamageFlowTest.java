package pepin.pepeforge.util.combat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

class DamageFlowTest {
    private final Player source = mock(Player.class);
    private final DamageSource damageSource = mock(DamageSource.class);
    private final LivingEntity target = target();

    @Test
    void cancelledHitReturnsNoHealthLossAndRestoresInvulnerability() {
        var dispatcher = new DamageEvents(new DamageFlow(), new Canceller());
        doAnswer(call -> {
            dispatcher.dispatch(event(target));
            return null;
        }).when(target).damage(4.0D, source);
        when(target.getNoDamageTicks()).thenReturn(12);

        assertEquals(new DamageFlow.Result(false, 0), DamageFlow.damage(target, 4, source, true));
        verify(target).setNoDamageTicks(0);
        verify(target).setNoDamageTicks(12);
    }

    @Test
    void successfulHitReportsActualHealthLossAndClearsItsContext() {
        var flow = new DamageFlow();
        var captured = new AtomicReference<EntityDamageByEntityEvent>();
        doAnswer(call -> {
            var event = event(target);
            captured.set(event);
            flow.capture(event);
            assertTrue(DamageFlow.isSecondaryDamage(event));
            when(target.getHealth()).thenReturn(17.0D);
            return null;
        }).when(target).damage(4.0D, source);

        assertEquals(new DamageFlow.Result(true, 3), DamageFlow.damage(target, 4, source));
        assertFalse(DamageFlow.isSecondaryDamage(captured.get()));
    }

    @Test
    void noEventIsNotASuccessfulHit() {
        assertFalse(DamageFlow.damage(target, 4, source).accepted());
    }

    @Test
    void foreignRegionTargetIsRejectedBeforeReadingItsState() {
        try (var scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(target)).thenReturn(false);
            assertFalse(DamageFlow.damage(target, 4, source).accepted());
            verifyNoInteractions(target, source);
        }
    }

    @Test
    void foreignRegionSourceCannotEnterSynchronousDamageListeners() {
        try (var scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(target)).thenReturn(true);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(source)).thenReturn(false);
            assertFalse(DamageFlow.damage(target, 4, source).accepted());
            verifyNoInteractions(target, source);
        }
    }

    @Test
    void teleportDuringDamageCannotCauseAnOffRegionHealthRead() {
        try (var scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(target)).thenReturn(true, false);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(source)).thenReturn(true);
            doAnswer(call -> {
                new DamageFlow().capture(event(target));
                return null;
            }).when(target).damage(4.0D, source);

            assertEquals(new DamageFlow.Result(true, 0), DamageFlow.damage(target, 4, source));
            verify(target, times(1)).getHealth();
        }
    }

    @Test
    void aNestedAttemptDoesNotReplaceTheOuterOutcome() {
        var flow = new DamageFlow();
        var inner = target();
        doAnswer(call -> {
            var event = event(inner);
            flow.capture(event);
            event.setCancelled(true);
            return null;
        }).when(inner).damage(2.0D, source);
        doAnswer(call -> {
            var outer = event(target);
            flow.capture(outer);
            assertFalse(DamageFlow.damage(inner, 2, source).accepted());
            assertTrue(DamageFlow.isSecondaryDamage(outer));
            return null;
        }).when(target).damage(4.0D, source);

        assertTrue(DamageFlow.damage(target, 4, source).accepted());
    }

    @Test
    void damageFailurePropagatesAndDoesNotLeakTheContext() {
        var flow = new DamageFlow();
        var event = event(target);
        doAnswer(call -> {
            flow.capture(event);
            throw new IllegalStateException("Damage failed");
        }).when(target).damage(4.0D, source);

        assertThrows(IllegalStateException.class, () -> DamageFlow.damage(target, 4, source));
        assertFalse(DamageFlow.isSecondaryDamage(event));
    }

    private LivingEntity target() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.isValid()).thenReturn(true);
        when(entity.getHealth()).thenReturn(20.0D);
        return entity;
    }

private EntityDamageByEntityEvent event(LivingEntity victim) {
    var event = mock(EntityDamageByEntityEvent.class);
    var cancelled = new AtomicBoolean(false);

    when(event.getDamager()).thenReturn(source);
    when(event.getEntity()).thenReturn(victim);
    when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
    when(event.getDamageSource()).thenReturn(damageSource);
    when(event.getDamage()).thenReturn(4.0D);
    when(event.getFinalDamage()).thenReturn(4.0D);

    when(event.isCancelled()).thenAnswer(call -> cancelled.get());
    doAnswer(call -> {
        cancelled.set(call.getArgument(0));
        return null;
    }).when(event).setCancelled(anyBoolean());

    return event;
}

    public static final class Canceller implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void cancel(EntityDamageByEntityEvent event) {
            event.setCancelled(true);
        }
    }
}
