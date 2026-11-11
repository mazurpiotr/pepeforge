package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MoonshieldTest {
    private static JavaPlugin plugin;
    private final Player player = mock(Player.class);
    private final AttributeInstance attribute = mock(AttributeInstance.class);
    private final Map<NamespacedKey, AttributeModifier> modifiers = new LinkedHashMap<>();
    private final AtomicReference<Double> absorption = new AtomicReference<>(0.0D);
    private MockedStatic<SchedulerCompat> scheduler;
    private Moonshield migration;

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
    void preparePlayer() {
        when(player.getAttribute(Attribute.MAX_ABSORPTION)).thenReturn(attribute);
        when(player.getAbsorptionAmount()).thenAnswer(call -> absorption.get());
        doAnswer(call -> {
            absorption.set(call.getArgument(0));
            return null;
        }).when(player).setAbsorptionAmount(anyDouble());
        when(attribute.getModifiers()).thenAnswer(call -> new ArrayList<>(modifiers.values()));
        when(attribute.getValue()).thenAnswer(call -> attribute.getBaseValue() + modifiers.values().stream()
                .mapToDouble(AttributeModifier::getAmount).sum());
        doAnswer(call -> {
            AttributeModifier modifier = call.getArgument(0);
            modifiers.remove(modifier.getKey());
            return null;
        }).when(attribute).removeModifier(any(AttributeModifier.class));
        scheduler = mockStatic(SchedulerCompat.class);
        scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(player)).thenReturn(true);
        migration = new Moonshield(plugin);
    }

    @AfterEach
    void releaseScheduler() {
        scheduler.close();
    }

    @Test
    void removesAllLegacyCapacityWithoutGrantingNativeAbsorption() {
        legacyModifier();
        legacyModifier();
        absorption.set(3.0D);
        migration.cleanup(player);
        assertTrue(modifiers.isEmpty());
        assertEquals(0.0D, absorption.get());
        verify(attribute, never()).addModifier(any());
    }

    @Test
    void goldenAppleCapacityAndPotionEffectSurviveMigration() {
        legacyModifier();
        AttributeModifier goldenApple = modifier(NamespacedKey.minecraft("golden_apple"), 4.0D);
        absorption.set(5.0D);
        migration.cleanup(player);
        assertEquals(List.of(goldenApple), new ArrayList<>(modifiers.values()));
        assertEquals(4.0D, absorption.get());
        verify(player, never()).removePotionEffect(any());
        verify(player, never()).addPotionEffect(any());
    }

    @Test
    void migrationNeverRefillsPartiallyDepletedAppleAbsorption() {
        legacyModifier();
        modifier(NamespacedKey.minecraft("golden_apple"), 4.0D);
        absorption.set(0.5D);
        migration.cleanup(player);
        assertEquals(0.5D, absorption.get());
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void matchingPrefixInAnotherNamespaceAndOtherPluginModifiersAreUntouched() {
        AttributeModifier foreign = modifier(new NamespacedKey("other", "crescent_khopesh_moonshield_legacy"), 4.0D);
        AttributeModifier unrelated = modifier(new NamespacedKey(plugin, "unrelated_shield"), 2.0D);
        absorption.set(5.0D);
        migration.cleanup(player);
        assertEquals(List.of(foreign, unrelated), new ArrayList<>(modifiers.values()));
        assertEquals(5.0D, absorption.get());
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    @Test
    void preservesBaseCapacityAndIsIdempotent() {
        legacyModifier();
        when(attribute.getBaseValue()).thenReturn(2.0D);
        absorption.set(3.0D);
        migration.cleanup(player);
        migration.cleanup(player);
        assertEquals(2.0D, absorption.get());
        verify(player).setAbsorptionAmount(2.0D);
    }

    @Test
    void joinCleansPersistedLegacyModifiers() {
        legacyModifier();
        absorption.set(2.0D);
        migration.onJoin(new PlayerJoinEvent(player, ""));
        assertTrue(modifiers.isEmpty());
        assertEquals(0.0D, absorption.get());
    }

    @Test
    void onlinePlayerMigrationIncludesPlayersAlreadyConnectedAtReload() {
        Player online = MockBukkit.getMock().addPlayer();
        Moonshield tracked = spy(migration);
        doNothing().when(tracked).cleanup(any(Player.class));
        tracked.cleanupOnlinePlayers();
        verify(tracked).cleanup(online);
    }

    @Test
    void cleanupWaitsForPlayerOwnershipAndStoppedMigrationCannotRunLater() {
        legacyModifier();
        scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(player)).thenReturn(false);
        AtomicReference<Runnable> deferred = new AtomicReference<>();
        scheduler.when(() -> SchedulerCompat.runForPlayer(eq(player), eq(plugin), any(Runnable.class)))
                .thenAnswer(call -> {
                    deferred.set(call.getArgument(2));
                    return null;
                });
        migration.cleanup(player);
        verify(player, never()).getAttribute(any());
        assertNotNull(deferred.get());
        migration.stop();
        scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(player)).thenReturn(true);
        deferred.get().run();
        assertEquals(1, modifiers.size());
        verify(player, never()).getAttribute(any());
    }

    @Test
    void missingAbsorptionAttributeIsSafe() {
        when(player.getAttribute(Attribute.MAX_ABSORPTION)).thenReturn(null);
        assertDoesNotThrow(() -> migration.cleanup(player));
        verify(player, never()).setAbsorptionAmount(anyDouble());
    }

    private AttributeModifier legacyModifier() {
        return modifier(new NamespacedKey(plugin, "crescent_khopesh_moonshield_" + UUID.randomUUID()), 4.0D);
    }

    private AttributeModifier modifier(NamespacedKey key, double amount) {
        AttributeModifier modifier = new AttributeModifier(key, amount,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        modifiers.put(key, modifier);
        return modifier;
    }
}
