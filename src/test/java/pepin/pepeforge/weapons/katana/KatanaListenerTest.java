package pepin.pepeforge.weapons.katana;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.ui.ActionBarHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KatanaListenerTest {

    private static JavaPlugin plugin;

    @BeforeAll
    static void setUpServer() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Test
    void droppingKatanaRestoresTheDroppedStacksNormalVisual() {
        ItemFactory itemFactory = mock(ItemFactory.class);
        KatanaListener listener = new KatanaListener(
                plugin,
                itemFactory,
                mock(PluginLang.class),
                mock(CooldownManager.class));
        Player player = MockBukkit.getMock().addPlayer();
        Item droppedEntity = mock(Item.class);
        ItemStack droppedStack = new ItemStack(Material.DIAMOND_SWORD);
        when(droppedEntity.getItemStack()).thenReturn(droppedStack);
        when(itemFactory.isKatana(droppedStack)).thenReturn(true);

        listener.onDrop(new PlayerDropItemEvent(player, droppedEntity));

        verify(itemFactory).setKatanaVisual(droppedStack, KatanaDefinition.VisualState.NORMAL);
        verify(droppedEntity).setItemStack(droppedStack);
    }
    @Test
    void occupiedOffhandBlocksActivationButLeavesVanillaInteractionAllowed() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.SHIELD));
            PlayerInteractEvent event = fixture.click();
            fixture.listener.onRightClick(event);
            assertEquals(0, fixture.timers.size());
            verify(event, never()).setCancelled(true);
            fixture.bars.verify(() -> ActionBarHelper.showActionBar(fixture.player, "offhand required"));
        }
    }

    @Test
    void equippedInvalidStateWarnsOnceAndFallbackRemainsSilent() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.SHIELD));
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.bars.verify(() -> ActionBarHelper.showActionBar(fixture.player, "offhand required"), times(1));
            fixture.listener.refreshEquipment(fixture.player, false);
            fixture.bars.verifyNoMoreInteractions();
        }
    }

    @Test
    void activeParryIgnoresOffhandChangesDuringEquipmentRefreshAndTicks() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            when(fixture.inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.SHIELD));
            clearInvocations(fixture.inventory);
            fixture.listener.refreshEquipment(fixture.player, true);
            fixture.timers.getFirst().runnable.run();
            verify(fixture.inventory, never()).getItemInOffHand();
            verify(fixture.timers.getFirst().task, never()).cancel();
        }
    }

    @Test
    void projectileReflectionDoesNotReadOffhandDuringActiveParry() {
        try (Fixture fixture = new Fixture()) {
            Projectile projectile = mock(Projectile.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(projectile.isValid()).thenReturn(true);
            when(projectile.getPersistentDataContainer()).thenReturn(data);
            when(projectile.getVelocity()).thenReturn(new Vector(0, 0, -1));
            when(projectile.getLocation()).thenReturn(new Location(fixture.world, 0, 1.6, 1));
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getEntity()).thenReturn(fixture.player);
            when(event.getDamager()).thenReturn(projectile);
            fixture.activate();
            clearInvocations(fixture.inventory);
            fixture.listener.onDamage(event);
            verify(event).setCancelled(true);
            verify(projectile).setShooter(fixture.player);
            verify(fixture.inventory, never()).getItemInOffHand();
        }
    }

    @Test
    void changingSelectedSlotResetsCapturedKatana() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            when(fixture.inventory.getHeldItemSlot()).thenReturn(1);
            when(fixture.inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
            fixture.listener.refreshEquipment(fixture.player, true);
            verify(fixture.factory).setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.NORMAL);
            verify(fixture.timers.getFirst().task).cancel();
        }
    }

    @Test
    void droppingActiveKatanaResetsDroppedStackAndStopsParry() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            Item entity = mock(Item.class);
            when(entity.getItemStack()).thenReturn(fixture.katana);
            fixture.listener.onDrop(new PlayerDropItemEvent(fixture.player, entity));
            verify(entity).setItemStack(fixture.katana);
            verify(fixture.timers.getFirst().task).cancel();
        }
    }

    @Test
    void cooldownTaskStopsOnUnequipAndResumesWithoutResettingCooldown() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.cooldowns.getRemainingCooldownMillis(fixture.player, "katana:parry")).thenReturn(500L);
            fixture.listener.refreshEquipment(fixture.player, true);
            assertEquals(1, fixture.timers.size());
            when(fixture.inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.AIR));
            fixture.timers.getFirst().runnable.run();
            verify(fixture.timers.getFirst().task).cancel();
            when(fixture.inventory.getItemInMainHand()).thenReturn(fixture.katana);
            fixture.listener.refreshEquipment(fixture.player, true);
            assertEquals(2, fixture.timers.size());
            assertEquals(2L, fixture.timers.get(1).period);
            verify(fixture.cooldowns, never()).clearCooldown(any(), anyString());
            verify(fixture.cooldowns, never()).setCooldown(any(), anyString(), anyLong());
        }
    }

    @Test
    void staleTicksDoNothingAfterStopAndRestart() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            Timer old = fixture.timers.getFirst();
            fixture.listener.stop();
            fixture.listener.start();
            clearInvocations(fixture.player, fixture.inventory, fixture.factory);
            old.runnable.run();
            verifyNoInteractions(fixture.player, fixture.inventory, fixture.factory);
        }
    }

    @Test
    void retiredTasksReleaseBookkeepingWithoutAccessingPlayer() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            clearInvocations(fixture.player, fixture.inventory, fixture.factory);
            fixture.timers.getFirst().retired.run();
            verifyNoInteractions(fixture.player, fixture.inventory, fixture.factory);
            when(fixture.cooldowns.getRemainingCooldownMillis(fixture.player, "katana:parry")).thenReturn(500L);
            fixture.listener.refreshEquipment(fixture.player, false);
            assertEquals(2, fixture.timers.size());
        }
    }

    @Test
    void occupiedOffhandDoesNotBlockVanillaChestInteractionOrWarn() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.SHIELD));
            Block chest = mock(Block.class);
            when(chest.getType()).thenReturn(Material.CHEST);
            PlayerInteractEvent event = fixture.click();
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(event.getClickedBlock()).thenReturn(chest);
            fixture.listener.onRightClick(event);
            verify(event, never()).setCancelled(true);
            fixture.bars.verifyNoInteractions();
        }
    }

    @Test
    void cooldownTickDoesNotReadOffhandAndStopsAfterExpiry() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.cooldowns.getRemainingCooldownMillis(fixture.player, "katana:parry")).thenReturn(500L);
            fixture.listener.refreshEquipment(fixture.player, false);
            clearInvocations(fixture.inventory);
            fixture.timers.getFirst().runnable.run();
            verify(fixture.inventory, never()).getItemInOffHand();
            when(fixture.cooldowns.getRemainingCooldownMillis(fixture.player, "katana:parry")).thenReturn(0L);
            fixture.timers.getFirst().runnable.run();
            verify(fixture.timers.getFirst().task).cancel();
        }
    }

    @Test
    void deathRestoresTheActualDropAndRemovesSessionMarker() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            ItemStack drop = fixture.katana.clone();
            PlayerDeathEvent death = mock(PlayerDeathEvent.class);
            when(death.getEntity()).thenReturn(fixture.player);
            when(death.getDrops()).thenReturn(List.of(drop));
            fixture.listener.onDeath(death);
            verify(fixture.factory, atLeastOnce()).setKatanaVisual(drop, KatanaDefinition.VisualState.NORMAL);
            assertFalse(drop.getItemMeta().getPersistentDataContainer()
                    .has(new NamespacedKey(plugin, "katana_parry_session"), PersistentDataType.STRING));
        }
    }

    @Test
    void deferredOldRuntimeCleanupDoesNotResetNewParryOnTheSameStack() {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            List<Runnable> cleanup = new ArrayList<>();
            fixture.scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(fixture.player)).thenReturn(false);
            fixture.scheduler.when(() -> SchedulerCompat.runForPlayer(eq(fixture.player), eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        cleanup.add(invocation.getArgument(2));
                        return null;
                    });
            fixture.listener.stop();
            fixture.listener.start();
            fixture.listener.onRightClick(fixture.click());
            clearInvocations(fixture.factory);
            cleanup.getFirst().run();
            verify(fixture.factory, never()).setKatanaVisual(any(), eq(KatanaDefinition.VisualState.NORMAL));
        }
    }

    @Test
    void activationStartsOneParryTimerAndOneEquippedCooldownTimer() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.cooldowns.getRemainingCooldownMillis(fixture.player, "katana:parry"))
                    .thenReturn(0L, 500L);
            fixture.listener.onRightClick(fixture.click());
            fixture.listener.refreshEquipment(fixture.player, true);
            assertEquals(2, fixture.timers.size());
            assertEquals(1L, fixture.timers.getFirst().period);
            assertEquals(2L, fixture.timers.get(1).period);
        }
    }

    @Test
    void activeParryStillAllowsVanillaChestInteractionWithoutReadingOffhand() {
        try (Fixture fixture = new Fixture()) {
            Block chest = mock(Block.class);
            when(chest.getType()).thenReturn(Material.CHEST);
            PlayerInteractEvent event = fixture.click();
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(event.getClickedBlock()).thenReturn(chest);
            fixture.activate();
            clearInvocations(fixture.inventory);
            fixture.listener.onRightClick(event);
            verify(event, never()).setCancelled(true);
            verify(fixture.inventory, never()).getItemInOffHand();
        }
    }

    @Test
    void successfulReflectionExtendsParryAndAlternatesVisualWithoutChangingCooldown() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            clearInvocations(fixture.factory, fixture.cooldowns);
            long initialUntil = fixture.sessionLong("until");
            when(fixture.world.getGameTime()).thenReturn(10L);

            fixture.listener.onDamage(fixture.projectileEvent(fixture.projectile()));

            assertEquals(initialUntil + KatanaDefinition.PARRY_EXTENSION_TICKS * 50L,
                    fixture.sessionLong("until"));
            verify(fixture.factory).setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.PARRY_B);
            verifyNoInteractions(fixture.cooldowns);
        }
    }

    @Test
    void multipleReflectionsInOneTickExtendOnlyOnce() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            clearInvocations(fixture.factory);
            when(fixture.world.getGameTime()).thenReturn(10L);
            fixture.listener.onDamage(fixture.projectileEvent(fixture.projectile()));
            fixture.listener.onDamage(fixture.projectileEvent(fixture.projectile()));

            long activationTime = fixture.sessionLong("maxUntil")
                    - KatanaDefinition.PARRY_MAX_DURATION_TICKS * 50L;
            assertEquals((KatanaDefinition.PARRY_DURATION_TICKS + KatanaDefinition.PARRY_EXTENSION_TICKS) * 50L,
                    fixture.sessionLong("until") - activationTime);
            verify(fixture.factory, times(1))
                    .setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.PARRY_B);
            verify(fixture.factory, times(1))
                    .setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.PARRY_A);
        }
    }

    @Test
    void reflectionExtensionStopsAtMaximumDurationAndNextTickAlternatesBack() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate();
            clearInvocations(fixture.factory);
            for (long tick = 1L; tick <= 10L; tick++) {
                when(fixture.world.getGameTime()).thenReturn(tick);
                fixture.listener.onDamage(fixture.projectileEvent(fixture.projectile()));
            }

            long activationTime = fixture.sessionLong("maxUntil")
                    - KatanaDefinition.PARRY_MAX_DURATION_TICKS * 50L;
            assertEquals(KatanaDefinition.PARRY_MAX_DURATION_TICKS * 50L,
                    fixture.sessionLong("until") - activationTime);
            verify(fixture.factory, times(5))
                    .setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.PARRY_B);
            verify(fixture.factory, times(5))
                    .setKatanaVisual(fixture.katana, KatanaDefinition.VisualState.PARRY_A);
        }
    }

    private record Timer(Runnable runnable, Runnable retired, ScheduledTaskCompat task, long period) {
    }

    private static final class Fixture implements AutoCloseable {
        private final Player player = mock(Player.class);
        private final PlayerInventory inventory = mock(PlayerInventory.class);
        private final World world = mock(World.class);
        private final ItemStack katana = new ItemStack(Material.DIAMOND_SWORD);
        private final ItemFactory factory = mock(ItemFactory.class);
        private final CooldownManager cooldowns = mock(CooldownManager.class);
        private final List<Timer> timers = new ArrayList<>();
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private final MockedStatic<ActionBarHelper> bars = mockStatic(ActionBarHelper.class);
        private final KatanaListener listener;

        private Fixture() {
            PluginLang lang = mock(PluginLang.class);
            when(lang.text("messages.two_handed.offhand_required")).thenReturn("offhand required");
            when(lang.text("messages.katana.cooldown")).thenReturn("cooldown {bar} {seconds}");
            bars.when(() -> ActionBarHelper.buildProgressBar(anyDouble())).thenReturn("progress");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
            when(player.getEyeLocation()).thenAnswer(ignored -> new Location(world, 0, 1.6, 0));
            when(inventory.getItemInMainHand()).thenReturn(katana);
            when(factory.isKatana(katana)).thenReturn(true);
            doNothing().when(factory).setKatanaVisual(eq(katana), any(KatanaDefinition.VisualState.class));
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(org.bukkit.entity.Entity.class)))
                    .thenReturn(true);
            scheduler.when(() -> SchedulerCompat.runTimerForEntity(eq(player), eq(plugin), any(Runnable.class),
                    any(Runnable.class), anyLong(), anyLong())).thenAnswer(invocation -> {
                ScheduledTaskCompat task = mock(ScheduledTaskCompat.class);
                timers.add(new Timer(invocation.getArgument(2), invocation.getArgument(3), task,
                        invocation.getArgument(5)));
                return task;
            });
            listener = new KatanaListener(plugin, factory, lang, cooldowns);
            listener.start();
        }

        private PlayerInteractEvent click() {
            PlayerInteractEvent event = mock(PlayerInteractEvent.class);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(event.getPlayer()).thenReturn(player);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            return event;
        }

        private void activate() {
            listener.onRightClick(click());
            assertEquals(1, timers.size());
        }

        private Projectile projectile() {
            Projectile projectile = mock(Projectile.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(projectile.isValid()).thenReturn(true);
            when(projectile.getPersistentDataContainer()).thenReturn(data);
            when(projectile.getVelocity()).thenReturn(new Vector(0, 0, -1));
            when(projectile.getLocation()).thenReturn(new Location(world, 0, 1.6, 1));
            return projectile;
        }

        private EntityDamageByEntityEvent projectileEvent(Projectile projectile) {
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getEntity()).thenReturn(player);
            when(event.getDamager()).thenReturn(projectile);
            return event;
        }

        private long sessionLong(String fieldName) throws Exception {
            Field sessionsField = KatanaListener.class.getDeclaredField("activeParries");
            sessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<UUID, ?> sessions = (java.util.Map<UUID, ?>) sessionsField.get(listener);
            Object session = sessions.get(player.getUniqueId());
            Field valueField = session.getClass().getDeclaredField(fieldName);
            valueField.setAccessible(true);
            return valueField.getLong(session);
        }

        @Override
        public void close() {
            listener.stop();
            bars.close();
            scheduler.close();
        }
    }

}
