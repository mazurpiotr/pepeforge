package pepin.pepeforge.weapons.rapier;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.rapier.compat.PaperRapierAttackCharge;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RapierAttackChargeTest {
    @Test
    void snapshotsBeforeNativeResetAndConsumesOnlyOnce() {
        try (Fixture fixture = new Fixture()) {
            fixture.charge.onPreAttack(fixture.event());
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.95F, fixture.charge.consume(fixture.player, fixture.target));
            assertEquals(0.05F, fixture.charge.consume(fixture.player, fixture.target));
        }
    }

    @Test
    void mismatchedTargetLaterTickAndCancelledAttackCannotReuseSnapshot() {
        try (Fixture fixture = new Fixture()) {
            fixture.charge.onPreAttack(fixture.event());
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.05F, fixture.charge.consume(fixture.player, mock(LivingEntity.class)));
            when(fixture.player.getAttackCooldown()).thenReturn(0.95F);
            fixture.charge.onPreAttack(fixture.event());
            when(fixture.player.getTicksLived()).thenReturn(11);
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.05F, fixture.charge.consume(fixture.player, fixture.target));
            when(fixture.player.getAttackCooldown()).thenReturn(0.95F);
            fixture.charge.onPreAttack(fixture.event());
            PrePlayerAttackEntityEvent cancelled = fixture.event();
            cancelled.setCancelled(true);
            fixture.charge.onPreAttack(cancelled);
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.05F, fixture.charge.consume(fixture.player, fixture.target));
        }
    }

    @Test
    void clearAndStopDiscardSnapshotsAndForeignRegionCallbacksDoNotReadPlayerState() {
        try (Fixture fixture = new Fixture()) {
            fixture.charge.onPreAttack(fixture.event());
            fixture.charge.clear(fixture.player);
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.05F, fixture.charge.consume(fixture.player, fixture.target));
            when(fixture.player.getAttackCooldown()).thenReturn(0.95F);
            fixture.charge.onPreAttack(fixture.event());
            fixture.charge.stop();
            when(fixture.player.getAttackCooldown()).thenReturn(0.05F);
            assertEquals(0.05F, fixture.charge.consume(fixture.player, fixture.target));
            clearInvocations(fixture.player);
            fixture.charge.onPreAttack(fixture.event());
            verifyNoInteractions(fixture.player);
        }
        try (Fixture fixture = new Fixture()) {
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(false);
            clearInvocations(fixture.player);
            fixture.charge.onPreAttack(fixture.event());
            verifyNoInteractions(fixture.player);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Player player = mock(Player.class);
        private final LivingEntity target = mock(LivingEntity.class);
        private final PaperRapierAttackCharge charge = new PaperRapierAttackCharge();
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);

        private Fixture() {
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getTicksLived()).thenReturn(10);
            when(player.getAttackCooldown()).thenReturn(0.95F);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);
        }

        private PrePlayerAttackEntityEvent event() {
            return new PrePlayerAttackEntityEvent(player, target, true);
        }

        @Override
        public void close() {
            charge.stop();
            scheduler.close();
        }
    }
}
