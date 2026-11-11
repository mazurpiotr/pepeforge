package pepin.pepeforge.util.combat;

import com.google.common.base.Function;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ui.BossBarManager;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.crescent.CrescentMoonPower;
import pepin.pepeforge.weapons.crescentkhopesh.CrescentKhopeshListener;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
class ShieldEventOrderingTest {
    private static JavaPlugin plugin;

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0D, 0.2D})
    void registeredWeaponBonusPrecedesShieldAndNativeAbsorption(double enchantmentReduction) {
        Player attacker = MockBukkit.getMock().addPlayer();
        Player defender = MockBukkit.getMock().addPlayer();
        attacker.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.isCrescentKhopesh(any(ItemStack.class))).thenReturn(true);
        when(factory.isItemEnabled(anyString())).thenReturn(true);
        PluginLang lang = mock(PluginLang.class);
        when(lang.text("messages.temporary_shield.bar"))
                .thenReturn("{name} · {current} / {limit} HP · {seconds} s");
        when(lang.text("messages.temporary_shield.sources.moonshield")).thenReturn("Moonshield");
        BossBarManager bars = mock(BossBarManager.class);
        TemporaryShieldManager shields = new TemporaryShieldManager(plugin, lang, bars);
        CrescentKhopeshListener weapon = new CrescentKhopeshListener(plugin, factory, shields,
                mock(pepin.pepeforge.util.aura.AuraManager.class));
        plugin.getServer().getPluginManager().registerEvents(weapon, plugin);
        plugin.getServer().getPluginManager().registerEvents(shields, plugin);
        try (var moon = mockStatic(CrescentMoonPower.class);
                var scheduler = mockStatic(SchedulerCompat.class, CALLS_REAL_METHODS)) {
            scheduler.when(SchedulerCompat::isServerStopping).thenReturn(false);
            try {
                moon.when(() -> CrescentMoonPower.getDamageModifier(attacker)).thenReturn(2.0D);
                shields.grant(defender, "moonshield", 2.0D, 4.0D, 80);
                Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
                modifiers.put(DamageModifier.BASE, 5.0D);
                modifiers.put(DamageModifier.MAGIC, -5.0D * enchantmentReduction);
                modifiers.put(DamageModifier.ABSORPTION, -Math.min(4.0D, 5.0D * (1 - enchantmentReduction)));
                Map<DamageModifier, Function<Double, Double>> functions = new EnumMap<>(DamageModifier.class);
                functions.put(DamageModifier.BASE, ignored -> 0.0D);
                functions.put(DamageModifier.MAGIC, remaining -> -remaining * enchantmentReduction);
                functions.put(DamageModifier.ABSORPTION, remaining -> -Math.min(4.0D, Math.max(0.0D, remaining)));
                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, defender,
                        DamageCause.ENTITY_ATTACK, mock(DamageSource.class), modifiers, functions);

                plugin.getServer().getPluginManager().callEvent(event);

                double remaining = 7.0D * (1 - enchantmentReduction) - 2.0D;
                assertEquals(7.0D, event.getDamage(), 1.0E-9);
                assertEquals(-7.0D * enchantmentReduction - 2.0D,
                        event.getDamage(DamageModifier.MAGIC), 1.0E-9);
                assertEquals(-Math.min(4.0D, remaining), event.getDamage(DamageModifier.ABSORPTION), 1.0E-9);
                assertEquals(Math.max(0.0D, remaining - 4.0D), event.getFinalDamage(), 1.0E-9);
                assertFalse(event.isCancelled());
                verify(bars).removeBar(eq(defender), anyString());
            } finally {
                weapon.stop();
                shields.stop();
                HandlerList.unregisterAll(plugin);
            }
        }
    }
}
