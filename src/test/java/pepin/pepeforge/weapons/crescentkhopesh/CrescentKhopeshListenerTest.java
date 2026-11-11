package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.aura.AuraManager;
import pepin.pepeforge.util.combat.DamageEvents;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TemporaryShieldManager;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CrescentKhopeshListenerTest {
    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private final Player player = mock(Player.class);
    private final LivingEntity target = mock(LivingEntity.class);
    private final ItemFactory factory = mock(ItemFactory.class);
    private final TemporaryShieldManager shield = mock(TemporaryShieldManager.class);
    private final World world = mock(World.class);
    private final AuraManager auraManager = mock(AuraManager.class);
    private CrescentKhopeshListener listener;

    @BeforeEach
    void setup() {
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        plugin.getConfig().set("mechanics.crescent_khopesh.moonstrike_mode", "WAVE");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(ignored -> new Location(world, 0.0D, 64.0D, 0.0D));
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(factory.isCrescentKhopesh(null)).thenReturn(true);
        when(factory.isItemEnabled(CrescentKhopeshDefinition.ITEM_ID)).thenReturn(true);
        listener = spy(new CrescentKhopeshListener(plugin, factory, shield, auraManager));
        doNothing().when(listener).activate(any(Player.class), any(UUID.class));
    }

    @Test
    void defaultSweepCapturesSettingsAndGrantsOnlyAcceptedTargetShield() {
        String path = "mechanics.crescent_khopesh.";
        plugin.getConfig().set(path + "moonstrike_mode", null);
        plugin.getConfig().set(path + "sweep_width", 4.0D);
        plugin.getConfig().set(path + "sweep_range", 3.0D);
        java.util.List<Runnable> acceptedHits = new java.util.ArrayList<>();
        try (var animations = mockConstruction(MoonSweepAuraEffect.class);
                var attacks = mockConstruction(MoonSweep.class, (sweep, context) -> {
                    org.junit.jupiter.api.Assertions.assertEquals(target.getUniqueId(), context.arguments().get(2));
                    org.junit.jupiter.api.Assertions.assertEquals(2.4D, (double) context.arguments().get(3), 1.0E-6);
                    org.junit.jupiter.api.Assertions.assertEquals(4.0D, context.arguments().get(4));
                    org.junit.jupiter.api.Assertions.assertEquals(3.0D, context.arguments().get(5));
                    acceptedHits.add((Runnable) context.arguments().get(6));
                }); var waves = mockConstruction(MoonWave.class)) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.activate(player, target.getUniqueId());
            verify(shield).grant(player, "moonshield", 0.5D, 4.0D, 80);
            verify(player).playSound(any(Location.class), eq(org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_RESONATE),
                    eq(1.3F), eq(1.35F));
            verify(attacks.constructed().getFirst()).strike();
            verify(animations.constructed().getFirst()).addSweep(eq(player), any(Location.class), eq(4.0D), eq(3.0D));
            verify(auraManager).addOrExtendActiveAura(player, animations.constructed().getFirst(), 10);
            org.junit.jupiter.api.Assertions.assertTrue(waves.constructed().isEmpty());
            acceptedHits.getFirst().run();
            verify(shield, times(2)).grant(player, "moonshield", 0.5D, 4.0D, 80);
            listener.onQuit(new org.bukkit.event.player.PlayerQuitEvent(player, ""));
            acceptedHits.getFirst().run();
            verify(shield, times(2)).grant(player, "moonshield", 0.5D, 4.0D, 80);
            verify(auraManager).removeActiveAura(player, MoonSweepAuraEffect.class);
            verify(animations.constructed().getFirst()).stop();
        }
    }

    @Test
    void overlappingActivationsShareControllerAndModeSwitchPreservesWave() {
        String path = "mechanics.crescent_khopesh.";
        try (var animations = mockConstruction(MoonSweepAuraEffect.class);
                var attacks = mockConstruction(MoonSweep.class); var waves = mockConstruction(MoonWave.class)) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.activate(player, target.getUniqueId());
            MoonWave wave = waves.constructed().getFirst();
            plugin.getConfig().set(path + "moonstrike_mode", "SWEEP");
            listener.activate(player, target.getUniqueId());
            listener.activate(player, target.getUniqueId());
            org.junit.jupiter.api.Assertions.assertEquals(1, animations.constructed().size());
            org.junit.jupiter.api.Assertions.assertEquals(2, attacks.constructed().size());
            verify(animations.constructed().getFirst(), times(2)).addSweep(eq(player), any(Location.class), eq(3.0D), eq(2.0D));
            verify(wave, never()).stop();
            listener.stop();
            verify(wave).stop();
            verify(auraManager).removeActiveAura(player.getUniqueId(), MoonSweepAuraEffect.class);
            verify(animations.constructed().getFirst()).stop();
        }
    }

    @Test
    void interruptionDuringDisplaySpawnPreventsLateDamageAndAuraRegistration() {
        plugin.getConfig().set("mechanics.crescent_khopesh.moonstrike_mode", "SWEEP");
        try (var animations = mockConstruction(MoonSweepAuraEffect.class, (effect, context) -> {
            when(effect.addSweep(eq(player), any(Location.class), anyDouble(), anyDouble())).thenAnswer(call -> {
                listener.onQuit(new org.bukkit.event.player.PlayerQuitEvent(player, ""));
                return false;
            });
        }); var attacks = mockConstruction(MoonSweep.class)) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.activate(player, target.getUniqueId());
            org.junit.jupiter.api.Assertions.assertTrue(attacks.constructed().isEmpty());
            verify(auraManager, never()).addOrExtendActiveAura(any(), any(), anyInt());
            verify(animations.constructed().getFirst(), atLeastOnce()).stop();
        }
    }

    @Test
    void configuredHitRequirementControlsActivation() {
        plugin.getConfig().set("mechanics.crescent_khopesh.hits_required", 5);
        for (int count = 0; count < 4; count++) {
            listener.onDamage(hit());
        }
        verify(listener, never()).activate(any(), any());
        listener.onDamage(hit());
        verify(listener).activate(player, target.getUniqueId());
    }

    @Test
    void activationCapturesConfiguredWaveAndShieldValuesWhileDamageAndGainStayFixed() {
        var config = plugin.getConfig();
        String path = "mechanics.crescent_khopesh.";
        config.set(path + "wave_range", 8.0D);
        config.set(path + "wave_width", 5.0D);
        config.set(path + "wave_speed", 2.0D);
        config.set(path + "shield_absorption", 6.0D);
        config.set(path + "wave_damage_multiplier", 5.0D);
        config.set(path + "shield_absorption_per_hit", 20.0D);
        java.util.List<Runnable> acceptedHits = new java.util.ArrayList<>();
        try (var construction = mockConstruction(MoonWave.class, (wave, context) -> {
            org.junit.jupiter.api.Assertions.assertEquals(2.4D, (double) context.arguments().get(3), 1.0E-6);
            org.junit.jupiter.api.Assertions.assertEquals(8.0D, context.arguments().get(4));
            org.junit.jupiter.api.Assertions.assertEquals(5.0D, context.arguments().get(5));
            org.junit.jupiter.api.Assertions.assertEquals(2.0D, context.arguments().get(6));
            acceptedHits.add((Runnable) context.arguments().get(7));
        })) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.activate(player, target.getUniqueId());
            verify(shield).grant(player, "moonshield", 0.5D, 6.0D, 80);
            config.set(path + "shield_absorption", 10.0D);
            acceptedHits.getFirst().run();
            verify(shield, times(2)).grant(player, "moonshield", 0.5D, 6.0D, 80);
            listener.stop();
        }
    }

    @Test
    void everyThirdAcceptedMeleeHitActivatesExactlyOnce() {
        listener.onDamage(hit());
        listener.onDamage(hit());
        verify(listener, never()).activate(any(), any());
        listener.onDamage(hit());
        verify(listener).activate(player, target.getUniqueId());
        listener.onDamage(hit());
        listener.onDamage(hit());
        listener.onDamage(hit());
        verify(listener, times(2)).activate(player, target.getUniqueId());
    }

    @Test
    void cancelledHitsSweepingAndSecondaryDamageCannotAdvanceCounter() {
        listener.onDamage(hit());
        EntityDamageByEntityEvent cancelled = hit();
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onDamage(cancelled);
        EntityDamageByEntityEvent sweep = hit();
        when(sweep.getCause()).thenReturn(DamageCause.ENTITY_SWEEP_ATTACK);
        listener.onDamage(sweep);

        when(target.isValid()).thenReturn(true);
        when(target.getHealth()).thenReturn(20.0D);
        DamageEvents events = new DamageEvents(new DamageFlow(), listener);
        EntityDamageByEntityEvent secondary = hit();
        doAnswer(call -> {
            events.dispatch(secondary);
            return null;
        }).when(target).damage(2.4D, player);
        DamageFlow.damage(target, 2.4D, player);
        verify(secondary, never()).setDamage(anyDouble());
        listener.onDamage(hit());
        verify(listener, never()).activate(any(), any());
        listener.onDamage(hit());
        verify(listener).activate(player, target.getUniqueId());
    }

    @Test
    void disabledItemAndStoppedModuleCannotTrigger() {
        when(factory.isItemEnabled(CrescentKhopeshDefinition.ITEM_ID)).thenReturn(false);
        listener.onDamage(hit());
        when(factory.isItemEnabled(CrescentKhopeshDefinition.ITEM_ID)).thenReturn(true);
        listener.onDamage(hit());
        listener.onDamage(hit());
        listener.stop();
        listener.onDamage(hit());
        verify(listener, never()).activate(any(), any());
    }

    @Test
    void activationCountsMainTargetBeforeWaveAndEachAcceptedWaveHitUntilStop() {
        java.util.List<Runnable> acceptedHits = new java.util.ArrayList<>();
        try (var construction = mockConstruction(MoonWave.class, (wave, context) -> {
            acceptedHits.add((Runnable) context.arguments().get(7));
            org.junit.jupiter.api.Assertions.assertEquals(2.4D,
                    (double) context.arguments().get(3), 1.0E-6);
        })) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.activate(player, target.getUniqueId());
            var order = inOrder(shield, construction.constructed().getFirst());
            order.verify(shield).grant(player, "moonshield", 0.5D, 4.0D, 80);
            order.verify(construction.constructed().getFirst()).start();
            acceptedHits.getFirst().run();
            verify(shield, times(2)).grant(player, "moonshield", 0.5D, 4.0D, 80);
            listener.stop();
            verify(construction.constructed().getFirst()).stop();
            acceptedHits.getFirst().run();
            verifyNoMoreInteractions(shield);
        }
    }

    @Test
    void mainTargetGrantsShieldOnlyOnTheThirdAcceptedPrimaryHit() {
        try (var construction = mockConstruction(MoonWave.class)) {
            doCallRealMethod().when(listener).activate(any(), any());
            listener.onDamage(hit());
            listener.onDamage(hit());
            EntityDamageByEntityEvent cancelled = hit();
            when(cancelled.isCancelled()).thenReturn(true);
            listener.onDamage(cancelled);
            verifyNoInteractions(shield);
            listener.onDamage(hit());
            verify(shield).grant(player, "moonshield", 0.5D, 4.0D, 80);
            verify(construction.constructed().getFirst()).start();
            listener.onDamage(hit());
            verifyNoMoreInteractions(shield);
            listener.stop();
        }
    }

    @Test
    void playerCleanupRemovesOnlyMoonshieldSource() {
        listener.onQuit(new org.bukkit.event.player.PlayerQuitEvent(player, ""));
        verify(shield).clearSource(player, "moonshield");
        verifyNoMoreInteractions(shield);
    }

    @Test
    void moduleOwnsOnlyItsShieldSourceAndMigratesEvenWhenItemIsDisabled() {
        pepin.pepeforge.PepeForgePlugin ownerPlugin = mock(pepin.pepeforge.PepeForgePlugin.class);
        org.bukkit.Server server = mock(org.bukkit.Server.class);
        org.bukkit.plugin.PluginManager pluginManager = mock(org.bukkit.plugin.PluginManager.class);
        when(ownerPlugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(factory.isItemEnabled(CrescentKhopeshDefinition.ITEM_ID)).thenReturn(false);
        try (var recipes = mockConstruction(CrescentKhopeshRecipes.class);
                var discovery = mockConstruction(CrescentKhopeshRecipeDiscovery.class);
                var legacy = mockConstruction(Moonshield.class);
                var combat = mockConstruction(CrescentKhopeshListener.class)) {
            CrescentKhopeshModule module = new CrescentKhopeshModule(ownerPlugin, factory, shield, auraManager);
            module.onEnable();
            Moonshield migration = legacy.constructed().getFirst();
            verify(pluginManager).registerEvents(migration, ownerPlugin);
            verify(migration).cleanupOnlinePlayers();
            module.onDisable();
            verify(combat.constructed().getFirst()).stop();
            verify(shield).clearSource("moonshield");
            verify(shield, never()).stop();
            verify(migration).stop();
            verify(recipes.constructed().getFirst()).unregisterAll();
        }
    }

    @Test
    void daylightReducesPrimaryDamageWithoutChangingHitAcceptance() {
        when(world.getTime()).thenReturn(6000L);
        EntityDamageByEntityEvent event = hit();
        new DamageEvents(new DamageFlow(), listener).dispatch(event);
        verify(event).setDamage(5.0D);
    }

    @Test
    void openSkyMoonlightAddsTwoPrimaryDamage() {
        when(world.getTime()).thenReturn(18000L);
        Location location = mock(Location.class);
        Block block = mock(Block.class);
        when(player.getLocation()).thenReturn(location);
        when(location.getBlock()).thenReturn(block);
        when(block.getLightFromSky()).thenReturn((byte) 15);
        EntityDamageByEntityEvent event = hit();
        new DamageEvents(new DamageFlow(), listener).dispatch(event);
        verify(event).setDamage(8.0D);
    }

    @Test
    void cancelledHitsAndOtherWeaponsDoNotReceiveCrescentDamageModifier() {
        EntityDamageByEntityEvent cancelled = hit();
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onPrimaryDamage(cancelled);
        verify(cancelled, never()).setDamage(anyDouble());

        when(factory.isCrescentKhopesh(null)).thenReturn(false);
        EntityDamageByEntityEvent otherWeapon = hit();
        listener.onPrimaryDamage(otherWeapon);
        verify(otherWeapon, never()).setDamage(anyDouble());
    }

    private EntityDamageByEntityEvent hit() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(target);
        when(event.getCause()).thenReturn(DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(6.0D);
        return event;
    }
}
