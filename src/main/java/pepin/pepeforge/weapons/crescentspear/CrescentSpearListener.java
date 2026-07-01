package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.protection.ProtectionUtil;
import pepin.pepeforge.util.ui.ActionBarHelper;
import pepin.pepeforge.weapons.crescent.CrescentMoonPower;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public final class CrescentSpearListener implements Listener {

    private static final double ACTIVE_FRONT_ARC_DOT = 0.5D;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final Map<UUID, Integer> charge = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastChargeGainTick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCountedTick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> specialAttackUntilTick = new ConcurrentHashMap<>();
    private final Set<UUID> armedPlayers = ConcurrentHashMap.newKeySet();

    public CrescentSpearListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
    }

    private ScheduledTaskCompat statusTask;

    public void startStatusTask() {
        statusTask = SchedulerCompat.runTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    UUID playerId = player.getUniqueId();
                    int currentCharge = charge.getOrDefault(playerId, 0);
                    boolean armed = armedPlayers.contains(playerId);
                    boolean holdingSpear = itemFactory.isCrescentSpear(player.getInventory().getItemInMainHand());
                    if (holdingSpear) {
                        if (armed) {
                            showReadyActionBar(player);
                        } else if (currentCharge > 0) {
                            showChargeActionBar(player, currentCharge);
                        }
                    }

                    if (currentCharge > 0 && !armed) {
                        long currentTick = player.getWorld().getGameTime();
                        long lastGainTick = lastChargeGainTick.getOrDefault(playerId, Long.MIN_VALUE);
                        if (currentTick - lastGainTick < CrescentSpearDefinition.CHARGE_DECAY_DELAY_TICKS) {
                            return;
                        }

                        int decayedCharge = Math.max(0,
                                currentCharge - CrescentSpearDefinition.CHARGE_DECAY_PER_INTERVAL);
                        if (decayedCharge == 0) {
                            charge.remove(playerId);
                            lastChargeGainTick.remove(playerId);
                        } else {
                            charge.put(playerId, decayedCharge);
                        }
                    }
                });
            }
        }, 1L, CrescentSpearDefinition.STATUS_INTERVAL_TICKS);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!itemFactory.isCrescentSpear(weapon)) {
            return;
        }

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity) || target == player) {
            return;
        }

        event.setDamage(Math.max(0.0D, event.getDamage() + CrescentMoonPower.getDamageModifier(player)));

        UUID playerId = player.getUniqueId();
        long currentTick = player.getWorld().getGameTime();
        long specialUntilTick = specialAttackUntilTick.getOrDefault(playerId, -1L);
        if (specialUntilTick >= currentTick) {
            return;
        }
        specialAttackUntilTick.remove(playerId);

        if (armedPlayers.remove(playerId)) {
            charge.remove(playerId);
            lastChargeGainTick.remove(playerId);
            triggerActiveSkill(player);
            return;
        }

        Long previousTick = lastCountedTick.get(playerId);
        // One spear swing can clip multiple targets in the same tick. Count it once
        // so the charge meter cannot jump several steps from one attack animation.
        if (previousTick != null && previousTick == currentTick) {
            return;
        }

        lastCountedTick.put(playerId, currentTick);
        lastChargeGainTick.put(playerId, currentTick);
        int nextCharge = Math.min(
                CrescentSpearDefinition.CHARGE_MAX,
                charge.getOrDefault(playerId, 0) + CrescentSpearDefinition.CHARGE_PER_HIT);
        charge.put(playerId, nextCharge);

        if (nextCharge >= CrescentSpearDefinition.CHARGE_MAX) {
            armedPlayers.add(playerId);
            showReadyActionBar(player);
        } else {
            showChargeActionBar(player, nextCharge);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        charge.remove(playerId);
        lastChargeGainTick.remove(playerId);
        lastCountedTick.remove(playerId);
        specialAttackUntilTick.remove(playerId);
        armedPlayers.remove(playerId);
    }

    private void triggerActiveSkill(Player player) {
        Location effectPoint = player.getEyeLocation().add(player.getLocation().getDirection().normalize()
                .multiply(CrescentSpearDefinition.ACTIVE_PARTICLE_DISTANCE));

        playSpecialEffects(player.getWorld(), effectPoint);

        UUID playerId = player.getUniqueId();
        specialAttackUntilTick.put(
                playerId,
                player.getWorld().getGameTime()
                        + CrescentSpearDefinition.ACTIVE_FIRST_HIT_DELAY_TICKS
                        + (long) ((CrescentSpearDefinition.ACTIVE_HIT_COUNT - 1)
                                * CrescentSpearDefinition.ACTIVE_HIT_INTERVAL_TICKS));

        double hitDamage = resolveActiveHitDamage(player);
        for (int i = 0; i < CrescentSpearDefinition.ACTIVE_HIT_COUNT; i++) {
            SchedulerCompat.runLaterForPlayer(player, plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                playActiveSwingVisuals(player);

                LivingEntity target = findActiveTarget(player);
                if (target == null || target.isDead() || !target.isValid()) {
                    return;
                }
                if (!ProtectionUtil.canDamage(player, target)) {
                    return;
                }

                playSpecialEffects(player.getWorld(), target.getLocation().add(0.0D, 1.0D, 0.0D));
                target.setNoDamageTicks(0);
                target.damage(hitDamage, player);
            }, CrescentSpearDefinition.ACTIVE_FIRST_HIT_DELAY_TICKS
                    + (long) i * CrescentSpearDefinition.ACTIVE_HIT_INTERVAL_TICKS);
        }
    }

    private LivingEntity findActiveTarget(Player player) {
        LivingEntity bestTarget = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        Vector lookDirection = player.getEyeLocation().getDirection().normalize();

        for (Entity entity : player.getNearbyEntities(
                CrescentSpearDefinition.ACTIVE_TARGET_RANGE,
                CrescentSpearDefinition.ACTIVE_TARGET_RANGE,
                CrescentSpearDefinition.ACTIVE_TARGET_RANGE)) {
            if (!(entity instanceof LivingEntity livingTarget) || livingTarget == player || livingTarget.isDead()) {
                continue;
            }

            Vector toTarget = livingTarget.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
            double distanceSquared = toTarget.lengthSquared();
            if (distanceSquared > CrescentSpearDefinition.ACTIVE_TARGET_RANGE
                    * CrescentSpearDefinition.ACTIVE_TARGET_RANGE) {
                continue;
            }

            Vector directionToTarget = toTarget.clone().normalize();
            if (lookDirection.dot(directionToTarget) < ACTIVE_FRONT_ARC_DOT) {
                continue;
            }

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestTarget = livingTarget;
            }
        }
        return bestTarget;
    }

    private void playActiveSwingVisuals(Player player) {
        player.swingMainHand();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector sweepDirection = player.getEyeLocation().getDirection().clone().setY(0.0D);
        if (sweepDirection.lengthSquared() < 1.0E-6D) {
            sweepDirection = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            sweepDirection.normalize();
        }

        double angleRadians = Math.toRadians(random.nextDouble(-22.0D, 22.0D));
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        Vector rotatedDirection = new Vector(
                sweepDirection.getX() * cos - sweepDirection.getZ() * sin,
                0.0D,
                sweepDirection.getX() * sin + sweepDirection.getZ() * cos);

        double distance = random.nextDouble(
                CrescentSpearDefinition.ACTIVE_PARTICLE_DISTANCE - 0.25D,
                CrescentSpearDefinition.ACTIVE_PARTICLE_DISTANCE + 0.15D);
        double verticalOffset = random.nextDouble(-0.08D, 0.08D);
        Location slashPoint = player.getEyeLocation()
                .add(rotatedDirection.multiply(distance))
                .add(0.0D, verticalOffset, 0.0D);
        World world = player.getWorld();
        world.spawnParticle(Particle.SWEEP_ATTACK, slashPoint, 1, 0.08D, 0.08D, 0.08D, 0.0D);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.55f, 1.55f);
    }

    private double resolveActiveHitDamage(Player player) {
        if (player.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
            return 4.0D;
        }
        return Math.max(1.0D, player.getAttribute(Attribute.ATTACK_DAMAGE).getValue());
    }

    private void playSpecialEffects(World world, Location point) {
        world.spawnParticle(
                Particle.END_ROD,
                point,
                2,
                0.03D, 0.03D, 0.03D,
                0.01D);
        world.playSound(
                point,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.8f,
                1.4f);
    }

    private void showChargeActionBar(Player player, int currentCharge) {
        double progress = Math.max(0.0D, Math.min(1.0D, (double) currentCharge / CrescentSpearDefinition.CHARGE_MAX));
        String message = lang.text("messages.crescent_spear.charge")
                .replace("{bar}", ActionBarHelper.buildProgressBar(progress));
        ActionBarHelper.showActionBar(player, message);
    }

    private void showReadyActionBar(Player player) {
        String message = lang.text("messages.crescent_spear.ready")
                .replace("{bar}", ActionBarHelper.buildProgressBar(1.0D));
        ActionBarHelper.showActionBar(player, message);
    }

}
