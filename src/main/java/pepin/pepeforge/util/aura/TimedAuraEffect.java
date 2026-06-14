package pepin.pepeforge.util.aura;

public interface TimedAuraEffect extends AuraEffect {

    boolean isExpired();
    void extendDuration(int ticks);
}
