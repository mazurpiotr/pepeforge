package pepin.pepeforge.weapons.rapier;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IceRapierSettingsTest {

    @Test
    void damageFallsLinearlyWithActualWearAndNeverExceedsEndpoints() {
        IceRapierSettings settings = IceRapierSettings.fromConfig(new YamlConfiguration());
        assertEquals(32, settings.maxDurability());
        assertEquals(200, settings.freezeTicks());
        assertEquals(8.0, settings.attackDamage(0, 32));
        assertEquals(6.0, settings.attackDamage(16, 32));
        assertEquals(4.125, settings.attackDamage(31, 32));
        assertEquals(4.0, settings.attackDamage(32, 32));
        assertEquals(4.0, settings.attackDamage(100, 32));
        assertEquals(8.0, settings.attackDamage(-1, 32));
        assertEquals(4.0, settings.attackDamage(1, 0));
    }

    @Test
    void malformedValuesRecoverAndEndpointsRemainOrdered() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mechanics.ice_rapier.min_damage", Double.NaN);
        config.set("mechanics.ice_rapier.max_damage", -20);
        config.set("mechanics.ice_rapier.max_durability", Double.POSITIVE_INFINITY);
        config.set("mechanics.ice_rapier.freeze_ticks", 2000);
        IceRapierSettings settings = IceRapierSettings.fromConfig(config);
        assertEquals(4.0, settings.minDamage());
        assertEquals(4.0, settings.maxDamage());
        assertEquals(32, settings.maxDurability());
        assertEquals(600, settings.freezeTicks());
    }
}
