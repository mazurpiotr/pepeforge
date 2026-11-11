package pepin.pepeforge.weapons.rapier;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class IceRapierListenerTest {
    @BeforeAll
    static void setUpServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Test
    void observesOnlyUncancelledMonitorDamage() throws NoSuchMethodException {
        EventHandler handler = IceRapierListener.class.getMethod("onDamage", EntityDamageByEntityEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.MONITOR, handler.priority());
        assertTrue(handler.ignoreCancelled());
        try (Fixture fixture = new Fixture()) {
            EntityDamageByEntityEvent event = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(event.isCancelled()).thenReturn(true);
            fixture.listener.onDamage(event);
            verifyNoInteractions(fixture.victim, fixture.factory);
        }
    }

    @Test
    void primaryThrustRefreshesNativeFreezeWithoutChangingDamageOrAddingPotionEffects() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.victim.getFreezeTicks()).thenReturn(60, 200);
            EntityDamageByEntityEvent event = fixture.damage(DamageCause.ENTITY_ATTACK);
            fixture.listener.onDamage(event);
            fixture.listener.onDamage(event);
            verify(fixture.victim).setFreezeTicks(200);
            verify(event, never()).setDamage(anyDouble());
            verify(event, never()).setCancelled(anyBoolean());
            verify(fixture.victim, never()).addPotionEffect(any());
        }
    }

    @Test
    void longerForeignFreezingIsPreserved() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.victim.getFreezeTicks()).thenReturn(500);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim, never()).setFreezeTicks(anyInt());
        }
    }

    @Test
    void projectileAndInternalDamageDoNotFreeze() {
        try (Fixture fixture = new Fixture(); MockedStatic<DamageFlow> flow = mockStatic(DamageFlow.class)) {
            fixture.listener.onDamage(fixture.damage(DamageCause.PROJECTILE));
            EntityDamageByEntityEvent internal = fixture.damage(DamageCause.ENTITY_ATTACK);
            flow.when(() -> DamageFlow.isSecondaryDamage(internal)).thenReturn(true);
            fixture.listener.onDamage(internal);
            verifyNoInteractions(fixture.victim, fixture.factory);
        }
    }

    @Test
    void otherRapiersAndDisabledItemsDoNotFreeze() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.factory.getRapierTier(fixture.item)).thenReturn(RapierTier.IRON, (RapierTier) null);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim, never()).getFreezeTicks();
            verify(fixture.victim, never()).setFreezeTicks(anyInt());
        }
    }

    @Test
    void bothModesFreezeTheirPrimaryTarget() {
        for (RapierMode mode : RapierMode.values()) {
            try (Fixture fixture = new Fixture()) {
                when(fixture.factory.getRapierMode(fixture.item)).thenReturn(mode);
                fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
                verify(fixture.victim).setFreezeTicks(200);
            }
        }
    }

    @Test
    void cutFreezesAcceptedSecondaryTargetsAndSkipsCancelledOrAbsorbedTargetsIndividually() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.factory.getRapierMode(fixture.item)).thenReturn(RapierMode.CUT);
            LivingEntity secondary = mock(LivingEntity.class);
            fixture.configureVictim(secondary);
            LivingEntity protectedTarget = mock(LivingEntity.class);
            LivingEntity absorbedTarget = mock(LivingEntity.class);
            EntityDamageByEntityEvent accepted = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            when(accepted.getEntity()).thenReturn(secondary);
            EntityDamageByEntityEvent cancelled = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            when(cancelled.getEntity()).thenReturn(protectedTarget);
            when(cancelled.isCancelled()).thenReturn(true);
            EntityDamageByEntityEvent absorbed = fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK);
            when(absorbed.getEntity()).thenReturn(absorbedTarget);
            when(absorbed.getFinalDamage()).thenReturn(0.0);

            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            fixture.listener.onDamage(accepted);
            fixture.listener.onDamage(cancelled);
            fixture.listener.onDamage(absorbed);

            verify(fixture.victim).setFreezeTicks(200);
            verify(secondary).setFreezeTicks(200);
            verifyNoInteractions(protectedTarget, absorbedTarget);
        }
    }

    @Test
    void thrustCannotFreezeThroughASecondarySweepEvent() {
        try (Fixture fixture = new Fixture()) {
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_SWEEP_ATTACK));
            verify(fixture.victim, never()).getFreezeTicks();
            verify(fixture.victim, never()).setFreezeTicks(anyInt());
        }
    }

    @Test
    void fullyBlockedOrAbsorbedAndNonFiniteDamageDoNotFreeze() {
        try (Fixture fixture = new Fixture()) {
            for (double damage : new double[]{0.0D, -1.0D, Double.NaN, Double.POSITIVE_INFINITY}) {
                EntityDamageByEntityEvent event = fixture.damage(DamageCause.ENTITY_ATTACK);
                when(event.getFinalDamage()).thenReturn(damage);
                fixture.listener.onDamage(event);
            }
            verifyNoInteractions(fixture.victim, fixture.factory);
        }
    }

    @Test
    void foreignAttackerAndVictimAreNotReadOrModified() {
        try (Fixture fixture = new Fixture()) {
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(false);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.player, never()).getInventory();
            verifyNoInteractions(fixture.victim);
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(true);
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.victim)).thenReturn(false);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verifyNoInteractions(fixture.victim, fixture.factory);
        }
    }

    @Test
    void invalidDeadOfflineDifferentWorldAndSelfHitsDoNotFreeze() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.player.isOnline()).thenReturn(false);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            when(fixture.player.isOnline()).thenReturn(true);
            when(fixture.victim.isValid()).thenReturn(false);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            when(fixture.victim.isValid()).thenReturn(true);
            when(fixture.victim.isDead()).thenReturn(true);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            when(fixture.victim.isDead()).thenReturn(false);
            when(fixture.victim.getWorld()).thenReturn(mock(World.class));
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            EntityDamageByEntityEvent self = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(self.getEntity()).thenReturn(fixture.player);
            fixture.listener.onDamage(self);
            verify(fixture.victim, never()).setFreezeTicks(anyInt());
            verify(fixture.player, never()).setFreezeTicks(anyInt());
        }
    }

    @Test
    void nativeEntityTagAndSkeletonImmunityAreRespected() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.immuneTypes.isTagged(EntityType.ZOMBIE)).thenReturn(true);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            when(fixture.immuneTypes.isTagged(EntityType.ZOMBIE)).thenReturn(false);
            when(fixture.victim.getType()).thenReturn(EntityType.SKELETON);
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim, never()).getFreezeTicks();
        }
    }

    @Test
    void leatherArmorAndDatapackImmuneWearablesAreRespected() {
        try (Fixture fixture = new Fixture()) {
            EntityEquipment equipment = mock(EntityEquipment.class);
            when(fixture.victim.getEquipment()).thenReturn(equipment);
            when(fixture.immuneWearables.isTagged(Material.LEATHER_BOOTS)).thenReturn(true);
            when(equipment.getArmorContents()).thenReturn(new ItemStack[]{null, new ItemStack(Material.LEATHER_BOOTS)});
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            when(fixture.immuneWearables.isTagged(Material.DIAMOND_HELMET)).thenReturn(true);
            when(equipment.getArmorContents()).thenReturn(new ItemStack[]{new ItemStack(Material.DIAMOND_HELMET)});
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim, never()).getFreezeTicks();
        }
    }

    @Test
    void horseLeatherArmorAndSpectatorsAreRespected() {
        try (Fixture fixture = new Fixture()) {
            Horse horse = mock(Horse.class);
            fixture.configureVictim(horse);
            HorseInventory inventory = mock(HorseInventory.class);
            when(horse.getInventory()).thenReturn(inventory);
            when(inventory.getArmor()).thenReturn(new ItemStack(Material.LEATHER_HORSE_ARMOR));
            when(fixture.immuneWearables.isTagged(Material.LEATHER_HORSE_ARMOR)).thenReturn(true);
            EntityDamageByEntityEvent horseHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(horseHit.getEntity()).thenReturn(horse);
            fixture.listener.onDamage(horseHit);
            verify(horse, never()).getFreezeTicks();

            Player spectator = mock(Player.class);
            fixture.configureVictim(spectator);
            when(spectator.getGameMode()).thenReturn(GameMode.SPECTATOR);
            EntityDamageByEntityEvent playerHit = fixture.damage(DamageCause.ENTITY_ATTACK);
            when(playerHit.getEntity()).thenReturn(spectator);
            fixture.listener.onDamage(playerHit);
            verify(spectator, never()).getFreezeTicks();
        }
    }

    @Test
    void stopPreventsNewFreezeAndRestartUsesConfiguredTicks() {
        try (Fixture fixture = new Fixture(320)) {
            fixture.listener.stop();
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verifyNoInteractions(fixture.victim, fixture.factory);
            fixture.listener.start();
            fixture.listener.onDamage(fixture.damage(DamageCause.ENTITY_ATTACK));
            verify(fixture.victim).setFreezeTicks(320);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Player player = mock(Player.class);
        private final LivingEntity victim = mock(LivingEntity.class);
        private final World world = mock(World.class);
        private final ItemStack item = new ItemStack(Material.IRON_SWORD);
        private final ItemFactory factory = mock(ItemFactory.class);
        @SuppressWarnings("unchecked")
        private final Tag<EntityType> immuneTypes = mock(Tag.class);
        @SuppressWarnings("unchecked")
        private final Tag<Material> immuneWearables = mock(Tag.class);
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private final IceRapierListener listener;

        private Fixture() {
            this(200);
        }

        private Fixture(int freezeTicks) {
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.isOnline()).thenReturn(true);
            when(player.isValid()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInMainHand()).thenReturn(item);
            when(factory.getRapierTier(item)).thenReturn(RapierTier.ICE);
            when(factory.getRapierMode(item)).thenReturn(RapierMode.THRUST);
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class))).thenReturn(true);
            configureVictim(victim);
            YamlConfiguration config = new YamlConfiguration();
            config.set("mechanics.ice_rapier.freeze_ticks", freezeTicks);
            listener = new IceRapierListener(factory, IceRapierSettings.fromConfig(config), immuneTypes, immuneWearables);
            listener.start();
        }

        private void configureVictim(LivingEntity entity) {
            when(entity.isValid()).thenReturn(true);
            when(entity.getWorld()).thenReturn(world);
            when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        }

        private EntityDamageByEntityEvent damage(DamageCause cause) {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getDamager()).thenReturn(player);
            when(event.getEntity()).thenReturn(victim);
            when(event.getCause()).thenReturn(cause);
            when(event.getFinalDamage()).thenReturn(4.0D);
            return event;
        }

        @Override
        public void close() {
            listener.stop();
            scheduler.close();
        }
    }
}
