package pepin.pepeforge.util.charge;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import pepin.pepeforge.util.persistence.PersistentDataCompat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChargeManager {

    private final NamespacedKey chargesKey;
    private final Map<UUID, Long> lastChargeGainTick = new ConcurrentHashMap<>();

    public ChargeManager(NamespacedKey chargesKey) {
        this.chargesKey = chargesKey;
    }

    public int getCharges(Player player) {
        return Math.max(0, PersistentDataCompat.getInt(player, chargesKey));
    }

    public int addCharge(Player player, int maximum, long currentTick) {
        int currentCharges = getCharges(player);
        int nextCharges = Math.min(Math.max(0, maximum), currentCharges + 1);
        setCharges(player, nextCharges);
        lastChargeGainTick.put(player.getUniqueId(), currentTick);
        return nextCharges;
    }

    public ChargeResult addChargeOncePerTick(Player player, int maximum, long currentTick) {
        UUID playerId = player.getUniqueId();
        Long lastGainTick = lastChargeGainTick.get(playerId);
        if (lastGainTick != null && lastGainTick == currentTick) {
            return new ChargeResult(getCharges(player), false);
        }

        return new ChargeResult(addCharge(player, maximum, currentTick), true);
    }

    public void setCharges(Player player, int charges) {
        PersistentDataCompat.setInt(player, chargesKey, Math.max(0, charges));
    }

    public void reset(Player player) {
        setCharges(player, 0);
        lastChargeGainTick.remove(player.getUniqueId());
    }

    public int decay(Player player, long currentTick, long intervalTicks, int amount) {
        int currentCharges = getCharges(player);
        if (currentCharges <= 0) {
            lastChargeGainTick.remove(player.getUniqueId());
            return 0;
        }

        long lastGainTick = lastChargeGainTick.computeIfAbsent(player.getUniqueId(), ignored -> currentTick);
        if (currentTick - lastGainTick < intervalTicks) {
            return currentCharges;
        }

        int decayedCharges = Math.max(0, currentCharges - Math.max(0, amount));
        setCharges(player, decayedCharges);
        if (decayedCharges == 0) {
            lastChargeGainTick.remove(player.getUniqueId());
        } else {
            lastChargeGainTick.put(player.getUniqueId(), currentTick);
        }
        return decayedCharges;
    }

    public int decay(Player player, long currentTick, long intervalTicks, int amount, int maximum) {
        if (getCharges(player) >= Math.max(0, maximum)) {
            return getCharges(player);
        }
        return decay(player, currentTick, intervalTicks, amount);
    }

    public void clearTransientState(UUID playerId) {
        lastChargeGainTick.remove(playerId);
    }

    public void clear() {
        lastChargeGainTick.clear();
    }

    public record ChargeResult(int charges, boolean added) {
    }
}
