package pepin.pepeforge.weapons.rapier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.ActionBarHelper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class RapierListenerTest {
    @BeforeAll
    static void setUpServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Test
    void normalRightClickLeavesMainHandAndShieldInteractionsUntouched() {
        try (Fixture fixture = new Fixture(true)) {
            when(fixture.player.isSneaking()).thenReturn(false);
            PlayerInteractEvent main = fixture.click(EquipmentSlot.HAND);
            PlayerInteractEvent offhand = fixture.click(EquipmentSlot.OFF_HAND);
            fixture.listener.onRightClick(main);
            fixture.listener.onRightClick(offhand);
            verify(fixture.factory, never()).updateRapierMode(any(), any());
            verify(main, never()).setUseItemInHand(any());
            verify(offhand, never()).setUseItemInHand(any());
        }
    }

    @Test
    void shiftRightClickTogglesOnceAndSuppressesPairedShieldOnlyInSwitchTick() {
        try (Fixture fixture = new Fixture(true)) {
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            PlayerInteractEvent shield = fixture.click(EquipmentSlot.OFF_HAND);
            fixture.listener.onRightClick(shield);
            verify(fixture.factory).updateRapierMode(fixture.item, RapierMode.CUT);
            verify(shield).setUseItemInHand(Event.Result.DENY);
            verify(shield).setUseInteractedBlock(Event.Result.DENY);

            when(fixture.player.getTicksLived()).thenReturn(101);
            PlayerInteractEvent laterShield = fixture.click(EquipmentSlot.OFF_HAND);
            fixture.listener.onRightClick(laterShield);
            verify(laterShield, never()).setUseItemInHand(any());
        }
    }

    @Test
    void vanillaCancelledAirCanSwitchButExplicitItemDenialCannot() {
        try (Fixture fixture = new Fixture(true)) {
            PlayerInteractEvent vanillaAir = fixture.click(EquipmentSlot.HAND);
            when(vanillaAir.isCancelled()).thenReturn(true);
            when(vanillaAir.useInteractedBlock()).thenReturn(Event.Result.DENY);
            fixture.listener.onRightClick(vanillaAir);
            verify(fixture.factory).updateRapierMode(fixture.item, RapierMode.CUT);

            when(fixture.player.getTicksLived()).thenReturn(101);
            PlayerInteractEvent deniedAir = fixture.click(EquipmentSlot.HAND);
            when(deniedAir.useItemInHand()).thenReturn(Event.Result.DENY);
            fixture.listener.onRightClick(deniedAir);
            verify(fixture.factory, times(1)).updateRapierMode(any(), any());
            verify(deniedAir, never()).setUseItemInHand(any());
        }
    }

    @Test
    void cancelledBlockAndUnpairedOffhandDoNotSwitchOrResurrectInteraction() {
        try (Fixture fixture = new Fixture(true)) {
            PlayerInteractEvent deniedBlock = fixture.click(EquipmentSlot.HAND);
            when(deniedBlock.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(deniedBlock.isCancelled()).thenReturn(true);
            fixture.listener.onRightClick(deniedBlock);
            PlayerInteractEvent offhand = fixture.click(EquipmentSlot.OFF_HAND);
            fixture.listener.onRightClick(offhand);
            verify(fixture.factory, never()).updateRapierMode(any(), any());
            verify(deniedBlock, never()).setUseItemInHand(any());
            verify(offhand, never()).setUseItemInHand(any());
        }
    }

    @Test
    void quitAndStopReleaseDuplicateInputState() {
        try (Fixture fixture = new Fixture(true)) {
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(fixture.player);
            fixture.listener.onQuit(quit);
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            fixture.listener.stop();
            fixture.listener.start();
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            verify(fixture.factory, times(3)).updateRapierMode(fixture.item, RapierMode.CUT);
            fixture.listener.stop();
            clearInvocations(fixture.factory);
            fixture.listener.onRightClick(fixture.click(EquipmentSlot.HAND));
            verifyNoInteractions(fixture.factory);
        }
    }

    @Test
    void thrustCancelsVanillaSweepDamageAndCutModeLeavesItAlone() {
        try (Fixture fixture = new Fixture(false)) {
            EntityDamageByEntityEvent thrustSweep = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            fixture.listener.onDamage(thrustSweep);
            verify(thrustSweep).setCancelled(true);
            when(fixture.factory.getRapierMode(fixture.item)).thenReturn(RapierMode.CUT);
            EntityDamageByEntityEvent sweep = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            fixture.listener.onDamage(sweep);
            verify(sweep, never()).setCancelled(anyBoolean());
            verify(sweep, never()).setDamage(anyDouble());
        }
    }

    @Test
    void chargedFrontalThrustBoostsPrimaryDamageAgainstMobsAndPlayers() {
        try (Fixture fixture = new Fixture(true)) {
            EntityDamageByEntityEvent mobHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            fixture.listener.onDamage(mobHit);
            verify(mobHit).setDamage(12.0D);

            Player victim = mock(Player.class);
            fixture.configureVictim(victim);
            EntityDamageByEntityEvent playerHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(playerHit.getEntity()).thenReturn(victim);
            fixture.listener.onDamage(playerHit);
            verify(playerHit).setDamage(12.0D);
        }
    }

    @Test
    void disabledBonusLowChargeCutModeAndRearHitsDoNotBoostDamage() {
        try (Fixture fixture = new Fixture(false)) {
            EntityDamageByEntityEvent hit = fixture.damage(DamageCause.ENTITY_ATTACK);
            fixture.listener.onDamage(hit);
            verify(hit, never()).setDamage(anyDouble());
        }
        try (Fixture fixture = new Fixture(true)) {
            for (float charge : new float[]{0.899F, Float.NaN}) {
                when(fixture.player.getAttackCooldown()).thenReturn(charge);
                EntityDamageByEntityEvent hit = fixture.damage(DamageCause.ENTITY_ATTACK);
                fixture.listener.onDamage(hit);
                verify(hit, never()).setDamage(anyDouble());
            }
            when(fixture.player.getAttackCooldown()).thenReturn(1.0F);
            when(fixture.factory.getRapierMode(fixture.item)).thenReturn(RapierMode.CUT);
            EntityDamageByEntityEvent sweepHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            fixture.listener.onDamage(sweepHit);
            verify(sweepHit, never()).setDamage(anyDouble());

            when(fixture.factory.getRapierMode(fixture.item)).thenReturn(RapierMode.THRUST);
            when(fixture.player.getLocation()).thenReturn(new Location(fixture.world, 0, 0, -2));
            EntityDamageByEntityEvent rearHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            fixture.listener.onDamage(rearHit);
            verify(rearHit, never()).setDamage(anyDouble());
        }
    }

    @Test
    void cancelledInternalAndForeignRegionDamageNeverBoostOrReadVictimState() {
        try (Fixture fixture = new Fixture(true);
                MockedStatic<DamageFlow> damageFlow = mockStatic(DamageFlow.class)) {
            EntityDamageByEntityEvent cancelled = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(cancelled.isCancelled()).thenReturn(true);
            fixture.listener.onDamage(cancelled);
            EntityDamageByEntityEvent internal = fixture.damage(DamageCause.ENTITY_ATTACK);
            damageFlow.when(() -> DamageFlow.isSecondaryDamage(internal)).thenReturn(true);
            fixture.listener.onDamage(internal);
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.victim)).thenReturn(false);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim, never()).getLocation();
            verify(fixture.player, never()).getAttackCooldown();
        }
    }

    @Test
    void thrustStillCancelsSweepWhenVictimBelongsToAnotherRegion() {
        try (Fixture fixture = new Fixture(true)) {
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.victim)).thenReturn(false);
            EntityDamageByEntityEvent event = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            fixture.listener.onDamage(event);
            verify(event).setCancelled(true);
            verify(fixture.victim, never()).getLocation();
        }
    }

    @Test
    void frontalConeUsesVictimHorizontalFacingWithInclusiveSixtyDegreeEdges() {
        Location victim = new Location(null, 0, 0, 0, 0, 90);
        assertTrue(RapierListener.isInFrontalCone(victim, new Location(null, Math.sqrt(3), 100, 1), 120));
        assertTrue(RapierListener.isInFrontalCone(victim, new Location(null, -Math.sqrt(3), -100, 1), 120));
        assertFalse(RapierListener.isInFrontalCone(victim, new Location(null, 1.74, 0, 1), 120));
        assertFalse(RapierListener.isInFrontalCone(victim, new Location(null, 0, 0, -1), 120));
        assertFalse(RapierListener.isInFrontalCone(victim, new Location(null, 0, 10, 0), 120));
        victim.setYaw(90);
        assertTrue(RapierListener.isInFrontalCone(victim, new Location(null, -1, 0, 0), 120));
        assertFalse(RapierListener.isInFrontalCone(victim, new Location(null, 1, 0, 0), 120));
    }

    private static final class Fixture implements AutoCloseable {
        private final Player player = mock(Player.class);
        private final PlayerInventory inventory = mock(PlayerInventory.class);
        private final LivingEntity victim = mock(LivingEntity.class);
        private final World world = mock(World.class);
        private final ItemStack item = new ItemStack(Material.IRON_SWORD);
        private final ItemFactory factory = mock(ItemFactory.class);
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private final MockedStatic<ActionBarHelper> bars = mockStatic(ActionBarHelper.class);
        private final RapierListener listener;

        private Fixture(boolean frontalBonus) {
            PluginLang lang = mock(PluginLang.class);
            when(lang.text("messages.rapier.mode_changed")).thenReturn("Mode: {mode}");
            when(lang.text("messages.rapier.modes.cut")).thenReturn("Cut");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getTicksLived()).thenReturn(100);
            when(player.isSneaking()).thenReturn(true);
            when(player.isOnline()).thenReturn(true);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenReturn(new Location(world, 0, 0, 2));
            when(player.getAttackCooldown()).thenReturn(0.9F);
            when(inventory.getItemInMainHand()).thenReturn(item);
            when(factory.getRapierTier(item)).thenReturn(RapierTier.IRON);
            when(factory.getRapierMode(item)).thenReturn(RapierMode.THRUST);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);
            configureVictim(victim);
            YamlConfiguration config = new YamlConfiguration();
            config.set("mechanics.rapier.frontal_bonus_enabled", frontalBonus);
            listener = new RapierListener(factory, lang, RapierSettings.fromConfig(config));
            listener.start();
        }

        private void configureVictim(LivingEntity entity) {
            when(entity.isValid()).thenReturn(true);
            when(entity.getWorld()).thenReturn(world);
            when(entity.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        }

        private PlayerInteractEvent click(EquipmentSlot hand) {
            PlayerInteractEvent event = mock(PlayerInteractEvent.class);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(event.getPlayer()).thenReturn(player);
            when(event.getHand()).thenReturn(hand);
            when(event.useItemInHand()).thenReturn(Event.Result.DEFAULT);
            return event;
        }

        private EntityDamageByEntityEvent damage(DamageCause cause) {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getDamager()).thenReturn(player);
            when(event.getEntity()).thenReturn(victim);
            when(event.getCause()).thenReturn(cause);
            when(event.getDamage()).thenReturn(10.0D);
            return event;
        }

        @Override
        public void close() {
            listener.stop();
            bars.close();
            scheduler.close();
        }
    }
}
