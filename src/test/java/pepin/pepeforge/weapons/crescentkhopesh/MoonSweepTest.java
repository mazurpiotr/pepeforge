package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MoonSweepTest {
    private final World world = mock(World.class);
    private final Player owner = mock(Player.class);
    private final List<Entity> targets = new ArrayList<>();
    private final Runnable acceptedHit = mock(Runnable.class);
    private final AtomicBoolean active = new AtomicBoolean(true);

    @BeforeAll
    static void createServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void destroyServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setup() {
        when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
        when(owner.getWorld()).thenReturn(world);
        when(owner.isOnline()).thenReturn(true);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getNearbyEntities(any(BoundingBox.class))).thenAnswer(call -> targets);
    }

    @Test
    void immediateSweepIsUniqueExcludesPrimaryAndOnlyAcceptedEventsGrantShield() {
        LivingEntity original = target(0, 0.6);
        LivingEntity accepted = target(0, 1);
        LivingEntity cancelled = target(0.4, 1);
        LivingEntity unobserved = target(-0.4, 1);
        targets.add(accepted);
        observeDamage(accepted, false, () -> { });
        observeDamage(cancelled, true, () -> { });
        MoonSweep sweep = sweep(original.getUniqueId());
        sweep.strike();
        sweep.strike();
        verify(original, never()).damage(anyDouble(), any(Entity.class));
        verify(accepted).damage(2.4D, owner);
        verify(cancelled).damage(2.4D, owner);
        verify(unobserved).damage(2.4D, owner);
        verify(acceptedHit).run();
        for (LivingEntity target : List.of(original, accepted, cancelled, unobserved)) {
            verify(target, never()).setNoDamageTicks(anyInt());
            verify(target, never()).setVelocity(any());
        }
    }

    @Test
    void eachTargetHasItsOwnShapeRayAndOneBlockedTargetDoesNotBlockAnother() {
        LivingEntity left = target(-0.8, 1);
        LivingEntity right = target(0.8, 1);
        when(world.rayTraceBlocks(any(Location.class), any(Vector.class), anyDouble(),
                eq(FluidCollisionMode.NEVER), eq(true))).thenAnswer(call -> {
                    Vector direction = call.getArgument(1);
                    return direction.getX() < 0 ? new RayTraceResult(new Vector(-0.25, 1.3, 0.5)) : null;
                });
        sweep(UUID.randomUUID()).strike();
        verify(left, never()).damage(anyDouble(), any(Entity.class));
        verify(right).damage(2.4D, owner);
        verify(world, times(2)).rayTraceBlocks(any(Location.class), any(Vector.class), anyDouble(),
                eq(FluidCollisionMode.NEVER), eq(true));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"lower slab, 0, 0.5, false", "upper slab, 0.5, 1, false",
        "single block, 0, 1, false", "wall, 0, 2, true", "fence, 0, 1.5, true"})
    void torsoRayPassesAboveLowShapesAndStopsAtShapesReachingItsHeight(
            String shapeName, double minY, double maxY, boolean blocked) {
        LivingEntity target = target(0, 1);
        BoundingBox shape = new BoundingBox(-0.5, minY, 0.35, 0.5, maxY, 0.65);
        when(world.rayTraceBlocks(any(Location.class), any(Vector.class), anyDouble(),
                eq(FluidCollisionMode.NEVER), eq(true))).thenAnswer(call -> {
                    Location start = call.getArgument(0);
                    Vector direction = call.getArgument(1);
                    double distance = call.getArgument(2);
                    return shape.rayTrace(start.toVector(), direction, distance);
                });
        sweep(UUID.randomUUID()).strike();
        verify(target, times(blocked ? 0 : 1)).damage(2.4D, owner);
    }

    @Test
    void lowTargetAndRearTargetDoNotCauseDamageOrBlockRays() {
        LivingEntity low = target(0, 1);
        when(low.getBoundingBox()).thenReturn(new BoundingBox(-0.3, 0, 0.7, 0.3, 1, 1.3));
        LivingEntity rear = target(0, -1);
        LivingEntity tall = target(0, 1);
        sweep(UUID.randomUUID()).strike();
        verify(low, never()).damage(anyDouble(), any(Entity.class));
        verify(rear, never()).damage(anyDouble(), any(Entity.class));
        verify(tall).damage(2.4D, owner);
        verify(world).rayTraceBlocks(any(Location.class), any(Vector.class), anyDouble(),
                eq(FluidCollisionMode.NEVER), eq(true));
    }

    @Test
    void activationPoseIsCapturedAndRayTargetsNearestIntersectionAtTorsoHeight() {
        LivingEntity target = target(0.8, 1.2);
        Location pose = new Location(world, 0, 0, 0, 0, -85);
        MoonSweep sweep = new MoonSweep(owner, pose, UUID.randomUUID(), 2.4, 3, 2, acceptedHit, active::get);
        pose.setYaw(180);
        pose.setX(30);
        sweep.strike();
        ArgumentCaptor<Location> start = ArgumentCaptor.forClass(Location.class);
        ArgumentCaptor<Vector> ray = ArgumentCaptor.forClass(Vector.class);
        ArgumentCaptor<Double> distance = ArgumentCaptor.forClass(Double.class);
        verify(world).rayTraceBlocks(start.capture(), ray.capture(), distance.capture(),
                eq(FluidCollisionMode.NEVER), eq(true));
        assertEquals(new Vector(0, 1.3, 0), start.getValue().toVector());
        Vector contact = start.getValue().toVector().add(ray.getValue().multiply(distance.getValue()));
        assertEquals(0.5, contact.getX(), 1.0E-9);
        assertEquals(0.9, contact.getZ(), 1.0E-9);
        verify(target).damage(2.4D, owner);
    }

    @Test
    void interruptionDuringDamagePreventsShieldAndLaterTargets() {
        LivingEntity first = target(0, 1);
        LivingEntity second = target(0.4, 1);
        observeDamage(first, false, () -> active.set(false));
        sweep(UUID.randomUUID()).strike();
        verify(first).damage(2.4D, owner);
        verify(second, never()).damage(anyDouble(), any(Entity.class));
        verify(acceptedHit, never()).run();
    }

    @Test
    void worldChangeDuringDamagePreventsShieldAndLaterTargets() {
        LivingEntity first = target(0, 1);
        LivingEntity second = target(0.4, 1);
        observeDamage(first, false, () -> when(owner.getWorld()).thenReturn(mock(World.class)));
        sweep(UUID.randomUUID()).strike();
        verify(second, never()).damage(anyDouble(), any(Entity.class));
        verify(acceptedHit, never()).run();
    }

    @Test
    void unloadedChunksPreventTargetQueriesAndRays() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        sweep(UUID.randomUUID()).strike();
        verify(world, never()).getNearbyEntities(any(BoundingBox.class));
        verify(world, never()).rayTraceBlocks(any(), any(), anyDouble(), any(), anyBoolean());
    }

    @Test
    void foreignChunksPreventChunkAndTargetReads() {
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(eq(world),
                    anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
            sweep(UUID.randomUUID()).strike();
            verify(world, never()).isChunkLoaded(anyInt(), anyInt());
            verify(world, never()).getNearbyEntities(any(BoundingBox.class));
        }
    }

    @Test
    void foreignTargetIsSkippedBeforeHitboxReads() {
        LivingEntity target = target(0, 1);
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(target)).thenReturn(false);
            sweep(UUID.randomUUID()).strike();
            verify(target, never()).getBoundingBox();
            verify(target, never()).damage(anyDouble(), any(Entity.class));
        }
    }

    @Test
    void chunksAreRecheckedAfterDamageCallbacksBeforeAnotherTargetOrRayIsRead() {
        LivingEntity first = target(0, 1);
        LivingEntity second = target(0.4, 1);
        observeDamage(first, false, () -> when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false));
        sweep(UUID.randomUUID()).strike();
        verify(acceptedHit).run();
        verify(second, never()).getBoundingBox();
        verify(second, never()).damage(anyDouble(), any(Entity.class));
    }

    private MoonSweep sweep(UUID excludedTarget) {
        return new MoonSweep(owner, new Location(world, 0, 0, 0, 0, 0), excludedTarget,
                2.4, 3, 2, acceptedHit, active::get);
    }

    private LivingEntity target(double x, double z) {
        LivingEntity target = mock(LivingEntity.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.isValid()).thenReturn(true);
        when(target.getHealth()).thenReturn(20.0D);
        when(target.getBoundingBox()).thenReturn(new BoundingBox(x - 0.3, 0, z - 0.3, x + 0.3, 2, z + 0.3));
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
