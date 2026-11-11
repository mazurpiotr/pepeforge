package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.VoxelShape;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TemporaryShieldManager;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MoonWaveTest {
    private static JavaPlugin plugin;
    private final World world = mock(World.class);
    private final Player owner = mock(Player.class);
    private final ItemDisplay display = mock(ItemDisplay.class);
    private final List<Entity> targets = new ArrayList<>();
    private final List<Transformation> transforms = new ArrayList<>();
    private final List<MoonWave> finished = new ArrayList<>();
    private final Runnable acceptedHit = mock(Runnable.class);
    private MoonWave wave;

    @BeforeAll
    static void createServer() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void destroyServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setup() {
        when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
        when(owner.getWorld()).thenReturn(world);
        when(owner.getLocation()).thenAnswer(call -> new Location(world, 0, 0, 0, 0, -85));
        when(owner.getHeight()).thenReturn(1.8D);
        when(owner.isOnline()).thenReturn(true);
        when(display.isValid()).thenReturn(true);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        Block air = mock(Block.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(world.getNearbyEntities(any(BoundingBox.class))).thenAnswer(call -> targets);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class))).thenAnswer(call -> {
            Consumer<ItemDisplay> initializer = call.getArgument(2);
            initializer.accept(display);
            return display;
        });
        doAnswer(call -> {
            transforms.add(call.getArgument(0));
            return null;
        }).when(display).setTransformation(any(Transformation.class));
    }

    @Test
    void thinWaveHitsTallTargetAndMissesTargetBelowItsLowerEdge() {
        LivingEntity low = target(-0.3, 2, 0.3, 2.6);
        when(low.getBoundingBox()).thenReturn(new BoundingBox(-0.3, 0, 2, 0.3, 1, 2.6));
        LivingEntity tall = target(-0.3, 2, 0.3, 2.6);
        when(tall.getBoundingBox()).thenReturn(new BoundingBox(-0.3, 0, 2, 0.3, 1.2, 2.6));
        start(UUID.randomUUID(), 4.5, 3);
        wave.run();
        verify(low, never()).damage(anyDouble(), any(Entity.class));
        verify(tall).damage(2.4D, owner);
        wave.stop();
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.8, 1.5})
    void spawnAndThinHitboxStayAboveFeetForStandingAndCrouchingWithoutShrinkingModel(double playerHeight) {
        when(owner.getHeight()).thenReturn(playerHeight);
        when(owner.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0, 0, -85));
        start(UUID.randomUUID(), 4.5, 0.65);
        ArgumentCaptor<Location> origin = ArgumentCaptor.forClass(Location.class);
        verify(world).spawn(origin.capture(), eq(ItemDisplay.class), any(Consumer.class));
        assertEquals(65.3, origin.getValue().getY(), 1.0E-9);
        wave.run();
        ArgumentCaptor<BoundingBox> bounds = ArgumentCaptor.forClass(BoundingBox.class);
        verify(world).getNearbyEntities(bounds.capture());
        assertEquals(65.1, bounds.getValue().getMinY(), 1.0E-9);
        assertEquals(65.5, bounds.getValue().getMaxY(), 1.0E-9);
        assertEquals(1.4, transforms.getFirst().getScale().y, 1.0E-6);
        assertEquals(1.4, transforms.getLast().getScale().y, 1.0E-6);
        assertEquals(transforms.getFirst().getLeftRotation(), transforms.getLast().getLeftRotation());
        wave.stop();
    }

    @ParameterizedTest
    @CsvSource({"STONE, 0, 1", "STONE_SLAB, 0, 0.5", "STONE_SLAB, 0.5, 1"})
    void wavePassesAboveLowCollisionShapesAndReachesTarget(Material material, double minY, double maxY) {
        LivingEntity behind = target(-0.3, 2, 0.3, 2.6);
        Block obstacle = solidBlock(material, new BoundingBox(0, minY, 0, 1, maxY, 1));
        when(world.getBlockAt(0, 0, 1)).thenReturn(obstacle);
        start(UUID.randomUUID(), 4.5, 3);
        wave.run();
        verify(obstacle).getCollisionShape();
        verify(behind).damage(2.4D, owner);
        verify(display, never()).remove();
        wave.stop();
    }

    @Test
    void fenceAtWaveEdgeStopsItEvenThoughItsBlockCellIsBelowTheHitbox() {
        LivingEntity behind = target(-0.3, 2, 0.3, 2.6);
        Block fence = solidBlock(Material.OAK_FENCE,
                new BoundingBox(0.375, 0, 0.375, 0.625, 1.5, 0.625));
        when(world.getBlockAt(1, 0, 1)).thenReturn(fence);
        start(UUID.randomUUID(), 4.5, 3);
        wave.run();
        verify(fence).getCollisionShape();
        verify(behind, never()).damage(anyDouble(), any(Entity.class));
        verify(display).remove();
        assertEquals(List.of(wave), finished);
    }

    @Test
    void configuredWidthScalesDisplayAndHitsTargetsNearItsEdge() {
        LivingEntity inside = target(1.8, 2, 2.4, 2.6);
        LivingEntity outside = target(2.1, 2, 2.7, 2.6);
        wave = new MoonWave(plugin, owner, UUID.randomUUID(), 2.4, 4.5, 4, 3, acceptedHit, finished::add);
        wave.start();
        wave.run();
        assertEquals(4, transforms.getFirst().getScale().x);
        assertEquals(4, transforms.getLast().getScale().x);
        verify(inside).damage(2.4D, owner);
        verify(outside, never()).damage(anyDouble(), any(Entity.class));
        wave.stop();
    }

    @Test
    void widerWaveStopsAtWallOutsideDefaultWidth() {
        LivingEntity behind = target(-0.3, 2, 0.3, 2.6);
        Block wall = mock(Block.class);
        when(wall.getType()).thenReturn(Material.STONE);
        VoxelShape shape = mock(VoxelShape.class);
        when(shape.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(0, 0, 0, 1, 1, 1)));
        when(wall.getCollisionShape()).thenReturn(shape);
        when(world.getBlockAt(2, 1, 1)).thenReturn(wall);
        wave = new MoonWave(plugin, owner, UUID.randomUUID(), 2.4, 4.5, 4.5, 4.5, acceptedHit, finished::add);
        wave.start();
        wave.run();
        verify(behind, never()).damage(anyDouble(), any(Entity.class));
        verify(display).remove();
        assertEquals(List.of(wave), finished);
    }

    @Test
    void fastPiercingWaveExcludesOriginalTargetAndNeverRepeatsDamage() {
        LivingEntity original = target(-0.3, 0.5, 0.3, 1.1);
        LivingEntity other = target(-0.3, 2.0, 0.3, 2.6);
        start(original.getUniqueId(), 4.5, 3);
        wave.run();
        wave.run();
        wave.run();
        verify(original, never()).damage(anyDouble(), any(Entity.class));
        verify(other).damage(2.4D, owner);
        verify(other, never()).setNoDamageTicks(anyInt());
        verify(other, never()).setVelocity(any());
        verify(display).remove();
        assertEquals(List.of(wave), finished);
    }

    @Test
    void edgeWallBlocksTargetsBehindItButAllowsEarlierTargets() {
        LivingEntity before = target(-0.3, 0.3, 0.3, 0.7);
        LivingEntity behind = target(-0.3, 2, 0.3, 2.6);
        Block wall = mock(Block.class);
        when(wall.getType()).thenReturn(Material.STONE);
        VoxelShape shape = mock(VoxelShape.class);
        when(shape.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(0, 0, 0, 1, 1, 1)));
        when(wall.getCollisionShape()).thenReturn(shape);
        when(world.getBlockAt(1, 1, 1)).thenReturn(wall);
        start(UUID.randomUUID(), 4.5, 4.5);
        wave.run();
        verify(before).damage(2.4D, owner);
        verify(behind, never()).damage(anyDouble(), any(Entity.class));
        verify(display).remove();
    }

    @Test
    void onlyAcceptedUniqueTargetsGrantShieldAndOriginalTargetNeverDoes() {
        LivingEntity original = target(-0.3, 0.2, 0.3, 0.8);
        LivingEntity first = target(-0.3, 0.3, 0.3, 0.9);
        LivingEntity second = target(-0.3, 0.4, 0.3, 1);
        LivingEntity cancelled = target(-0.3, 0.5, 0.3, 1.1);
        LivingEntity unobserved = target(-0.3, 0.6, 0.3, 1.2);
        observeDamage(first, false, () -> { });
        observeDamage(second, false, () -> { });
        observeDamage(cancelled, true, () -> { });
        start(original.getUniqueId(), 4.5, 0.65);
        wave.run();
        wave.run();
        verify(acceptedHit, times(2)).run();
        verify(first).damage(2.4D, owner);
        verify(second).damage(2.4D, owner);
        verify(cancelled).damage(2.4D, owner);
        verify(unobserved).damage(2.4D, owner);
        verify(original, never()).damage(anyDouble(), any(Entity.class));
        wave.stop();
    }

    @Test
    void acceptedWaveDamageGrantsIndependentShieldWithoutTouchingNativeAbsorption() {
        TemporaryShieldManager shield = mock(TemporaryShieldManager.class);
        LivingEntity original = target(-0.3, 0.3, 0.3, 0.9);
        LivingEntity other = target(-0.3, 2.0, 0.3, 2.6);
        observeDamage(other, false, () -> { });
        wave = new MoonWave(plugin, owner, original.getUniqueId(), 2.4D, 4.5D, 2.8D, 0.65D,
                () -> shield.grant(owner, "moonshield", 0.5D, 4.0D, 80), finished::add);
        wave.start();
        verifyNoInteractions(shield);
        wave.run();
        wave.run();
        wave.run();
        verify(shield).grant(owner, "moonshield", 0.5D, 4.0D, 80);
        wave.run();
        wave.stop();
        verifyNoMoreInteractions(shield);
        verify(owner, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void interruptionDuringAcceptedDamageCannotGrantShield() {
        LivingEntity target = target(-0.3, 0.3, 0.3, 0.9);
        observeDamage(target, false, () -> wave.stop());
        start(UUID.randomUUID(), 4.5, 0.65);
        wave.run();
        verify(acceptedHit, never()).run();
        assertEquals(1, finished.size());
    }

    @Test
    void worldChangeDuringAcceptedDamageCannotGrantShield() {
        LivingEntity target = target(-0.3, 0.3, 0.3, 0.9);
        observeDamage(target, false, () -> when(owner.getWorld()).thenReturn(mock(World.class)));
        start(UUID.randomUUID(), 4.5, 0.65);
        wave.run();
        verify(acceptedHit, never()).run();
        assertEquals(1, finished.size());
    }

    @Test
    void capturedHorizontalDirectionAndTravelIgnorePitchYawChangesAndVisualRoll() {
        start(UUID.randomUUID(), 4.5, 0.65);
        when(owner.getLocation()).thenReturn(new Location(world, 0, 0, 0, 90, 85));
        wave.run();
        wave.run();
        Transformation transform = transforms.getLast();
        assertEquals(0, transform.getTranslation().x, 1.0E-6);
        assertEquals(0, transform.getTranslation().y, 1.0E-6);
        assertEquals(1.3, transform.getTranslation().z, 1.0E-6);
        assertEquals(transforms.getFirst().getLeftRotation(), transform.getLeftRotation());
        verify(display, never()).teleport(any(Location.class));
        wave.stop();
    }

    @Test
    void simultaneousWavesHaveIndependentHitSetsAndInterruptionIsIdempotent() {
        LivingEntity target = target(-0.3, 0.3, 0.3, 0.7);
        start(UUID.randomUUID(), 4.5, 0.65);
        ItemDisplay otherDisplay = mock(ItemDisplay.class);
        when(otherDisplay.isValid()).thenReturn(true);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class))).thenAnswer(call -> {
            Consumer<ItemDisplay> initializer = call.getArgument(2);
            initializer.accept(otherDisplay);
            return otherDisplay;
        });
        MoonWave other = new MoonWave(plugin, owner, UUID.randomUUID(), 2.4, 4.5, 2.8, 0.65, acceptedHit, finished::add);
        other.start();
        wave.run();
        other.run();
        verify(target, times(2)).damage(2.4D, owner);
        wave.stop();
        wave.stop();
        other.stop();
        wave.run();
        other.run();
        assertEquals(2, finished.size());
        verify(target, times(2)).damage(2.4D, owner);
        verify(display).remove();
        verify(otherDisplay).remove();
    }

    @Test
    void unloadedChunksInterruptWithoutDamage() {
        LivingEntity target = target(-0.3, 0.3, 0.3, 0.7);
        start(UUID.randomUUID(), 4.5, 0.65);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        wave.run();
        verify(target, never()).damage(anyDouble(), any(Entity.class));
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        assertEquals(1, finished.size());
    }

    @Test
    void foreignChunksInterruptBeforeAnyBlockOrTargetReads() {
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(eq(world),
                    anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
            start(UUID.randomUUID(), 4.5, 0.65);
            wave.run();
            verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
            verify(world, never()).getNearbyEntities(any(BoundingBox.class));
            assertEquals(1, finished.size());
        }
    }

    @Test
    void schedulerRetirementReleasesWaveAndCancelsItsHandle() {
        List<Runnable> retired = new ArrayList<>();
        ScheduledTaskCompat task = mock(ScheduledTaskCompat.class);
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.runTimerForEntity(eq(display), eq(plugin),
                    any(Runnable.class), any(Runnable.class), eq(1L), eq(1L))).thenAnswer(call -> {
                        retired.add(call.getArgument(3));
                        return task;
                    });
            start(UUID.randomUUID(), 4.5, 0.65);
            retired.getFirst().run();
            retired.getFirst().run();
            verify(task).cancel();
            assertEquals(1, finished.size());
        }
    }

    @Test
    void disconnectInterruptsWithoutDamage() {
        LivingEntity target = target(-0.3, 0.3, 0.3, 0.7);
        start(UUID.randomUUID(), 4.5, 0.65);
        when(owner.isOnline()).thenReturn(false);
        wave.run();
        verify(target, never()).damage(anyDouble(), any(Entity.class));
        assertEquals(1, finished.size());
    }

    @Test
    void interruptionDuringSpawnCannotLeaveAnUntrackedDisplay() {
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class))).thenAnswer(call -> {
            Consumer<ItemDisplay> initializer = call.getArgument(2);
            initializer.accept(display);
            wave.stop();
            return display;
        });
        start(UUID.randomUUID(), 4.5, 0.65);
        verify(display).remove();
        assertEquals(1, finished.size());
        wave.run();
        assertEquals(1, transforms.size());
    }

    private void start(UUID excludedTarget, double range, double speed) {
        wave = new MoonWave(plugin, owner, excludedTarget, 2.4, range, 2.8, speed, acceptedHit, finished::add);
        wave.start();
    }

    private Block solidBlock(Material material, BoundingBox collisionBox) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        VoxelShape shape = mock(VoxelShape.class);
        when(shape.getBoundingBoxes()).thenReturn(List.of(collisionBox));
        when(block.getCollisionShape()).thenReturn(shape);
        return block;
    }

    private LivingEntity target(double minX, double minZ, double maxX, double maxZ) {
        LivingEntity target = mock(LivingEntity.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.isValid()).thenReturn(true);
        when(target.getHealth()).thenReturn(20.0D);
        when(target.getBoundingBox()).thenReturn(new BoundingBox(minX, 0, minZ, maxX, 2, maxZ));
        targets.add(target);
        return target;
    }

    private void observeDamage(LivingEntity target, boolean cancelled, Runnable afterEvent) {
        doAnswer(call -> {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getDamager()).thenReturn(owner);
            when(event.getEntity()).thenReturn(target);
            when(event.isCancelled()).thenReturn(cancelled);
            new DamageFlow().capture(event);
            assertTrue(DamageFlow.isSecondaryDamage(event));
            afterEvent.run();
            return null;
        }).when(target).damage(2.4D, owner);
    }
}
