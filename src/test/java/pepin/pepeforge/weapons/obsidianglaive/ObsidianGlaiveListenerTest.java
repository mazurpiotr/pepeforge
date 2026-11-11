package pepin.pepeforge.weapons.obsidianglaive;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import com.google.common.base.Function;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.obsidianglaive.compat.GlaiveCombatAdapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ObsidianGlaiveListenerTest {
    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void acceptedPrimaryHitsCycleOnceAndCancelledOrSecondaryHitsDoNotAdvance() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            assertEquals(1, f.stage.get());
            EntityDamageByEntityEvent cancelled = f.damage(f.primary, 7, DamageCause.ENTITY_ATTACK);
            f.listener.onDamage(cancelled);
            cancelled.setCancelled(true);
            f.listener.onSuccessfulDamage(cancelled);
            assertEquals(1, f.stage.get());
            EntityDamageByEntityEvent sweep = f.damage(f.primary, 1, DamageCause.ENTITY_SWEEP_ATTACK);
            f.listener.onDamage(sweep);
            assertTrue(sweep.isCancelled());
            f.listener.onSuccessfulDamage(sweep);
            assertEquals(1, f.stage.get());
            f.hit();
            assertEquals(2, f.stage.get());
            f.hit();
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void closePenaltyAndMovementDependOnAcceptedPositiveFinalDamage() {
        try (Fixture f = new Fixture(true, true)) {
            f.place(f.primary, 1.0D);
            EntityDamageByEntityEvent close = f.hit();
            assertEquals(2.1D, close.getDamage(), 1.0E-9D);
            verify(f.primary).setVelocity(argThat(v -> Math.abs(v.getZ() - 0.35D) < 1.0E-9D));
        }
        try (Fixture f = new Fixture(true, true)) {
            f.place(f.primary, 1.0D);
            EntityDamageByEntityEvent blocked = f.damage(f.primary, 7, DamageCause.ENTITY_ATTACK);
            when(blocked.getFinalDamage()).thenReturn(0.0D);
            f.listener.onDamage(blocked);
            f.listener.onSuccessfulDamage(blocked);
            assertEquals(1, f.stage.get());
            verify(f.primary, never()).setVelocity(any());
        }
    }

    @Test
    void fullWhackDamageStartsAtExactlyOneAndHalfBlocks() {
        try (Fixture f = new Fixture(true, true)) {
            f.place(f.primary, 1.5D);
            assertEquals(7.0D, f.hit().getDamage());
            verify(f.primary, never()).setVelocity(any());
        }
    }

    @Test
    void stabWorksUpCloseAndPreservesStrongerUpwardVelocity() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            when(f.primary.getVelocity()).thenReturn(new Vector(0.2, 0.5, 0.1));
            when(f.attack.sweepAllowed()).thenReturn(true);
            f.hit();
            verify(f.primary).setVelocity(new Vector(0.2, 0.5, 0.1));
            verify(f.world).spawnParticle(eq(Particle.GUST_EMITTER_SMALL), any(Location.class), eq(1),
                    eq(0.0D), eq(0.0D), eq(0.0D), eq(0.0D));
            verify(f.world).getNearbyEntities(any(BoundingBox.class));
            f.hit();
            f.hit();
            f.place(f.primary, 0.5D);
            EntityDamageByEntityEvent closeStab = f.hit();
            assertFalse(closeStab.isCancelled());
            assertEquals(7.0D, closeStab.getDamage());
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void disabledStabAndLiftRespectTheirOwnSettings() {
        try (Fixture f = new Fixture(false, true)) {
            for (int i = 0; i < 5; i++) {
                f.hit();
                assertEquals(0, f.stage.get());
            }
        }
        try (Fixture f = new Fixture(true, false)) {
            f.hit();
            f.hit();
            LivingEntity secondary = f.secondary(3.0D, false, false);
            when(f.world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of(secondary));
            when(f.attack.stabDamage(secondary)).thenReturn(7.0D);
            f.hit();
            verify(f.primary, never()).setVelocity(any());
            verify(secondary).damage(7.0D, f.player);
            verify(secondary, never()).setVelocity(any());
            verify(f.world, never()).spawnParticle(eq(Particle.GUST_EMITTER_SMALL), any(Location.class), anyInt(),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    @Test
    void cancelledAndFullyBlockedStabsDoNotCreateLiftOrWindParticles() {
        for (boolean cancelled : new boolean[]{true, false}) {
            try (Fixture f = new Fixture(true, true)) {
                f.hit();
                f.hit();
                EntityDamageByEntityEvent stab = f.damage(f.primary, 7, DamageCause.ENTITY_ATTACK);
                f.listener.onDamage(stab);
                if (cancelled) {
                    stab.setCancelled(true);
                } else {
                    when(stab.getFinalDamage()).thenReturn(0.0D);
                }
                f.listener.onSuccessfulDamage(stab);
                if (cancelled) {
                    verify(f.world, never()).getNearbyEntities(any(BoundingBox.class));
                    assertEquals(2, f.stage.get());
                }
                verify(f.primary, never()).setVelocity(any());
                verify(f.world, never()).spawnParticle(eq(Particle.GUST_EMITTER_SMALL), any(Location.class), anyInt(),
                        anyDouble(), anyDouble(), anyDouble(), anyDouble());
            }
        }
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void stabRetainsArmorResistanceAndEnchantmentProtection() {
        try (Fixture f = new Fixture(true, true)) {
            Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
            modifiers.put(DamageModifier.BASE, 7.0D);
            modifiers.put(DamageModifier.ARMOR, -3.5D);
            modifiers.put(DamageModifier.RESISTANCE, -0.7D);
            modifiers.put(DamageModifier.MAGIC, -0.56D);
            Map<DamageModifier, Function<Double, Double>> functions = new EnumMap<>(DamageModifier.class);
            functions.put(DamageModifier.BASE, ignored -> 0.0D);
            functions.put(DamageModifier.ARMOR, amount -> -amount * 0.5D);
            functions.put(DamageModifier.RESISTANCE, amount -> -amount * 0.2D);
            functions.put(DamageModifier.MAGIC, amount -> -amount * 0.2D);

            EntityDamageByEntityEvent whack = new EntityDamageByEntityEvent(f.player, f.primary,
                    DamageCause.ENTITY_ATTACK, mock(DamageSource.class), modifiers, functions);
            f.listener.onDamage(whack);
            assertEquals(-3.5D, whack.getDamage(DamageModifier.ARMOR));
            f.listener.onSuccessfulDamage(whack);
            f.hit();
            assertEquals(2, f.stage.get());

            EntityDamageByEntityEvent stab = new EntityDamageByEntityEvent(f.player, f.primary,
                    DamageCause.ENTITY_ATTACK, mock(DamageSource.class), modifiers, functions);
            f.listener.onDamage(stab);
            assertEquals(7.0D, stab.getDamage());
            assertEquals(-3.5D, stab.getDamage(DamageModifier.ARMOR));
            assertEquals(-0.7D, stab.getDamage(DamageModifier.RESISTANCE), 1.0E-9D);
            assertEquals(-0.56D, stab.getDamage(DamageModifier.MAGIC), 1.0E-9D);
            assertEquals(2.24D, stab.getFinalDamage(), 1.0E-9D);
        }
    }

    @Test
    void stabPiercesAlignedTargetsOnceWithoutSweepDamageOrExtraComboProgress() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            LivingEntity close = f.secondary(0.5D, false, false);
            LivingEntity far = f.secondary(4.5D, false, false);
            LivingEntity outside = f.secondary(Math.nextUp(4.5D), false, false);
            LivingEntity behind = f.secondary(-1.0D, false, false);
            LivingEntity sideways = f.secondary(3.0D, false, false);
            when(sideways.getBoundingBox()).thenReturn(new BoundingBox(0.5, 0, 3, 1.1, 1.8, 3.6));
            when(f.world.getNearbyEntities(any(BoundingBox.class)))
                    .thenReturn(List.of(f.primary, far, close, outside, behind, sideways));
            when(f.attack.stabDamage(close)).thenReturn(7.0D);
            when(f.attack.stabDamage(far)).thenReturn(9.0D);
            f.hit();
            verify(close).damage(7.0D, f.player);
            verify(far).damage(9.0D, f.player);
            verify(f.primary, never()).damage(anyDouble(), any(Entity.class));
            verify(outside, never()).damage(anyDouble(), any(Entity.class));
            verify(behind, never()).damage(anyDouble(), any(Entity.class));
            verify(sideways, never()).damage(anyDouble(), any(Entity.class));
            verify(close).setVelocity(new Vector(0, 0.18, 0));
            verify(far).setVelocity(new Vector(0, 0.18, 0));
            verify(f.attack, never()).sweepDamage(any());
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void piercingTargetsRespectIndependentProtectionShieldAndRegionOwnership() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            LivingEntity denied = f.secondary(2.8D, true, false);
            LivingEntity blocked = f.secondary(3.2D, false, true);
            LivingEntity accepted = f.secondary(3.6D, false, false);
            LivingEntity foreign = f.secondary(4.0D, false, false);
            f.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(foreign)).thenReturn(false);
            when(f.world.getNearbyEntities(any(BoundingBox.class)))
                    .thenReturn(List.of(denied, blocked, accepted, foreign));
            when(f.attack.stabDamage(any())).thenReturn(7.0D);
            f.hit();
            verify(denied).damage(7.0D, f.player);
            verify(blocked).damage(7.0D, f.player);
            verify(accepted).damage(7.0D, f.player);
            verify(denied, never()).setVelocity(any());
            verify(blocked, never()).setVelocity(any());
            verify(accepted).setVelocity(new Vector(0, 0.18, 0));
            verify(foreign, never()).getBoundingBox();
            verify(foreign, never()).damage(anyDouble(), any(Entity.class));
            verify(f.world, times(2)).spawnParticle(eq(Particle.GUST_EMITTER_SMALL), any(Location.class), eq(1),
                    eq(0.0D), eq(0.0D), eq(0.0D), eq(0.0D));
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void piercingStopsAtWallsAndUsesCreativeReach() {
        for (boolean creative : new boolean[]{false, true}) {
            try (Fixture f = new Fixture(true, false)) {
                f.hit();
                f.hit();
                when(f.player.getGameMode()).thenReturn(creative ? GameMode.CREATIVE : GameMode.SURVIVAL);
                LivingEntity distant = f.secondary(6.5D, false, false);
                when(f.attack.stabDamage(distant)).thenReturn(7.0D);
                when(f.world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of(distant));
                f.hit();
                verify(distant, times(creative ? 1 : 0)).damage(7.0D, f.player);
            }
        }
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            LivingEntity beyondWall = f.secondary(4.0D, false, false);
            when(f.world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of(beyondWall));
            when(f.world.rayTraceBlocks(any(), any(), eq(4.5D), any(), anyBoolean()))
                    .thenReturn(new RayTraceResult(new Vector(0, 1.62, 3.5)));
            f.hit();
            verify(beyondWall, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Test
    void piercingChecksVisibilityAtAimedContactRatherThanNearestHitboxCorner() {
        try (Fixture f = new Fixture(true, false)) {
            f.hit();
            f.hit();
            when(f.player.getEyeLocation()).thenAnswer(i -> new Location(f.world, 0, 1.62, 0, -45, 0));
            when(f.primary.getBoundingBox()).thenReturn(new BoundingBox(1.7, 0, 1.7, 2.3, 1.8, 2.3));
            LivingEntity target = f.secondary(3.0D, false, false);
            when(target.getBoundingBox()).thenReturn(new BoundingBox(2.6, 0, 2.8, 3.2, 1.8, 3.4));
            when(f.world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of(target));
            when(f.attack.stabDamage(target)).thenReturn(7.0D);
            when(f.world.rayTraceBlocks(any(), argThat((Vector v) -> Math.abs(v.getX() - v.getZ()) > 1.0E-6D),
                    anyDouble(), any(), anyBoolean())).thenReturn(new RayTraceResult(new Vector(2, 1.62, 2.2)));
            f.hit();
            verify(target).damage(7.0D, f.player);
        }
    }

    @Test
    void onlyLatestExpiryCanResetAndEquipmentChangeInvalidatesQueuedCallbacks() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            Runnable old = f.tasks.getFirst();
            f.tick.set(150L);
            f.hit();
            Runnable current = f.tasks.getLast();
            old.run();
            assertEquals(2, f.stage.get());
            f.tick.set(310L);
            current.run();
            assertEquals(0, f.stage.get());
            f.hit();
            Runnable beforeSwitch = f.tasks.getLast();
            PlayerItemHeldEvent change = mock(PlayerItemHeldEvent.class);
            when(change.getPlayer()).thenReturn(f.player);
            f.listener.onHeldItem(change);
            assertEquals(0, f.stage.get());
            f.hit();
            beforeSwitch.run();
            assertEquals(1, f.stage.get());
            f.listener.stop();
            f.tasks.getLast().run();
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void wallsUnloadedChunksAndForeignRegionsRejectCustomDamageAndSideEffects() {
        try (Fixture f = new Fixture(true, true)) {
            when(f.world.rayTraceBlocks(any(), any(), anyDouble(), any(), anyBoolean()))
                    .thenReturn(mock(RayTraceResult.class));
            assertTrue(f.hit().isCancelled());
            assertEquals(0, f.stage.get());
            verify(f.primary, never()).setVelocity(any());
        }
        try (Fixture f = new Fixture(true, true)) {
            when(f.world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
            assertTrue(f.hit().isCancelled());
            verifyNoInteractions(f.attack);
        }
        try (Fixture f = new Fixture(true, true)) {
            f.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(f.primary)).thenReturn(false);
            f.hit();
            verify(f.primary, never()).getBoundingBox();
            assertEquals(0, f.stage.get());
        }
    }

    @Test
    void splashUsesIndependentDamageAndOnlyAcceptedTargetsReceiveMovement() {
        try (Fixture f = new Fixture(true, true)) {
            f.place(f.primary, 1.0D);
            LivingEntity secondary = f.secondary(2.0D, false, false);
            LivingEntity denied = f.secondary(1.2D, true, false);
            LivingEntity blocked = f.secondary(1.3D, false, true);
            when(f.world.getNearbyEntities(any(BoundingBox.class)))
                    .thenReturn(List.of(f.primary, secondary, denied, blocked));
            when(f.attack.sweepAllowed()).thenReturn(true);
            when(f.attack.sweepDamage(any())).thenReturn(6.0D);
            f.hit();
            verify(secondary).damage(6.0D, f.player);
            verify(denied).damage(doubleThat(d -> Math.abs(d - 1.8D) < 1.0E-9D), eq(f.player));
            verify(blocked).damage(doubleThat(d -> Math.abs(d - 1.8D) < 1.0E-9D), eq(f.player));
            verify(secondary).setVelocity(any());
            verify(denied, never()).setVelocity(any());
            verify(blocked, never()).setVelocity(any());
            assertEquals(1, f.stage.get());
        }
    }

    @Test
    void inventoryTransferRestoresPreparedItemsInContainersAndCursorAfterTheTransaction() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            Inventory container = mock(Inventory.class);
            when(container.getSize()).thenReturn(9);
            InventoryView view = mock(InventoryView.class);
            when(view.getTopInventory()).thenReturn(container);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(f.player);
            when(click.getCurrentItem()).thenReturn(f.item);
            when(click.getView()).thenReturn(view);
            f.listener.onInventoryClick(click);
            verify(container, never()).setItem(anyInt(), any());
            when(container.getItem(0)).thenReturn(f.item);
            when(f.inventory.getItem(0)).thenReturn(null);
            when(f.inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
            when(f.player.getItemOnCursor()).thenReturn(f.item);
            f.tasks.getLast().run();
            assertEquals(0, f.stage.get());
            verify(container).setItem(0, f.item);
            verify(f.player).setItemOnCursor(f.item);
        }
    }

    @Test
    void quittingBeforeTransferCallbackStillRestoresTheCapturedContainer() {
        try (Fixture f = new Fixture(true, true)) {
            f.hit();
            f.hit();
            Inventory container = mock(Inventory.class);
            when(container.getSize()).thenReturn(9);
            InventoryView view = mock(InventoryView.class);
            when(view.getTopInventory()).thenReturn(container);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getWhoClicked()).thenReturn(f.player);
            when(click.getCurrentItem()).thenReturn(f.item);
            when(click.getView()).thenReturn(view);
            f.listener.onInventoryClick(click);
            Runnable queued = f.tasks.getLast();
            when(container.getItem(0)).thenReturn(f.item);
            when(f.inventory.getItem(0)).thenReturn(null);
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(f.player);
            f.listener.onQuit(quit);
            assertEquals(0, f.stage.get());
            verify(container).setItem(0, f.item);
            clearInvocations(container);
            queued.run();
            verify(container, never()).setItem(anyInt(), any());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Player player = mock(Player.class);
        private final PlayerInventory inventory = mock(PlayerInventory.class);
        private final LivingEntity primary = mock(LivingEntity.class);
        private final World world = mock(World.class);
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        private final ItemFactory factory = mock(ItemFactory.class);
        private final GlaiveCombatAdapter combat = mock(GlaiveCombatAdapter.class);
        private final GlaiveCombatAdapter.Attack attack = mock(GlaiveCombatAdapter.Attack.class);
        private final AtomicInteger stage = new AtomicInteger();
        private final AtomicLong tick = new AtomicLong(100L);
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private final List<Runnable> tasks = new ArrayList<>();
        private final ObsidianGlaiveListener listener;
        private final DamageFlow flow = new DamageFlow();

        private Fixture(boolean stab, boolean lift) {
            when(plugin.getServer()).thenReturn(org.bukkit.Bukkit.getServer());
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getInventory()).thenReturn(inventory);
            when(player.isOnline()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(player.getScoreboard()).thenReturn(mock(Scoreboard.class));
            when(player.getLocation()).thenAnswer(i -> new Location(world, 0, 0, 0));
            when(player.getEyeLocation()).thenAnswer(i -> new Location(world, 0, 1.62, 0));
            when(inventory.getItemInMainHand()).thenReturn(item);
            when(inventory.getItem(0)).thenReturn(item);
            when(inventory.getSize()).thenReturn(9);
            when(factory.isObsidianGlaive(item)).thenReturn(true);
            when(factory.getItemId(item)).thenReturn(ItemIds.OBSIDIAN_GLAIVE);
            when(factory.getObsidianGlaiveIdentity(item)).thenReturn("glaive-1");
            when(factory.getObsidianGlaiveStage(item)).thenAnswer(i -> stage.get());
            when(factory.hasCurrentGlaiveRevision(item)).thenReturn(true);
            doAnswer(i -> { stage.set(i.getArgument(1)); return null; })
                    .when(factory).prepareObsidianGlaive(eq(item), anyInt());
            doAnswer(i -> { stage.set(0); return null; }).when(factory).beginObsidianGlaiveCombo(item);
            when(world.getGameTime()).thenAnswer(i -> tick.get());
            when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
            when(world.getNearbyEntities(any(BoundingBox.class))).thenReturn(List.of());
            when(combat.capture(player, primary)).thenReturn(attack);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(World.class), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(eq(player), eq(plugin), any(Runnable.class),
                    any(Runnable.class), anyLong())).thenAnswer(i -> {
                        tasks.add(i.getArgument(2));
                        return (pepin.pepeforge.util.scheduler.ScheduledTaskCompat) () -> {};
                    });
            place(primary, 2.5D);
            listener = new ObsidianGlaiveListener(plugin, factory, new ObsidianGlaiveSettings(7, stab, lift), combat);
            listener.start();
        }

        private void place(LivingEntity target, double z) {
            when(target.getUniqueId()).thenReturn(UUID.randomUUID());
            when(target.isValid()).thenReturn(true);
            when(target.getWorld()).thenReturn(world);
            when(target.getHealth()).thenReturn(10.0D);
            when(target.getBoundingBox()).thenReturn(new BoundingBox(-0.3, 0, z, 0.3, 1.8, z + 0.6));
            when(target.getLocation()).thenAnswer(i -> new Location(world, 0, 0, z + 0.3));
            when(target.getVelocity()).thenAnswer(i -> new Vector());
        }

        private LivingEntity secondary(double z, boolean cancel, boolean block) {
            LivingEntity entity = mock(LivingEntity.class);
            place(entity, z);
            doAnswer(i -> {
                EntityDamageByEntityEvent event = damage(entity, i.getArgument(0), DamageCause.ENTITY_ATTACK);
                if (block) {
                    when(event.getFinalDamage()).thenReturn(0.0D);
                }
                flow.capture(event);
                listener.onDamage(event);
                event.setCancelled(cancel);
                listener.onSuccessfulDamage(event);
                return null;
            }).when(entity).damage(anyDouble(), eq(player));
            return entity;
        }

        private EntityDamageByEntityEvent hit() {
            EntityDamageByEntityEvent event = damage(primary, 7, DamageCause.ENTITY_ATTACK);
            listener.onDamage(event);
            listener.onSuccessfulDamage(event);
            return event;
        }

        private EntityDamageByEntityEvent damage(LivingEntity entity, double amount, DamageCause cause) {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            double[] value = {amount};
            AtomicBoolean cancelled = new AtomicBoolean();
            when(event.getDamager()).thenReturn(player);
            when(event.getEntity()).thenReturn(entity);
            when(event.getCause()).thenReturn(cause);
            when(event.getDamage()).thenAnswer(i -> value[0]);
            when(event.getFinalDamage()).thenAnswer(i -> value[0]);
            doAnswer(i -> { value[0] = i.getArgument(0); return null; }).when(event).setDamage(anyDouble());
            when(event.isCancelled()).thenAnswer(i -> cancelled.get());
            doAnswer(i -> { cancelled.set(i.getArgument(0)); return null; }).when(event).setCancelled(anyBoolean());
            return event;
        }

        @Override
        public void close() {
            listener.stop();
            scheduler.close();
        }
    }
}
