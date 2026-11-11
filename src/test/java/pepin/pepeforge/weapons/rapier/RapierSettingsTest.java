package pepin.pepeforge.weapons.rapier;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RapierSettingsTest {
    @Test
    void emptyConfigurationUsesDocumentedDefaults() {
        RapierSettings settings = RapierSettings.fromConfig(new YamlConfiguration());
        assertEquals(5.0D, settings.attackDamage(RapierTier.IRON));
        assertEquals(6.0D, settings.attackDamage(RapierTier.DIAMOND));
        assertEquals(7.0D, settings.attackDamage(RapierTier.NETHERITE));
        assertEquals(2.0D, settings.attackSpeed());
        assertFalse(settings.frontalBonusEnabled());
        assertEquals(20.0D, settings.frontalBonusPercent());
        assertEquals(120.0D, settings.frontalConeDegrees());
    }

    @Test
    void invalidNumbersFallBackAndFiniteValuesAreClamped() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mechanics.rapier.damage.iron", Double.NaN);
        config.set("mechanics.rapier.damage.diamond", Double.POSITIVE_INFINITY);
        config.set("mechanics.rapier.damage.netherite", -4);
        config.set("mechanics.rapier.attack_speed", 100);
        config.set("mechanics.rapier.frontal_bonus_percent", 200);
        config.set("mechanics.rapier.frontal_cone_degrees", Double.NEGATIVE_INFINITY);
        RapierSettings settings = RapierSettings.fromConfig(config);
        assertEquals(5.0D, settings.ironDamage());
        assertEquals(6.0D, settings.diamondDamage());
        assertEquals(1.0D, settings.netheriteDamage());
        assertEquals(4.0D, settings.attackSpeed());
        assertEquals(100.0D, settings.frontalBonusPercent());
        assertEquals(120.0D, settings.frontalConeDegrees());
    }
}
