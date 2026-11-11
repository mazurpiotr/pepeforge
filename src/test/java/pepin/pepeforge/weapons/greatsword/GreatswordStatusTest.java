package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.ActionBarHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GreatswordStatusTest {

    @BeforeAll
    static void initializeServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void releaseServer() {
        MockBukkit.unmock();
    }

    @Test
    void idleEquipmentDoesNotStartATimer() {
        try (Fixture fixture = new Fixture()) {
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.listener.refreshEquipment(fixture.player, false);

            assertTrue(fixture.tasks.isEmpty());
            fixture.scheduler.verify(() -> SchedulerCompat.runTimer(any(), any(), anyLong(), anyLong()), never());
        }
    }

    @Test
    void equipmentRefreshKeepsExistingReachAndRemovesOnlyItsOwnModifierOnUnequip() {
        try (Fixture fixture = new Fixture()) {
            AttributeInstance reach = mock(AttributeInstance.class);
            AttributeModifier own = mock(AttributeModifier.class);
            AttributeModifier foreign = mock(AttributeModifier.class);
            when(own.getKey()).thenReturn(new NamespacedKey("pepeforge", "greatsword_reach_bonus"));
            when(own.getAmount()).thenReturn(0.45D);
            when(foreign.getKey()).thenReturn(new NamespacedKey("other", "reach"));
            when(reach.getModifiers()).thenReturn(Set.of(own, foreign));
            when(fixture.player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)).thenReturn(reach);

            fixture.listener.refreshEquipment(fixture.player, true);
            verify(reach, never()).removeModifier(any(AttributeModifier.class));
            assertTrue(fixture.tasks.isEmpty());

            when(fixture.inventory.getItemInMainHand()).thenReturn(null);
            fixture.listener.refreshEquipment(fixture.player, true);
            verify(reach).removeModifier(own);
            verify(reach, never()).removeModifier(foreign);
        }
    }

    @Test
    void swingResolvesAfterGraceAndRhythmExpiresAtItsOriginalTick() {
        try (Fixture fixture = new Fixture()) {
            fixture.swing();
            assertEquals(1, fixture.tasks.size());
            Task task = fixture.tasks.getFirst();
            assertEquals(1L, task.delay);
            assertEquals(1L, task.period);
            clearInvocations(fixture.inventory);

            fixture.tick(1);
            verify(fixture.world, never()).playSound(any(Location.class), eq(Sound.ENTITY_PLAYER_ATTACK_SWEEP), anyFloat(), anyFloat());
            fixture.tick(2);
            verify(fixture.world).playSound(any(Location.class), eq(Sound.ENTITY_PLAYER_ATTACK_SWEEP), anyFloat(), anyFloat());
            fixture.tick(14);
            verify(fixture.player, never()).playSound(any(Location.class), eq(Sound.BLOCK_NOTE_BLOCK_PLING), anyFloat(), anyFloat());
            fixture.tick(15);
            fixture.tick(15);
            verify(fixture.player).playSound(any(Location.class), eq(Sound.BLOCK_NOTE_BLOCK_PLING), anyFloat(), anyFloat());
            fixture.tick(20);
            assertFalse(task.cancelled);
            fixture.tick(21);
            assertTrue(task.cancelled);
            ArgumentCaptor<PotionEffect> penalty = ArgumentCaptor.forClass(PotionEffect.class);
            verify(fixture.player).addPotionEffect(penalty.capture());
            assertEquals(PotionEffectType.MINING_FATIGUE, penalty.getValue().getType());
            assertEquals(40, penalty.getValue().getDuration());
            verifyNoInteractions(fixture.inventory);
            fixture.actionBars.verify(() -> ActionBarHelper.showActionBar(fixture.player, ""));
        }
    }

    @Test
    void invalidEquipmentCancelsActiveWorkAndWarnsOnceUntilItChanges() {
        try (Fixture fixture = new Fixture()) {
            fixture.swing();
            Task task = fixture.tasks.getFirst();
            ItemStack shield = mock(ItemStack.class);
            when(shield.getType()).thenReturn(Material.SHIELD);
            when(fixture.inventory.getItemInOffHand()).thenReturn(shield);

            fixture.listener.refreshEquipment(fixture.player, false);
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.swing();
            fixture.tick(2);

            assertTrue(task.cancelled);
            assertEquals(1, fixture.tasks.size());
            fixture.actionBars.verify(() -> ActionBarHelper.showActionBar(fixture.player, "offhand"), times(1));
            verify(fixture.world, never()).playSound(any(Location.class), eq(Sound.ENTITY_PLAYER_ATTACK_SWEEP), anyFloat(), anyFloat());

            when(fixture.inventory.getItemInOffHand()).thenReturn(null);
            fixture.listener.refreshEquipment(fixture.player, true);
            when(fixture.inventory.getItemInOffHand()).thenReturn(shield);
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.actionBars.verify(() -> ActionBarHelper.showActionBar(fixture.player, "offhand"), times(2));
        }
    }

    @Test
    void quittingAndStoppingFenceCancelledCallbacksAcrossNewSessions() {
        try (Fixture fixture = new Fixture()) {
            fixture.swing();
            Task oldSession = fixture.tasks.getFirst();
            fixture.listener.onQuit(new PlayerQuitEvent(fixture.player, ""));
            fixture.swing();
            Task currentSession = fixture.tasks.get(1);
            oldSession.callback.run();
            oldSession.retired.run();
            assertTrue(oldSession.cancelled);
            assertFalse(currentSession.cancelled);

            fixture.listener.stop();
            fixture.listener.start();
            fixture.swing();
            Task restarted = fixture.tasks.get(2);
            currentSession.callback.run();
            currentSession.retired.run();
            assertTrue(currentSession.cancelled);
            assertFalse(restarted.cancelled);
            fixture.tick(2);
            verify(fixture.world).playSound(any(Location.class), eq(Sound.ENTITY_PLAYER_ATTACK_SWEEP), anyFloat(), anyFloat());
        }
    }

    @Test
    void retirementDropsBookkeepingWithoutAccessingPlayerState() {
        try (Fixture fixture = new Fixture()) {
            fixture.swing();
            Task task = fixture.tasks.getFirst();
            clearInvocations(fixture.player, fixture.inventory, fixture.world);

            task.retired.run();
            task.callback.run();

            verifyNoInteractions(fixture.player, fixture.inventory, fixture.world);
            fixture.swing();
            assertEquals(2, fixture.tasks.size());
            fixture.tick(2);
            verify(fixture.world).playSound(any(Location.class), eq(Sound.ENTITY_PLAYER_ATTACK_SWEEP), anyFloat(), anyFloat());
        }
    }

    @Test
    void queuedCleanupFromPreviousListenerPreservesNewListenersModifiersAndActionBar() {
        try (Fixture fixture = new Fixture()) {
            AttributeInstance reach = mock(AttributeInstance.class);
            AttributeInstance knockback = mock(AttributeInstance.class);
            AttributeModifier reachModifier = mock(AttributeModifier.class);
            AttributeModifier knockbackModifier = mock(AttributeModifier.class);
            when(reachModifier.getKey()).thenReturn(new NamespacedKey("pepeforge", "greatsword_reach_bonus"));
            when(reachModifier.getAmount()).thenReturn(0.45D);
            when(knockbackModifier.getKey()).thenReturn(new NamespacedKey("pepeforge", "greatsword_projectile_knockback"));
            when(reach.getModifiers()).thenReturn(Set.of(reachModifier));
            when(knockback.getModifiers()).thenReturn(Set.of(knockbackModifier));
            when(fixture.player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)).thenReturn(reach);
            when(fixture.player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)).thenReturn(knockback);
            fixture.swing();
            fixture.tick(2);
            List<Runnable> cleanups = new ArrayList<>();
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(false);
            fixture.scheduler.when(() -> SchedulerCompat.runForPlayer(eq(fixture.player), eq(fixture.plugin),
                    any(Runnable.class), any(Runnable.class))).thenAnswer(invocation -> {
                        cleanups.add(invocation.getArgument(2));
                        return null;
                    });
            fixture.listener.stop();
            assertEquals(1, cleanups.size());

            GreatswordListener replacement = new GreatswordListener(fixture.plugin, fixture.items, fixture.lang);
            replacement.start();
            replacement.refreshEquipment(fixture.player, false);
            fixture.actionBars.clearInvocations();
            clearInvocations(reach, knockback);
            cleanups.getFirst().run();

            verifyNoInteractions(reach, knockback);
            fixture.actionBars.verify(() -> ActionBarHelper.showActionBar(fixture.player, ""), never());
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(true);
            replacement.stop();
        }
    }

    @Test
    void cleanupWithoutAnOwnerStillRemovesLegacyModifiers() {
        try (Fixture fixture = new Fixture()) {
            AttributeInstance reach = mock(AttributeInstance.class);
            AttributeModifier legacy = mock(AttributeModifier.class);
            when(legacy.getKey()).thenReturn(new NamespacedKey("pepeforge", "greatsword_reach_bonus"));
            when(reach.getModifiers()).thenReturn(Set.of(legacy));
            when(fixture.player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)).thenReturn(reach);

            fixture.listener.clearAllPlayerState();

            verify(reach).removeModifier(legacy);
        }
    }

    @Test
    void lifecycleRetirementUsesCapturedIdentityWithoutEntityAccess() {
        try (Fixture fixture = new Fixture()) {
            fixture.swing();
            List<Runnable> retirements = new ArrayList<>();
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(false);
            fixture.scheduler.when(() -> SchedulerCompat.runForPlayer(eq(fixture.player), eq(fixture.plugin),
                    any(Runnable.class), any(Runnable.class))).thenAnswer(invocation -> {
                        retirements.add(invocation.getArgument(3));
                        return null;
                    });
            fixture.listener.stop();
            clearInvocations(fixture.player);

            retirements.getFirst().run();

            verifyNoInteractions(fixture.player);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final JavaPlugin plugin = mock(JavaPlugin.class);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final ItemFactory items = mock(ItemFactory.class);
        final PluginLang lang = mock(PluginLang.class);
        final World world = mock(World.class);
        final ItemStack weapon = mock(ItemStack.class);
        final AtomicLong time = new AtomicLong();
        final List<Task> tasks = new ArrayList<>();
        final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        final MockedStatic<ActionBarHelper> actionBars = mockStatic(ActionBarHelper.class);
        final GreatswordListener listener;

        Fixture() {
            when(plugin.getName()).thenReturn("PepeForge");
            when(plugin.namespace()).thenReturn("pepeforge");
            when(plugin.getServer()).thenReturn(server);
            doReturn(List.of(player)).when(server).getOnlinePlayers();
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getWorld()).thenReturn(world);
            when(player.getEyeLocation()).thenAnswer(ignored -> new Location(world, 0, 65, 0));
            when(player.getLocation()).thenAnswer(ignored -> new Location(world, 0, 64, 0));
            when(world.getGameTime()).thenAnswer(ignored -> time.get());
            when(inventory.getItemInMainHand()).thenReturn(weapon);
            when(items.getGreatswordTier(weapon)).thenReturn(GreatswordTier.IRON);
            when(lang.text("messages.two_handed.offhand_required")).thenReturn("offhand");
            when(lang.text("messages.greatsword.rhythm")).thenReturn("{bar} {stage}/{max_stage}");
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(player)).thenReturn(true);
            scheduler.when(() -> SchedulerCompat.runTimerForEntity(eq(player), eq(plugin), any(Runnable.class),
                    any(Runnable.class), anyLong(), anyLong())).thenAnswer(invocation -> {
                        Task task = new Task(invocation.getArgument(2), invocation.getArgument(3),
                                invocation.getArgument(4), invocation.getArgument(5));
                        tasks.add(task);
                        return task;
                    });
            listener = new GreatswordListener(plugin, items, lang);
            listener.start();
        }

        void swing() {
            listener.onSwing(new PlayerAnimationEvent(player, PlayerAnimationType.ARM_SWING));
        }

        void tick(long tick) {
            time.set(tick);
            List.copyOf(tasks).forEach(task -> task.callback.run());
        }

        @Override
        public void close() {
            scheduler.close();
            actionBars.close();
        }
    }

    private static final class Task implements ScheduledTaskCompat {
        final Runnable callback;
        final Runnable retired;
        final long delay;
        final long period;
        boolean cancelled;

        Task(Runnable callback, Runnable retired, long delay, long period) {
            this.callback = callback;
            this.retired = retired;
            this.delay = delay;
            this.period = period;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
