package pepin.pepeforge.util.combat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.aura.AuraManager;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.weapons.anchor.AnchorListener;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordListener;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordManager;
import pepin.pepeforge.weapons.katana.KatanaListener;
import pepin.pepeforge.weapons.greatsword.GreatswordListener;

class CombatProtectionTest {
    static {
        org.mockbukkit.mockbukkit.MockBukkit.mock();
    }

    @org.junit.jupiter.api.AfterAll
    static void tearDownServer() {
        org.mockbukkit.mockbukkit.MockBukkit.unmock();
    }

    private final ItemFactory items = mock(ItemFactory.class);
    private final CrimsonSwordManager manager = mock(CrimsonSwordManager.class);
    private final AuraManager auras = mock(AuraManager.class);
    private final Player attacker = mock(Player.class);
    private final Player defender = mock(Player.class);
    private final ItemStack sword = mock(ItemStack.class);
    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private CrimsonSwordListener crimson;

    @BeforeEach
    void setUp() {
        when(plugin.getName()).thenReturn("PepeForge");
        when(plugin.namespace()).thenReturn("pepeforge");
        when(attacker.getUniqueId()).thenReturn(UUID.randomUUID());
        when(defender.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(attacker.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(sword);
        when(defender.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(items.isCrimsonSword(sword)).thenReturn(true);
        when(manager.getLevel(sword)).thenReturn(5);
        when(manager.lifesteal(5)).thenReturn(0.1D);
        when(defender.getHealth()).thenReturn(20.0D);
        when(attacker.isValid()).thenReturn(true);
        when(attacker.isOnline()).thenReturn(true);
        when(defender.isValid()).thenReturn(true);
        when(defender.isOnline()).thenReturn(true);
        World world = mock(World.class);
        when(attacker.getWorld()).thenReturn(world);
        when(defender.getWorld()).thenReturn(world);
        when(defender.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0));
        when(defender.getEyeLocation()).thenAnswer(call -> new Location(world, 0, 65, 0));
        when(attacker.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 8, 180, 0));
        when(attacker.getEyeLocation()).thenAnswer(call -> new Location(world, 0, 65, 8, 180, 0));
        crimson = new CrimsonSwordListener(items, manager, auras);
    }

    @Test
    void highPriorityDamageModifierCannotGrantRewardsBeforeLaterCancellation() {
        var event = event();
        new DamageEvents(crimson, new DamageFlowTest.Canceller()).dispatch(event);
        assertTrue(event.isCancelled());
        verify(manager, never()).addXp(any(), any(), anyDouble());
        verify(manager, never()).heal(any(), anyDouble());
        verifyNoInteractions(auras);
    }

    @Test
    void acceptedPrimaryHitGrantsRewardsExactlyOnce() {
        var event = event();
        new DamageEvents(crimson).dispatch(event);
        assertFalse(event.isCancelled());
        verify(manager).addXp(attacker, sword, event.getFinalDamage());
        verify(manager).heal(attacker, event.getFinalDamage() * 0.1D);
    }

    @Test
    void secondaryAuraDamageNeverFeedsSwordXpOrLifesteal() {
        when(defender.isValid()).thenReturn(true);
        var events = new DamageEvents(new DamageFlow(), crimson);
        doAnswer(call -> {
            events.dispatch(event());
            when(defender.getHealth()).thenReturn(18.0D);
            return null;
        }).when(defender).damage(4.0D, attacker);

        assertEquals(new DamageFlow.Result(true, 2), DamageFlow.damage(defender, 4, attacker));
        verify(manager, never()).addXp(any(), any(), anyDouble());
        verify(manager, never()).heal(any(), anyDouble());
    }

    @Test
    void cancelledAnchorHitCannotApplySnareOrStartItsTask() {
        var anchor = new AnchorListener(plugin, items, mock(CooldownManager.class), mock(PluginLang.class));
        when(items.isAnchor(sword)).thenReturn(true);
        var event = event();
        new DamageEvents(anchor, new DamageFlowTest.Canceller()).dispatch(event);
        assertTrue(event.isCancelled());
        verify(defender, never()).addPotionEffect(any());
        verify(defender, never()).getPersistentDataContainer();
    }

    @Test
    void realKatanaParryCancelsCrimsonRewardsAndDamagesBeforeCounterPush() throws ReflectiveOperationException {
        var katana = katana();
        activateParry(katana, defender);
        var events = new DamageEvents(new DamageFlow(), crimson, katana);
        dispatchCounterattack(events);

        var event = event();
        events.dispatch(event);
        assertTrue(event.isCancelled());
        verify(manager, never()).addXp(any(), any(), anyDouble());
        verify(manager, never()).heal(any(), anyDouble());
        var order = inOrder(attacker);
        order.verify(attacker).damage(1.0D, defender);
        order.verify(attacker).setVelocity(any());
    }

    @Test
    void protectionCanCancelCounterattackWithoutUndoingParry() throws ReflectiveOperationException {
        var katana = katana();
        activateParry(katana, defender);
        var events = new DamageEvents(new DamageFlow(), katana, new DamageFlowTest.Canceller());
        dispatchCounterattack(events);

        var event = event();
        events.dispatch(event);
        assertTrue(event.isCancelled());
        verify(attacker).damage(1.0D, defender);
        verify(attacker, never()).setVelocity(any());
    }

    @Test
    void twoParryingKatanasCannotRecursivelyCounterattack() throws ReflectiveOperationException {
        var katana = katana();
        activateParry(katana, defender);
        activateParry(katana, attacker);
        var events = new DamageEvents(new DamageFlow(), katana);
        dispatchCounterattack(events);

        var event = event();
        events.dispatch(event);
        assertTrue(event.isCancelled());
        verify(attacker, times(1)).damage(1.0D, defender);
        verify(defender, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
        verify(attacker, never()).setVelocity(any());
    }

    @Test
    void cancelledHookCannotPullEitherParticipant() throws ReflectiveOperationException {
        dispatchHook(new DamageEvents(new DamageFlow(), new DamageFlowTest.Canceller()), false, false);
        hitWithAnchor();
        verify(defender).damage(1.0D, attacker);
        verify(attacker, never()).setVelocity(any());
        verify(defender, never()).setVelocity(any());
    }

    @Test
    void shieldBlockedHookStillPullsBothParticipants() throws ReflectiveOperationException {
        dispatchHook(new DamageEvents(new DamageFlow()), true, false);
        hitWithAnchor();
        verify(attacker).setVelocity(argThat(velocity -> velocity.getZ() < 0));
        verify(defender).setVelocity(argThat(velocity -> velocity.getZ() > 0));
    }

    @Test
    void lethalHookStillPullsThrowerButDoesNotMoveDeadTarget() throws ReflectiveOperationException {
        dispatchHook(new DamageEvents(new DamageFlow()), false, true);
        hitWithAnchor();
        verify(attacker).setVelocity(argThat(velocity -> velocity.getZ() < 0));
        verify(defender, never()).setVelocity(any());
    }

    @Test
    void hookWithoutDamageEventCannotPull() throws ReflectiveOperationException {
        hitWithAnchor();
        verify(attacker, never()).setVelocity(any());
        verify(defender, never()).setVelocity(any());
    }

    private KatanaListener katana() {
        return new KatanaListener(plugin, items, mock(PluginLang.class), mock(CooldownManager.class));
    }

    @SuppressWarnings("unchecked")
    private void activateParry(KatanaListener katana, Player player) throws ReflectiveOperationException {
        var field = KatanaListener.class.getDeclaredField("activeParryUntil");
        field.setAccessible(true);
        ((Map<UUID, Long>) field.get(katana)).put(player.getUniqueId(), System.currentTimeMillis() + 60_000L);
    }

    private void dispatchCounterattack(DamageEvents events) {
        doAnswer(call -> {
            events.dispatch(new EntityDamageByEntityEvent(defender, attacker, DamageCause.ENTITY_ATTACK,
                    mock(DamageSource.class), 1.0D));
            return null;
        }).when(attacker).damage(1.0D, defender);
    }

    private void dispatchHook(DamageEvents events, boolean shield, boolean lethal) {
        doAnswer(call -> {
            var hit = event();
            if (shield) {
                hit.setDamage(0.0D);
            }
            events.dispatch(hit);
            if (lethal && !hit.isCancelled()) {
                when(defender.isDead()).thenReturn(true);
                when(defender.getHealth()).thenReturn(0.0D);
            }
            return null;
        }).when(defender).damage(1.0D, attacker);
    }

    private void hitWithAnchor() throws ReflectiveOperationException {
        var anchor = new AnchorListener(plugin, items, mock(CooldownManager.class), mock(PluginLang.class));
        var method = AnchorListener.class.getDeclaredMethod("hitEntity", Player.class,
                org.bukkit.entity.LivingEntity.class, Location.class);
        method.setAccessible(true);
        method.invoke(anchor, attacker, defender, defender.getLocation());
    }

    @Test
    void cancelledCleaveDoesNotApplyKnockback() throws ReflectiveOperationException {
        var greatsword = new GreatswordListener(plugin, items, mock(PluginLang.class));
        var method = GreatswordListener.class.getDeclaredMethod("applyAreaAttack",
                Player.class, java.util.List.class, double.class, int.class, boolean.class);
        method.setAccessible(true);
        World world = mock(World.class);
        when(attacker.getWorld()).thenReturn(world);
        when(attacker.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0));
        when(attacker.getEyeLocation()).thenAnswer(call -> new Location(world, 0, 65, 0));
        when(defender.isValid()).thenReturn(true);
        var dispatcher = new DamageEvents(new DamageFlow(), new DamageFlowTest.Canceller());
        doAnswer(call -> {
            dispatcher.dispatch(event());
            return null;
        }).when(defender).damage(4.0D, attacker);

        method.invoke(greatsword, attacker, java.util.List.of(defender), 4.0D, 1, false);
        verify(defender).damage(4.0D, attacker);
        verify(defender, never()).setVelocity(any());
    }

    private EntityDamageByEntityEvent event() {
        return new EntityDamageByEntityEvent(attacker, defender, DamageCause.ENTITY_ATTACK,
                mock(DamageSource.class), 4.0D);
    }
}
