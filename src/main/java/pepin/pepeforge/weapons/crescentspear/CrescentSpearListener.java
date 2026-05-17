package pepin.pepeforge.weapons.crescentspear;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CrescentSpearListener implements Listener {

    private static final long COMBO_WINDOW_MILLIS = CrescentSpearDefinition.COMBO_WINDOW_TICKS * 50L;
    private static final long PROC_FEEDBACK_MILLIS = CrescentSpearDefinition.PROC_FEEDBACK_TICKS * 50L;
    private static final int COMBO_BAR_SEGMENTS = 20;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final Map<UUID, Integer> hitCounter = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    private final Map<UUID, Long> lastCountedTick = new HashMap<>();
    private final Map<UUID, Long> procUntil = new HashMap<>();

    public CrescentSpearListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
    }

    public void startStatusTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!itemFactory.isCrescentSpear(player.getInventory().getItemInMainHand())) {
                    continue;
                }

                UUID playerId = player.getUniqueId();
                long procUntilMillis = procUntil.getOrDefault(playerId, 0L);
                if (procUntilMillis > now) {
                    showProcActionBar(player, procUntilMillis - now);
                    continue;
                }
                procUntil.remove(playerId);

                int comboCount = hitCounter.getOrDefault(playerId, 0);
                if (comboCount <= 0) {
                    continue;
                }

                long lastHitMillis = lastHitAt.getOrDefault(playerId, 0L);
                long elapsed = now - lastHitMillis;
                if (elapsed > COMBO_WINDOW_MILLIS) {
                    hitCounter.remove(playerId);
                    lastHitAt.remove(playerId);
                    continue;
                }

                showComboActionBar(player, comboCount, COMBO_WINDOW_MILLIS - elapsed);
            }
        }, 1L, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!itemFactory.isCrescentSpear(weapon)) {
            return;
        }

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity livingTarget) || target == player) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTick = player.getWorld().getGameTime();
        Long previousTick = lastCountedTick.get(playerId);
        // One spear swing can clip multiple targets in the same tick. Count it once
        // so the combo cannot jump several steps from a single attack animation.
        if (previousTick != null && previousTick == currentTick) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastHitMillis = lastHitAt.getOrDefault(playerId, 0L);

        int comboCount;
        if (now - lastHitMillis > COMBO_WINDOW_MILLIS) {
            comboCount = 1;
        } else {
            comboCount = hitCounter.getOrDefault(playerId, 0) + 1;
        }

        hitCounter.put(playerId, comboCount);
        lastHitAt.put(playerId, now);
        lastCountedTick.put(playerId, currentTick);
        if (comboCount < CrescentSpearDefinition.HIT_COMBO_TRIGGER) {
            return;
        }

        event.setDamage(event.getDamage() + CrescentSpearDefinition.PROC_DAMAGE_BONUS);
        hitCounter.remove(playerId);
        lastHitAt.remove(playerId);
        procUntil.put(playerId, now + PROC_FEEDBACK_MILLIS);
        launchTarget(livingTarget);
        applySpeedIfBetter(player);
        playLaunchEffects(player.getWorld(), livingTarget);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        hitCounter.remove(playerId);
        lastHitAt.remove(playerId);
        lastCountedTick.remove(playerId);
        procUntil.remove(playerId);
    }

    private void launchTarget(LivingEntity target) {
        Vector velocity = target.getVelocity().clone();
        velocity.setY(Math.max(velocity.getY(), CrescentSpearDefinition.LAUNCH_UPWARD_VELOCITY));
        target.setVelocity(velocity);
    }

    private void applySpeedIfBetter(Player player) {
        PotionEffect candidate = new PotionEffect(
                PotionEffectType.SPEED,
                CrescentSpearDefinition.SPEED_DURATION_TICKS,
                CrescentSpearDefinition.SPEED_AMPLIFIER,
                true,
                false,
                true
        );

        PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
        if (current != null) {
            if (current.getAmplifier() > candidate.getAmplifier()) {
                return;
            }
            if (current.getAmplifier() == candidate.getAmplifier()
                    && current.getDuration() >= candidate.getDuration()) {
                return;
            }
        }

        player.addPotionEffect(candidate);
    }

    private void playLaunchEffects(World world, LivingEntity target) {
        world.playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.95f, 1.1f);
        world.spawnParticle(Particle.GUST_EMITTER_LARGE, target.getLocation().add(0.0, 0.9, 0.0), 1, 0.0, 0.0, 0.0, 0.0);
    }

    private void showComboActionBar(Player player, int comboCount, long remainingMillis) {
        double progress = Math.max(0.0D, Math.min(1.0D, (double) remainingMillis / COMBO_WINDOW_MILLIS));
        String bar = buildProgressBar(progress);
        String message = lang.text("messages.crescent_spear.combo")
                .replace("{hits}", comboCount + "/" + CrescentSpearDefinition.HIT_COMBO_TRIGGER)
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", remainingMillis / 1000.0D));
        showActionBar(player, message);
    }

    private void showProcActionBar(Player player, long remainingMillis) {
        double progress = Math.max(0.0D, Math.min(1.0D, (double) remainingMillis / PROC_FEEDBACK_MILLIS));
        String bar = buildProgressBar(progress);
        String message = lang.text("messages.crescent_spear.proc")
                .replace("{hits}", CrescentSpearDefinition.HIT_COMBO_TRIGGER + "/" + CrescentSpearDefinition.HIT_COMBO_TRIGGER)
                .replace("{bar}", bar);
        showActionBar(player, message);
    }

    private void showActionBar(Player player, String message) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message))
        );
    }

    private String buildProgressBar(double progress) {
        int filled = (int) Math.round(progress * COMBO_BAR_SEGMENTS);
        StringBuilder bar = new StringBuilder("&b");
        for (int i = 0; i < COMBO_BAR_SEGMENTS; i++) {
            if (i == filled) {
                bar.append("&7");
            }
            bar.append('|');
        }
        return bar.toString();
    }
}
