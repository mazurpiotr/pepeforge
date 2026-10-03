package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
import pepin.pepeforge.util.charge.ChargeManager;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.ui.ActionBarHelper;
import pepin.pepeforge.weapons.crescent.CrescentMoonPower;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public final class CrescentSpearListener implements Listener {

    private static final double ACTIVE_FRONT_ARC_DOT = 0.5D;
    private static final String CONFIG_PATH = "mechanics.crescent_spear";

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final ChargeManager chargeManager;
    private final Map<UUID, Long> specialAttackUntilTick = new ConcurrentHashMap<>();
    private final Object taskLock = new Object();
    private final Map<UUID, Set<ScheduledTaskCompat>> activeSkillTasks = new ConcurrentHashMap<>();

    public CrescentSpearListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.chargeManager = new ChargeManager(
            new NamespacedKey(plugin, CrescentSpearDefinition.CHARGES_KEY_STRING));
    }

    private int getChargesRequired() {
        return Math.max(1, Math.min(100, plugin.getConfig().getInt(
                CONFIG_PATH + ".charges_required", CrescentSpearDefinition.DEFAULT_CHARGES_REQUIRED)));
    }

    private int getActiveHitCount() {
        return Math.max(1, Math.min(20, plugin.getConfig().getInt(
                CONFIG_PATH + ".active_hit_count", CrescentSpearDefinition.DEFAULT_ACTIVE_HIT_COUNT)));
    }

    private ScheduledTaskCompat statusTask;
    private volatile long lifecycleGeneration;
    private volatile boolean running;

    public void startStatusTask() {
        synchronized (taskLock) {
            if (running) {
                return;
            }
            long generation = lifecycleGeneration + 1L;
            ScheduledTaskCompat scheduledTask = SchedulerCompat.runTimer(plugin, () -> {
                if (!isRunning(generation)) {
                    return;
                }
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!isRunning(generation)) {
                        return;
                    }
                    SchedulerCompat.runForPlayer(player, plugin, () -> {
                        if (!isRunning(generation) || !player.isOnline()) {
                            return;
                        }
                        UUID playerId = player.getUniqueId();
                        int currentCharge = chargeManager.getCharges(player);
                        int chargesRequired = getChargesRequired();
                        if (currentCharge > 0 && currentCharge < chargesRequired) {
                            long currentTick = player.getWorld().getGameTime();
                            currentCharge = chargeManager.decay(player, currentTick,
                                    CrescentSpearDefinition.CHARGE_DECAY_DELAY_TICKS,
                                    CrescentSpearDefinition.CHARGE_DECAY_PER_INTERVAL,
                                    chargesRequired);
                        }
                        boolean holdingSpear = itemFactory.isCrescentSpear(player.getInventory().getItemInMainHand());
                        if (holdingSpear) {
                            if (currentCharge >= chargesRequired) {
                                showReadyActionBar(player);
                            } else if (currentCharge > 0) {
                                showChargeActionBar(player, currentCharge);
                            }
                        }
                    });
                }
            }, 1L, CrescentSpearDefinition.STATUS_INTERVAL_TICKS);
            lifecycleGeneration = generation;
            running = true;
            statusTask = scheduledTask;
        }
    }

    public void stop() {
        synchronized (taskLock) {
            running = false;
            lifecycleGeneration++;
            if (statusTask != null) {
                statusTask.cancel();
                statusTask = null;
            }
            activeSkillTasks.values().forEach(tasks -> tasks.forEach(ScheduledTaskCompat::cancel));
            activeSkillTasks.clear();
            specialAttackUntilTick.clear();
            chargeManager.clear();
        }
    }

    private boolean isRunning(long generation) {
        return running && lifecycleGeneration == generation;
    }

    private ScheduledTaskCompat schedulePlayerTask(
            Player player,
            Runnable runner,
            long generation,
            long delayTicks) {
        synchronized (taskLock) {
            if (!isRunning(generation)) {
                return null;
            }
            ScheduledTaskCompat task = SchedulerCompat.runLaterForPlayer(player, plugin, runner, delayTicks);
            activeSkillTasks.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(task);
            return task;
        }
    }

    private void finishPlayerTask(UUID playerId, ScheduledTaskCompat task) {
        if (task == null) {
            return;
        }
        Set<ScheduledTaskCompat> tasks = activeSkillTasks.get(playerId);
        if (tasks != null && tasks.remove(task) && tasks.isEmpty()) {
            activeSkillTasks.remove(playerId, tasks);
        }
    }

    private void cancelPlayerTasks(UUID playerId) {
        Set<ScheduledTaskCompat> tasks = activeSkillTasks.remove(playerId);
        if (tasks != null) {
            tasks.forEach(ScheduledTaskCompat::cancel);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!running || DamageFlow.isSecondaryDamage(event) || !(event.getDamager() instanceof Player player)) {
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
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulDamage(EntityDamageByEntityEvent event) {
        if (!running || DamageFlow.isSecondaryDamage(event) || !(event.getDamager() instanceof Player player)) {
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


        UUID playerId = player.getUniqueId();
        long currentTick = player.getWorld().getGameTime();
        long specialUntilTick = specialAttackUntilTick.getOrDefault(playerId, -1L);
        if (specialUntilTick >= currentTick) {
            return;
        }
        specialAttackUntilTick.remove(playerId);

        int chargesRequired = getChargesRequired();
        if (chargeManager.getCharges(player) >= chargesRequired) {
            chargeManager.reset(player);
            triggerActiveSkill(player);
            return;
        }

        ChargeManager.ChargeResult chargeResult = chargeManager.addChargeOncePerTick(
                player, chargesRequired, currentTick);
        if (!chargeResult.added()) {
            return;
        }

        int nextCharge = chargeResult.charges();

        if (nextCharge >= chargesRequired) {
            showReadyActionBar(player);
        } else {
            showChargeActionBar(player, nextCharge);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelPlayerTasks(playerId);
        chargeManager.clearTransientState(playerId);
        specialAttackUntilTick.remove(playerId);
    }

    private void triggerActiveSkill(Player player) {
        if (!running) {
            return;
        }
        long generation = lifecycleGeneration;
        Location effectPoint = player.getEyeLocation().add(player.getLocation().getDirection().normalize()
                .multiply(CrescentSpearDefinition.ACTIVE_PARTICLE_DISTANCE));

        playSpecialEffects(player.getWorld(), effectPoint);

        UUID playerId = player.getUniqueId();
        int activeHitCount = getActiveHitCount();
        specialAttackUntilTick.put(
                playerId,
                player.getWorld().getGameTime()
                        + CrescentSpearDefinition.ACTIVE_FIRST_HIT_DELAY_TICKS
                + (long) ((activeHitCount - 1)
                                * CrescentSpearDefinition.ACTIVE_HIT_INTERVAL_TICKS));

        double hitDamage = resolveActiveHitDamage(player);
        for (int i = 0; i < activeHitCount; i++) {
            int hitIndex = i;
            class ActiveHitTask implements Runnable {
                private ScheduledTaskCompat taskRef;

                @Override
                public void run() {
                    try {
                        if (!isRunning(generation) || !player.isOnline()) {
                            return;
                        }
                        playActiveSwingVisuals(player);

                        if (!isRunning(generation)) {
                            return;
                        }
                        LivingEntity target = findActiveTarget(player);
                        if (target == null || target.isDead() || !target.isValid()
                                || !isRunning(generation)) {
                            return;
                        }
                        if (!DamageFlow.damage(target, hitDamage, player, true).accepted()
                                || !isRunning(generation)) {
                            return;
                        }
                        playSpecialEffects(player.getWorld(), target.getLocation().add(0.0D, 1.0D, 0.0D));
                        if (hitIndex == activeHitCount - 1) {
                            applyLastHitKnockback(player, target);
                        }
                    } finally {
                        finishPlayerTask(playerId, taskRef);
                    }
                }
            }

            ActiveHitTask task = new ActiveHitTask();
            task.taskRef = schedulePlayerTask(
                    player,
                    task,
                    generation,
                    CrescentSpearDefinition.ACTIVE_FIRST_HIT_DELAY_TICKS
                            + (long) hitIndex * CrescentSpearDefinition.ACTIVE_HIT_INTERVAL_TICKS);
        }
    }

    private void applyLastHitKnockback(Player player, LivingEntity target) {
        Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector());
        push.setY(0.0D);
        if (push.lengthSquared() < 0.001D) {
            push = player.getLocation().getDirection().setY(0.0D);
        }
        if (push.lengthSquared() < 0.001D) {
            return;
        }

        Vector velocity = target.getVelocity().add(push.normalize().multiply(
                CrescentSpearDefinition.ACTIVE_LAST_HIT_KNOCKBACK));
        velocity.setY(Math.max(velocity.getY(), CrescentSpearDefinition.ACTIVE_LAST_HIT_LIFT));
        target.setVelocity(velocity);
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
        double progress = Math.max(0.0D, Math.min(1.0D, (double) currentCharge / getChargesRequired()));
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
