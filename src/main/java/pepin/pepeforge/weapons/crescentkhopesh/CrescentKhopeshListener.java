package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.aura.AuraManager;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TemporaryShieldManager;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.crescent.CrescentMoonPower;

public final class CrescentKhopeshListener implements Listener {
    private static final String CONFIG_PATH = "mechanics.crescent_khopesh.";
    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final TemporaryShieldManager shield;
    private final AuraManager auraManager;
    private final Map<UUID, Integer> hits = new ConcurrentHashMap<>();
    private final Map<UUID, Object> activations = new ConcurrentHashMap<>();
    private final Map<UUID, MoonSweepAuraEffect> sweeps = new ConcurrentHashMap<>();
    private final Set<MoonWave> waves = ConcurrentHashMap.newKeySet();
    private final Object lifecycleLock = new Object();
    private volatile boolean running = true;

    public CrescentKhopeshListener(JavaPlugin plugin, ItemFactory itemFactory, TemporaryShieldManager shield,
            AuraManager auraManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.shield = shield;
        this.auraManager = auraManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrimaryDamage(EntityDamageByEntityEvent event) {
        if (!running || event.isCancelled() || DamageFlow.isSecondaryDamage(event)
                || !(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target) || target == player
                || !SchedulerCompat.isOwnedByCurrentRegion(player)
                || !SchedulerCompat.isOwnedByCurrentRegion(target)
                || !itemFactory.isCrescentKhopesh(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setDamage(Math.max(0.0D, event.getDamage() + CrescentMoonPower.getDamageModifier(player)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!running || event.isCancelled() || DamageFlow.isSecondaryDamage(event)
                || event.getCause() != DamageCause.ENTITY_ATTACK
                || !(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target) || target == player
                || !SchedulerCompat.isOwnedByCurrentRegion(player)
                || !SchedulerCompat.isOwnedByCurrentRegion(target)
                || !itemFactory.isCrescentKhopesh(player.getInventory().getItemInMainHand())
                || !itemFactory.isItemEnabled(CrescentKhopeshDefinition.ITEM_ID)) {
            return;
        }
        int required = Math.max(1, Math.min(100, plugin.getConfig().getInt(CONFIG_PATH + "hits_required",
                CrescentKhopeshDefinition.DEFAULT_HITS_REQUIRED)));
        UUID playerId = player.getUniqueId();
        int count = hits.merge(playerId, 1, Integer::sum);
        if (count >= required) {
            hits.remove(playerId);
            activate(player, target.getUniqueId());
        }
    }

    void activate(Player player, UUID excludedTarget) {
        synchronized (lifecycleLock) {
            if (!running) {
                return;
            }
            double shieldGain = CrescentKhopeshDefinition.SHIELD_ABSORPTION_PER_HIT;
            double shieldLimit = value("shield_absorption", CrescentKhopeshDefinition.DEFAULT_SHIELD_ABSORPTION,
                    0.0D, 40.0D);
            UUID playerId = player.getUniqueId();
            Object activation = activations.computeIfAbsent(playerId, ignored -> new Object());
            Location activationLocation = player.getLocation().clone();
            Runnable acceptedHit = () -> {
                synchronized (lifecycleLock) {
                    if (running && activations.get(playerId) == activation) {
                        shield.grant(player, CrescentKhopeshDefinition.SHIELD_SOURCE_ID, shieldGain, shieldLimit,
                                CrescentKhopeshDefinition.SHIELD_DURATION_TICKS);
                    }
                }
            };
            // The accepted triggering hit counts once, without additional damage to its target.
            shield.grant(player, CrescentKhopeshDefinition.SHIELD_SOURCE_ID, shieldGain, shieldLimit,
                    CrescentKhopeshDefinition.SHIELD_DURATION_TICKS);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.3F, 1.35F);
            double damage = CrescentKhopeshDefinition.BASE_ATTACK_DAMAGE
                    * CrescentKhopeshDefinition.WAVE_DAMAGE_MULTIPLIER;
            if (MoonstrikeMode.fromConfig(plugin.getConfig()) == MoonstrikeMode.SWEEP) {
                double width = value("sweep_width", CrescentKhopeshDefinition.DEFAULT_SWEEP_WIDTH, 0.1D, 16.0D);
                double range = value("sweep_range", CrescentKhopeshDefinition.DEFAULT_SWEEP_RANGE, 0.1D, 16.0D);
                MoonSweepAuraEffect effect = sweeps.get(playerId);
                if (effect == null || effect.isExpired()) {
                    effect = new MoonSweepAuraEffect(plugin);
                    sweeps.put(playerId, effect);
                }
                effect.addSweep(player, activationLocation, width, range);
                if (!running || activations.get(playerId) != activation) {
                    effect.stop();
                    return;
                }
                auraManager.addOrExtendActiveAura(player, effect, 10);
                new MoonSweep(player, activationLocation, excludedTarget, damage, width, range, acceptedHit,
                        () -> running && activations.get(playerId) == activation).strike();
                return;
            }
            MoonWave wave = new MoonWave(plugin, player, excludedTarget,
                    damage,
                    value("wave_range", CrescentKhopeshDefinition.DEFAULT_WAVE_RANGE, 0.1D, 16.0D),
                    value("wave_width", CrescentKhopeshDefinition.DEFAULT_WAVE_WIDTH, 0.1D, 16.0D),
                    value("wave_speed", CrescentKhopeshDefinition.DEFAULT_WAVE_SPEED, 0.05D, 16.0D),
                    acceptedHit, waves::remove);
            waves.add(wave);
            wave.start();
        }
    }

    private double value(String key, double fallback, double min, double max) {
        double configured = plugin.getConfig().getDouble(CONFIG_PATH + key, fallback);
        return Double.isFinite(configured) ? Math.max(min, Math.min(max, configured)) : fallback;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clear(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
    }

    private void clear(Player player) {
        synchronized (lifecycleLock) {
            UUID playerId = player.getUniqueId();
            activations.remove(playerId);
            hits.remove(playerId);
            waves.stream().filter(wave -> wave.ownerId().equals(playerId)).forEach(MoonWave::stop);
            auraManager.removeActiveAura(player, MoonSweepAuraEffect.class);
            MoonSweepAuraEffect effect = sweeps.remove(playerId);
            if (effect != null) {
                effect.stop();
            }
            shield.clearSource(player, CrescentKhopeshDefinition.SHIELD_SOURCE_ID);
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            running = false;
            waves.forEach(MoonWave::stop);
            waves.clear();
            sweeps.forEach((playerId, effect) -> {
                auraManager.removeActiveAura(playerId, MoonSweepAuraEffect.class);
                effect.stop();
            });
            sweeps.clear();
            activations.clear();
            hits.clear();
        }
    }
}
