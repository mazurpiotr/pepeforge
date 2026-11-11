package pepin.pepeforge.weapons.rapier;

import java.util.Locale;

public enum RapierMode {
    THRUST(0.75D),
    CUT(0.0D);

    private final double attackRangeBonus;

    RapierMode(double attackRangeBonus) {
        this.attackRangeBonus = attackRangeBonus;
    }

    public double attackRangeBonus() {
        return attackRangeBonus;
    }

    public String modeKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RapierMode fromStoredValue(String value) {
        return "CUT".equals(value) || "SWEEP".equals(value) ? CUT : THRUST;
    }

    public RapierMode next() {
        return this == THRUST ? CUT : THRUST;
    }
}
