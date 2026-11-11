package pepin.pepeforge.weapons.obsidianglaive.compat;

import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlaiveCombatAdapterTest {
    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void paperSnapshotSurvivesCooldownResetAndCanOnlyBeConsumedForItsOriginalHit() {
        try (Fixture f = new Fixture()) {
            PaperGlaiveCombatAdapter adapter = new PaperGlaiveCombatAdapter(f.plugin, f.factory, null);
            adapter.onPreAttack(f.preAttack());
            when(f.player.getAttackCooldown()).thenReturn(0.0F);
            GlaiveCombatAdapter.Attack captured = adapter.capture(f.player, f.target);
            assertTrue(captured.sweepAllowed());
            assertEquals(4.5D, captured.sweepDamage(f.target));
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());

            when(f.player.getAttackCooldown()).thenReturn(1.0F);
            adapter.onPreAttack(f.preAttack());
            when(f.player.getTicksLived()).thenReturn(101);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            adapter.onPreAttack(f.preAttack());
            LivingEntity other = mock(LivingEntity.class);
            f.configureTarget(other);
            assertFalse(adapter.capture(f.player, other).sweepAllowed());
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
        }
    }

    @Test
    void cancelledPreAttacksAndClearedOrStoppedProvidersCannotAuthorizeSweeps() {
        try (Fixture f = new Fixture()) {
            PaperGlaiveCombatAdapter adapter = new PaperGlaiveCombatAdapter(f.plugin, f.factory, null);
            PrePlayerAttackEntityEvent cancelled = f.preAttack();
            when(cancelled.isCancelled()).thenReturn(true);
            adapter.onPreAttack(cancelled);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            adapter.onPreAttack(f.preAttack());
            adapter.clear(f.player);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            adapter.onPreAttack(f.preAttack());
            adapter.stop();
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
        }
    }

    @Test
    void fallbackSweepRequiresStrictChargeGroundedNonSprintAndSlowMovement() {
        try (Fixture f = new Fixture()) {
            BukkitGlaiveCombatAdapter adapter = new BukkitGlaiveCombatAdapter(f.plugin, f.factory, null);
            when(f.player.getAttackCooldown()).thenReturn(0.9F);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            when(f.player.getAttackCooldown()).thenReturn(1.0F);
            when(f.player.isOnGround()).thenReturn(false);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            when(f.player.isOnGround()).thenReturn(true);
            when(f.player.isSprinting()).thenReturn(true);
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            when(f.player.isSprinting()).thenReturn(false);
            adapter.onMove(new PlayerMoveEvent(f.player, new Location(f.world, 0, 0, 0),
                    new Location(f.world, 0.3, 0, 0)));
            assertFalse(adapter.capture(f.player, f.target).sweepAllowed());
            adapter.onMove(new PlayerMoveEvent(f.player, new Location(f.world, 0, 0, 0),
                    new Location(f.world, 0.1, 0, 0)));
            assertTrue(adapter.capture(f.player, f.target).sweepAllowed());
        }
    }

    @Test
    void fallbackDamageUsesSweepRatioAndIndependentTargetEnchantments() {
        try (Fixture f = new Fixture()) {
            f.item.addUnsafeEnchantment(Enchantment.SMITE, 2);
            BukkitGlaiveCombatAdapter adapter = new BukkitGlaiveCombatAdapter(f.plugin, f.factory, null);
            GlaiveCombatAdapter.Attack attack = adapter.capture(f.player, f.target);
            assertEquals(4.5D, attack.sweepDamage(f.target));
            LivingEntity undead = mock(LivingEntity.class);
            f.configureTarget(undead);
            when(undead.getType()).thenReturn(EntityType.ZOMBIE);
            assertEquals(9.5D, attack.sweepDamage(undead));
            f.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(undead)).thenReturn(false);
            assertEquals(0.0D, attack.sweepDamage(undead));
        }
    }

    @Test
    void stabSnapshotRetainsChargeDamageAndWeaponForIndependentTargets() {
        try (Fixture f = new Fixture()) {
            f.item.addUnsafeEnchantment(Enchantment.SMITE, 2);
            PaperGlaiveCombatAdapter adapter = new PaperGlaiveCombatAdapter(f.plugin, f.factory, null);
            adapter.onPreAttack(f.preAttack());
            when(f.player.getAttackCooldown()).thenReturn(0.0F);
            f.attribute(Attribute.ATTACK_DAMAGE, 100.0D);
            f.item.removeEnchantment(Enchantment.SMITE);
            GlaiveCombatAdapter.Attack attack = adapter.capture(f.player, f.target);

            assertEquals(7.0D, attack.stabDamage(f.target));
            LivingEntity undead = mock(LivingEntity.class);
            f.configureTarget(undead);
            when(undead.getType()).thenReturn(EntityType.ZOMBIE);
            assertEquals(12.0D, attack.stabDamage(undead));
            assertEquals(0.0D, adapter.capture(f.player, f.target).stabDamage(undead));
            f.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(undead)).thenReturn(false);
            assertEquals(0.0D, attack.stabDamage(undead));
        }
    }

    @Test
    void criticalStabsMultiplyPhysicalDamageButNotEnchantmentBonus() {
        try (Fixture f = new Fixture()) {
            f.item.addUnsafeEnchantment(Enchantment.SMITE, 2);
            when(f.player.isOnGround()).thenReturn(false);
            when(f.player.getFallDistance()).thenReturn(0.5F);
            BukkitGlaiveCombatAdapter adapter = new BukkitGlaiveCombatAdapter(f.plugin, f.factory, null);
            GlaiveCombatAdapter.Attack attack = adapter.capture(f.player, f.target);
            LivingEntity undead = mock(LivingEntity.class);
            f.configureTarget(undead);
            when(undead.getType()).thenReturn(EntityType.ZOMBIE);

            assertFalse(attack.sweepAllowed());
            assertEquals(10.5D, attack.stabDamage(f.target));
            assertEquals(15.5D, attack.stabDamage(undead));

            when(f.player.hasPotionEffect(PotionEffectType.BLINDNESS)).thenReturn(true);
            assertEquals(12.0D, adapter.capture(f.player, f.target).stabDamage(undead));
        }
    }

    @Test
    void stabsRetainPartialAttackChargeScalingWithoutSweepEligibility() {
        try (Fixture f = new Fixture()) {
            f.item.addUnsafeEnchantment(Enchantment.SMITE, 2);
            when(f.player.getAttackCooldown()).thenReturn(0.5F);
            BukkitGlaiveCombatAdapter adapter = new BukkitGlaiveCombatAdapter(f.plugin, f.factory, null);
            GlaiveCombatAdapter.Attack attack = adapter.capture(f.player, f.target);
            LivingEntity undead = mock(LivingEntity.class);
            f.configureTarget(undead);
            when(undead.getType()).thenReturn(EntityType.ZOMBIE);

            assertFalse(attack.sweepAllowed());
            assertEquals(2.8D, attack.stabDamage(f.target), 1.0E-9D);
            assertEquals(5.3D, attack.stabDamage(undead), 1.0E-9D);

            when(f.player.getAttackCooldown()).thenReturn(Float.NaN);
            assertEquals(0.0D, adapter.capture(f.player, f.target).stabDamage(undead));
        }
    }

    @Test
    void nativeStabFormulaIncludesItemBonusAndKeepsEnchantmentsOutsideCriticalMultiplier() {
        assertEquals(18.5F, NativeGlaiveCombat.stabDamage(7.0F, 7.0F, 2.0F, 12.0F, 1.0F, true));
        assertEquals(5.3F, NativeGlaiveCombat.stabDamage(7.0F, 2.8F, 0.0F, 12.0F, 0.5F, false), 1.0E-6F);
    }

    private static final class Fixture implements AutoCloseable {
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final ItemFactory factory = mock(ItemFactory.class);
        private final Player player = mock(Player.class);
        private final LivingEntity target = mock(LivingEntity.class);
        private final World world = mock(World.class);
        private final ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);

        private Fixture() {
            when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(item);
            when(factory.isObsidianGlaive(item)).thenReturn(true);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getTicksLived()).thenReturn(100);
            when(player.isOnline()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            when(player.isOnGround()).thenReturn(true);
            when(player.getAttackCooldown()).thenReturn(1.0F);
            attribute(Attribute.ATTACK_DAMAGE, 7.0D);
            attribute(Attribute.SWEEPING_DAMAGE_RATIO, 0.5D);
            attribute(Attribute.MOVEMENT_SPEED, 0.1D);
            configureTarget(target);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);
        }

        private void attribute(Attribute attribute, double value) {
            AttributeInstance instance = mock(AttributeInstance.class);
            when(instance.getValue()).thenReturn(value);
            when(player.getAttribute(attribute)).thenReturn(instance);
        }

        private void configureTarget(LivingEntity entity) {
            when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
            when(entity.isValid()).thenReturn(true);
            when(entity.getWorld()).thenReturn(world);
            when(entity.getType()).thenReturn(EntityType.COW);
        }

        private PrePlayerAttackEntityEvent preAttack() {
            PrePlayerAttackEntityEvent event = mock(PrePlayerAttackEntityEvent.class);
            when(event.getPlayer()).thenReturn(player);
            when(event.getAttacked()).thenReturn(target);
            when(event.willAttack()).thenReturn(true);
            return event;
        }

        @Override
        public void close() {
            scheduler.close();
        }
    }
}
