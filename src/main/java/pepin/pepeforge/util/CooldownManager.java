package pepin.pepeforge.util;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownManager {

    private static final String DEFAULT_KEY = "default";

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public void setCooldown(Player player, long durationMillis) {
        setCooldown(player, DEFAULT_KEY, durationMillis);
    }

    public void setCooldown(Player player, String key, long durationMillis) {
        cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(key, System.currentTimeMillis() + durationMillis);
    }

    public long getRemainingCooldownMillis(Player player) {
        return getRemainingCooldownMillis(player, DEFAULT_KEY);
    }

    public long getRemainingCooldownMillis(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0L;
        }

        long remainingMillis = playerCooldowns.getOrDefault(key, 0L) - System.currentTimeMillis();
        if (remainingMillis > 0L) {
            return remainingMillis;
        }

        playerCooldowns.remove(key);
        if (playerCooldowns.isEmpty()) {
            cooldowns.remove(player.getUniqueId());
        }
        return 0L;
    }

    public double getRemainingCooldownSeconds(Player player) {
        return getRemainingCooldownMillis(player, DEFAULT_KEY) / 1000.0D;
    }

    public double getRemainingCooldownSeconds(Player player, String key) {
        return getRemainingCooldownMillis(player, key) / 1000.0D;
    }

    public boolean isOnCooldown(Player player) {
        return isOnCooldown(player, DEFAULT_KEY);
    }

    public boolean isOnCooldown(Player player, String key) {
        return getRemainingCooldownMillis(player, key) > 0L;
    }

    public void clearCooldown(Player player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return;
        }

        playerCooldowns.remove(key);
        if (playerCooldowns.isEmpty()) {
            cooldowns.remove(player.getUniqueId());
        }
    }

    public void clearPlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public void clearAll() {
        cooldowns.clear();
    }
}
