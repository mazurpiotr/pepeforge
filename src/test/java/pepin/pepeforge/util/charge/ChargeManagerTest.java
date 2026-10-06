package pepin.pepeforge.util.charge;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.util.persistence.PersistentDataCompat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChargeManagerTest {

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
    void chargesAreLimitedAndDecayAfterInterval() {
        Player player = MockBukkit.getMock().addPlayer();
        ChargeManager manager = new ChargeManager(new NamespacedKey(plugin, "test_charges"));

        assertEquals(1, manager.addCharge(player, 2, 10));
        assertEquals(2, manager.addCharge(player, 2, 11));
        assertEquals(2, manager.addCharge(player, 2, 12));
        assertEquals(2, manager.decay(player, 12, 5, 1));
        assertEquals(1, manager.decay(player, 17, 5, 1));
        assertEquals(1, PersistentDataCompat.getInt(player, new NamespacedKey(plugin, "test_charges")));
    }

    @Test
    void resetClearsChargesAndTransientDecayState() {
        Player player = MockBukkit.getMock().addPlayer();
        ChargeManager manager = new ChargeManager(new NamespacedKey(plugin, "test_reset"));

        manager.addCharge(player, 5, 10);
        manager.reset(player);

        assertEquals(0, manager.getCharges(player));
        assertEquals(0, manager.decay(player, 100, 1, 1));
    }
}