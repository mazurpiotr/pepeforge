package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MoonSweepAuraEffectTest {
    private static JavaPlugin plugin;
    private final World world = mock(World.class);
    private final Player player = mock(Player.class);
    private final List<ItemDisplay> displays = new ArrayList<>();
    private final List<List<ItemStack>> frames = new ArrayList<>();
    private MoonSweepAuraEffect effect;
    private long gameTick;

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
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0, 0, -80));
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getGameTime()).thenAnswer(call -> gameTick);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class))).thenAnswer(call -> {
            ItemDisplay display = mock(ItemDisplay.class);
            when(display.isValid()).thenReturn(true);
            List<ItemStack> frameList = new ArrayList<>();
            doAnswer(frame -> {
                frameList.add(frame.getArgument(0));
                return null;
            }).when(display).setItemStack(any(ItemStack.class));
            displays.add(display);
            frames.add(frameList);
            Consumer<ItemDisplay> initializer = call.getArgument(2);
            initializer.accept(display);
            return display;
        });
        try (var itemModels = mockStatic(ItemMetaCompat.class)) {
            itemModels.when(() -> ItemMetaCompat.setItemModel(any(ItemMeta.class), any(NamespacedKey.class)))
                    .thenAnswer(call -> {
                        ItemMeta meta = call.getArgument(0);
                        NamespacedKey model = call.getArgument(1);
                        meta.setDisplayName(model.toString());
                        return null;
                    });
            effect = new MoonSweepAuraEffect(plugin);
        }
    }

    @Test
    void playsFiveFramesForTwoTicksEachAndRemovesAtTenTicks() {
        assertTrue(effect.addSweep(player, player.getLocation(), 3, 2));
        for (int tick = 1; tick <= 9; tick++) {
            advanceTick();
            assertEquals(1 + tick / 2, frames.getFirst().size());
            verify(displays.getFirst(), never()).remove();
        }
        for (int frame = 0; frame < 5; frame++) {
            assertEquals("pepeforge:moon_sweep_" + frame,
                    frames.getFirst().get(frame).getItemMeta().getDisplayName());
        }
        advanceTick();
        assertTrue(effect.isExpired());
        verify(displays.getFirst()).remove();
        effect.stop();
        effect.stop();
        verify(displays.getFirst()).remove();
    }

    @Test
    void overlappingActivationsStartAtFrameZeroAndExpireIndependently() {
        effect.addSweep(player, player.getLocation(), 3, 2);
        for (int tick = 0; tick < 4; tick++) {
            advanceTick();
        }
        effect.addSweep(player, player.getLocation(), 4, 3);
        assertEquals(3, frames.getFirst().size());
        assertEquals(1, frames.get(1).size());
        assertEquals("pepeforge:moon_sweep_0", frames.get(1).getFirst().getItemMeta().getDisplayName());
        for (int tick = 0; tick < 6; tick++) {
            advanceTick();
        }
        verify(displays.getFirst()).remove();
        verify(displays.get(1), never()).remove();
        assertFalse(effect.isExpired());
        for (int tick = 0; tick < 4; tick++) {
            advanceTick();
        }
        assertTrue(effect.isExpired());
        verify(displays.get(1)).remove();
    }

    @ParameterizedTest
    @ValueSource(floats = {0, 90, 180, 270})
    void modelRightPointsForwardAndPlaneKeepsCapturedOrientation(float yaw) {
        when(player.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0, yaw, -80));
        effect.addSweep(player, player.getLocation(), 3, 2);
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class);
        verify(world).spawn(location.capture(), eq(ItemDisplay.class), any(Consumer.class));
        double radians = Math.toRadians(yaw);
        assertEquals(-Math.sin(radians), location.getValue().getX(), 1.0E-6);
        assertEquals(Math.cos(radians), location.getValue().getZ(), 1.0E-6);
        assertEquals(65.3, location.getValue().getY(), 1.0E-6);
        ArgumentCaptor<Transformation> transformation = ArgumentCaptor.forClass(Transformation.class);
        verify(displays.getFirst()).setTransformation(transformation.capture());
        Transformation transform = transformation.getValue();
        assertEquals(new Vector3f(3, 2, 1), transform.getScale());
        // The model's fixed Z rotation maps its texture's right direction to +Y.
        Vector3f modelRight = new Vector3f(0, 1, 0).rotate(transform.getLeftRotation());
        assertEquals(-Math.sin(radians), modelRight.x, 1.0E-6);
        assertEquals(0, modelRight.y, 1.0E-6);
        assertEquals(Math.cos(radians), modelRight.z, 1.0E-6);
        when(player.getLocation()).thenReturn(new Location(world, 100, 20, 100, yaw + 90, 80));
        advanceTick();
        advanceTick();
        verify(displays.getFirst(), times(1)).setTransformation(any(Transformation.class));
        verify(displays.getFirst(), never()).teleport(any(Location.class));
        effect.stop();
    }

    @Test
    void stopCleansEveryDisplayOnceAndPreventsNewActivations() {
        effect.addSweep(player, player.getLocation(), 3, 2);
        effect.addSweep(player, player.getLocation(), 3, 2);
        effect.stop();
        effect.stop();
        effect.tick(player);
        assertFalse(effect.addSweep(player, player.getLocation(), 3, 2));
        assertTrue(effect.isExpired());
        displays.forEach(display -> verify(display).remove());
    }

    @Test
    void worldChangeRemovesTheCapturedDisplay() {
        effect.addSweep(player, player.getLocation(), 3, 2);
        when(player.getWorld()).thenReturn(mock(World.class));
        effect.tick(player);
        assertTrue(effect.isExpired());
        verify(displays.getFirst()).remove();
    }

    @Test
    void unloadedChunksPreventSpawning() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        assertFalse(effect.addSweep(player, player.getLocation(), 3, 2));
        verify(world, never()).spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class));
    }

    @Test
    void foreignChunksPreventChunkReadsAndSpawning() {
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(eq(world),
                    anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
            assertFalse(effect.addSweep(player, player.getLocation(), 3, 2));
            verify(world, never()).isChunkLoaded(anyInt(), anyInt());
            verify(world, never()).spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class));
        }
    }

    @Test
    void cancellationDuringSpawnCannotLeakADisplay() {
        doAnswer(call -> {
            ItemDisplay display = mock(ItemDisplay.class);
            when(display.isValid()).thenReturn(true);
            displays.add(display);
            Consumer<ItemDisplay> initializer = call.getArgument(2);
            initializer.accept(display);
            effect.stop();
            return display;
        }).when(world).spawn(any(Location.class), eq(ItemDisplay.class), any(Consumer.class));
        assertFalse(effect.addSweep(player, player.getLocation(), 3, 2));
        verify(displays.getFirst()).remove();
    }

    @Test
    void sameTickAndDuplicateCallbacksDoNotAdvanceFramesOrShortenLifetime() {
        effect.addSweep(player, player.getLocation(), 3, 2);
        advanceTick();
        gameTick++;
        effect.addSweep(player, player.getLocation(), 3, 2);
        effect.tick(player);
        effect.tick(player);
        assertEquals(2, frames.getFirst().size());
        assertEquals(1, frames.get(1).size());
        for (int tick = 3; tick <= 11; tick++) {
            advanceTick();
            effect.tick(player);
        }
        verify(displays.getFirst()).remove();
        verify(displays.get(1), never()).remove();
        assertEquals(5, frames.get(1).size());
        advanceTick();
        verify(displays.get(1)).remove();
        assertTrue(effect.isExpired());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void foreignCleanupDoesNotAccessTheDisplayOrScheduleWhenPluginDisabledOrServerStopping(boolean stopping) {
        JavaPlugin lifecyclePlugin = mock(JavaPlugin.class);
        when(lifecyclePlugin.isEnabled()).thenReturn(stopping);
        MoonSweepAuraEffect lifecycleEffect = new MoonSweepAuraEffect(lifecyclePlugin);
        assertTrue(lifecycleEffect.addSweep(player, player.getLocation(), 3, 2));
        ItemDisplay display = displays.getFirst();
        clearInvocations(display);
        try (var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(display)).thenReturn(false);
            scheduler.when(SchedulerCompat::isServerStopping).thenReturn(stopping);
            lifecycleEffect.stop();
            scheduler.verify(() -> SchedulerCompat.runForEntity(eq(display), eq(lifecyclePlugin), any()), never());
            verify(display, never()).isValid();
            verify(display, never()).remove();
        }
        assertTrue(lifecycleEffect.isExpired());
    }

    private void advanceTick() {
        gameTick++;
        effect.tick(player);
    }
}
