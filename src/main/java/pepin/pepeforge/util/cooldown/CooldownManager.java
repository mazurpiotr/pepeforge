package pepin.pepeforge.util.cooldown;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager implements Listener {

    private static final String DEFAULT_KEY = "default";

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer());
    }

    public void setCooldown(Player player, long durationMillis) {
        setCooldown(player, DEFAULT_KEY, durationMillis);
    }

    public void setCooldown(Player player, String key, long durationMillis) {
        cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
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
