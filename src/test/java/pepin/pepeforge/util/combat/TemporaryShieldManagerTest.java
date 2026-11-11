package pepin.pepeforge.util.combat;

import org.bukkit.boss.BarColor;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.BossBarManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pepin.pepeforge.util.combat.ShieldDamageReductionTest.event;

@SuppressWarnings("deprecation")
class TemporaryShieldManagerTest {

    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private final Player player = mock(Player.class);
    private final PluginLang lang = mock(PluginLang.class);
    private final BossBarManager bars = mock(BossBarManager.class);
    private final ScheduledTaskCompat task = mock(ScheduledTaskCompat.class);
    private final AtomicBoolean owned = new AtomicBoolean(true);
    private final List<Runnable> timers = new ArrayList<>();
    private final List<Runnable> retired = new ArrayList<>();
    private MockedStatic<SchedulerCompat> scheduler;
    private TemporaryShieldManager manager;

    @BeforeEach
    void setup() {
        when(plugin.isEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(lang.text("messages.temporary_shield.bar"))
                .thenReturn("{name} · {current} / {limit} HP · {seconds} s");
        when(lang.text(startsWith("messages.temporary_shield.sources."))).thenAnswer(call -> {
            String path = call.getArgument(0);
            return path.substring(path.lastIndexOf('.') + 1);
        });
        scheduler = mockStatic(SchedulerCompat.class);
        scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class)))
                .thenAnswer(call -> owned.get());
        scheduler.when(() -> SchedulerCompat.runTimerForEntity(eq(player), eq(plugin), any(Runnable.class),
                any(Runnable.class), eq(1L), eq(1L))).thenAnswer(call -> {
                    timers.add(call.getArgument(2));
                    retired.add(call.getArgument(3));
                    return task;
                });
        manager = new TemporaryShieldManager(plugin, lang, bars);
    }

    @AfterEach
    void release() {
        manager.stop();
        scheduler.close();
    }

    @Test
    void repeatedGrantsCapWithoutChangingNativeAbsorption() {
        when(player.getAbsorptionAmount()).thenReturn(4.0D);
        for (int hit = 0; hit < 12; hit++) {
            manager.grant(player, "moonshield", 0.5D, 4.0D, 80);
        }
        assertEquals(1, timers.size());
        assertEquals("moonshield · 4 / 4 HP · 4.0 s", latestTitle());
        assertRemaining(4.0D);
        verify(player, never()).setAbsorptionAmount(anyDouble());
        verify(player, never()).setHealth(anyDouble());
        verify(player, never()).setNoDamageTicks(anyInt());
        verify(player, never()).setVelocity(any());
    }

    @Test
    void expiresExactlyAtEightyTicksAndUpdatesTimeEveryTenTicks() {
        grant(2.0D);
        ticks(70);
        assertEquals("moonshield · 2 / 4 HP · 0.5 s", latestTitle());
        ticks(9);
        verify(bars, never()).removeBar(eq(player), anyString());
        ticks(1);
        verify(bars).removeBar(eq(player), anyString());
        verify(task).cancel();
        assertRemaining(0.0D);
    }

    @Test
    void grantAtCapRefreshesWholePool() {
        grant(4.0D);
        ticks(79);
        grant(0.5D);
        ticks(79);
        verify(bars, never()).removeBar(eq(player), anyString());
        assertEquals("moonshield · 4 / 4 HP · 0.5 s", latestTitle());
        ticks(1);
        assertRemaining(0.0D);
    }

    @Test
    void acceptedDamageSpendsBeforeGoldenAppleAndUpdatesBar() {
        grant(4.0D);
        EntityDamageEvent hit = event(player, 2.0D, 4.0D);
        manager.protect(hit);
        assertEquals("moonshield · 4 / 4 HP · 4.0 s", latestTitle());
        manager.finishDamage(hit);
        assertEquals("moonshield · 2 / 4 HP · 4.0 s", latestTitle());
        assertEquals(0.0D, hit.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION), 1.0E-9D);
        assertFalse(hit.isCancelled());
        assertRemaining(2.0D);
    }

    @Test
    void lateCancellationReleasesReservation() {
        grant(2.0D);
        EntityDamageEvent hit = event(player, 5.0D, 0.0D);
        manager.protect(hit);
        hit.setCancelled(true);
        manager.finishDamage(hit);
        assertRemaining(2.0D);
    }

    @Test
    void nestedHitsCannotSpendReservedHpTwice() {
        grant(4.0D);
        EntityDamageEvent outer = event(player, 3.0D, 0.0D);
        manager.protect(outer);
        EntityDamageEvent inner = event(player, 3.0D, 0.0D);
        manager.protect(inner);
        manager.finishDamage(inner);
        assertEquals(2.0D, inner.getFinalDamage());
        outer.setCancelled(true);
        manager.finishDamage(outer);
        assertRemaining(3.0D);
    }

    @Test
    void replenishmentWhileDamageIsReservedRemainsCapped() {
        grant(4.0D);
        EntityDamageEvent hit = event(player, 3.0D, 0.0D);
        manager.protect(hit);
        grant(0.5D);
        manager.finishDamage(hit);
        assertRemaining(1.0D);
    }

    @Test
    void expiryWhileDamageIsPendingCannotRestoreOldPool() {
        grant(2.0D);
        EntityDamageEvent hit = event(player, 1.0D, 0.0D);
        manager.protect(hit);
        ticks(80);
        grant(0.5D);
        hit.setCancelled(true);
        manager.finishDamage(hit);
        assertRemaining(0.5D);
    }

    @Test
    void clearingAndReplacingSourceWhilePendingDoesNotConsumeReplacement() {
        grant(2.0D);
        manager.grant(player, "other", 0.5D, 4.0D, 160);
        EntityDamageEvent hit = event(player, 1.0D, 0.0D);
        manager.protect(hit);
        manager.clearSource(player, "moonshield");
        grant(0.5D);
        manager.finishDamage(hit);
        assertRemaining(1.0D);
    }

    @Test
    void consumesEarliestExpiryThenSourceId() {
        manager.grant(player, "later", 2.0D, 4.0D, 100);
        manager.grant(player, "beta", 1.0D, 4.0D, 80);
        manager.grant(player, "alpha", 1.0D, 4.0D, 80);
        EntityDamageEvent hit = event(player, 1.5D, 0.0D);
        manager.protect(hit);
        manager.finishDamage(hit);
        assertEquals("beta · 0.5 / 4 HP · 4.0 s", latestTitle());
        manager.clearSource(player, "beta");
        assertRemaining(2.0D);
    }

    @Test
    void zeroAndUnsupportedDamageDoNotSpendShield() {
        grant(2.0D);
        EntityDamageEvent zero = event(player, 0.0D, 0.0D);
        manager.protect(zero);
        manager.finishDamage(zero);
        EntityDamageEvent cancelled = event(player, 3.0D, 0.0D);
        cancelled.setCancelled(true);
        manager.protect(cancelled);
        manager.finishDamage(cancelled);
        assertEquals(3.0D, cancelled.getFinalDamage());
        EntityDamageEvent unsupported = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.FIRE,
                mock(DamageSource.class), 3.0D);
        manager.protect(unsupported);
        manager.finishDamage(unsupported);
        assertEquals(3.0D, unsupported.getFinalDamage());
        assertRemaining(2.0D);
    }

    @Test
    void primaryDamageReducedToZeroDoesNotSpendShield() {
        grant(2.0D);
        EntityDamageEvent hit = event(player, 5.0D, 4.0D);
        hit.setDamage(0.0D);
        manager.protect(hit);
        manager.finishDamage(hit);
        assertRemaining(2.0D);
    }

    @Test
    void retirementDuringSchedulingCannotLeakTheReturnedTask() {
        scheduler.when(() -> SchedulerCompat.runTimerForEntity(eq(player), eq(plugin), any(Runnable.class),
                any(Runnable.class), eq(1L), eq(1L))).thenAnswer(call -> {
                    Runnable callback = call.getArgument(3);
                    callback.run();
                    return task;
                });
        grant(2.0D);
        verify(task).cancel();
        assertRemaining(0.0D);
    }

    @Test
    void excludesUnavoidableDeathCausesButProtectsEnvironmentalDamage() {
        grant(2.0D);
        for (EntityDamageEvent.DamageCause cause : List.of(EntityDamageEvent.DamageCause.VOID,
                EntityDamageEvent.DamageCause.KILL, EntityDamageEvent.DamageCause.SUICIDE)) {
            EntityDamageEvent hit = spy(event(player, 3.0D, 0.0D));
            when(hit.getCause()).thenReturn(cause);
            manager.protect(hit);
            manager.finishDamage(hit);
            assertEquals(3.0D, hit.getFinalDamage());
        }
        EntityDamageEvent fire = spy(event(player, 3.0D, 0.0D));
        when(fire.getCause()).thenReturn(EntityDamageEvent.DamageCause.FIRE);
        manager.protect(fire);
        manager.finishDamage(fire);
        assertEquals(1.0D, fire.getFinalDamage());
    }

    @Test
    void sourceCleanupPreservesOtherSourceAndPlayerTimer() {
        grant(2.0D);
        manager.grant(player, "other", 0.5D, 4.0D, 80);
        manager.clearSource("moonshield");
        verify(task, never()).cancel();
        assertRemaining(0.5D);
        verify(task).cancel();
    }

    @Test
    void retirementCancelsTimerAndClearsShield() {
        grant(2.0D);
        retired.getFirst().run();
        verify(task).cancel();
        assertRemaining(0.0D);
    }

    @Test
    void stopRejectsFutureGrantsAndStaleRetirementCannotRemoveReplacementBar() {
        grant(2.0D);
        ArgumentCaptor<String> previousId = ArgumentCaptor.forClass(String.class);
        verify(bars).updateBar(eq(player), previousId.capture(), anyString(), anyDouble(), eq(BarColor.BLUE));
        owned.set(false);
        manager.stop();
        verify(bars).removeBar(player.getUniqueId(), previousId.getValue());
        owned.set(true);
        manager.grant(player, "moonshield", 1.0D, 4.0D, 80);
        assertEquals(1, timers.size());
        TemporaryShieldManager next = new TemporaryShieldManager(plugin, lang, bars);
        next.grant(player, "moonshield", 1.0D, 4.0D, 80);
        retired.getFirst().run();
        ArgumentCaptor<String> allIds = ArgumentCaptor.forClass(String.class);
        verify(bars, times(2)).updateBar(eq(player), allIds.capture(), anyString(), anyDouble(), eq(BarColor.BLUE));
        assertNotEquals(allIds.getAllValues().get(0), allIds.getAllValues().get(1));
        verify(bars, never()).removeBar(player, allIds.getAllValues().get(1));
        verify(bars, never()).removeBar(player.getUniqueId(), allIds.getAllValues().get(1));
        next.stop();
    }

    @Test
    void quitDeathAndWorldChangeClearState() {
        grant(2.0D);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        manager.onQuit(quit);
        assertRemaining(0.0D);
        grant(2.0D);
        PlayerDeathEvent death = mock(PlayerDeathEvent.class);
        when(death.getEntity()).thenReturn(player);
        manager.onDeath(death);
        assertRemaining(0.0D);
        grant(2.0D);
        PlayerChangedWorldEvent world = mock(PlayerChangedWorldEvent.class);
        when(world.getPlayer()).thenReturn(player);
        manager.onWorldChange(world);
        assertRemaining(0.0D);
        verify(task, times(3)).cancel();
    }

    @Test
    void foreignRegionAndInvalidGrantsNeverStartTimer() {
        owned.set(false);
        grant(2.0D);
        owned.set(true);
        manager.grant(player, "moonshield", Double.NaN, 4.0D, 80);
        manager.grant(player, "moonshield", 1.0D, 0.0D, 80);
        manager.grant(player, "moonshield", 1.0D, 4.0D, 0);
        assertTrue(timers.isEmpty());
        verifyNoInteractions(bars);
    }

    @Test
    void disableOutsideOwnedRegionClearsBarWithoutScheduling() {
        grant(2.0D);
        UUID playerId = player.getUniqueId();
        owned.set(false);
        when(plugin.isEnabled()).thenReturn(false);

        manager.stop();

        verify(bars).removeBar(eq(playerId), anyString());
        verify(task).cancel();
        scheduler.verify(() -> SchedulerCompat.runForPlayer(eq(player), eq(plugin), any(Runnable.class)), never());
        assertEquals(1, timers.size());
        manager.grant(player, "moonshield", 1.0D, 4.0D, 80);
        assertEquals(1, timers.size());
    }

    @Test
    void serverShutdownClearsBarWithoutScheduling() {
        grant(2.0D);
        UUID playerId = player.getUniqueId();
        owned.set(false);
        scheduler.when(SchedulerCompat::isServerStopping).thenReturn(true);

        manager.stop();

        verify(bars).removeBar(eq(playerId), anyString());
        verify(task).cancel();
        scheduler.verify(() -> SchedulerCompat.runForPlayer(eq(player), eq(plugin), any(Runnable.class)), never());
    }

    private void grant(double amount) {
        manager.grant(player, "moonshield", amount, 4.0D, 80);
    }

    private void ticks(int count) {
        for (int tick = 0; tick < count; tick++) {
            timers.getLast().run();
        }
    }

    private void assertRemaining(double amount) {
        EntityDamageEvent hit = event(player, 100.0D, 0.0D);
        manager.protect(hit);
        manager.finishDamage(hit);
        assertEquals(100.0D - amount, hit.getFinalDamage(), 1.0E-9D);
    }

    private String latestTitle() {
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        verify(bars, atLeastOnce()).updateBar(eq(player), anyString(), title.capture(), anyDouble(), eq(BarColor.BLUE));
        return title.getAllValues().getLast();
    }
}
