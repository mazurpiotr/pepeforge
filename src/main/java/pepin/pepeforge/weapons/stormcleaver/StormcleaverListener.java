package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.charge.ChargeManager;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.ActionBarHelper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StormcleaverListener implements Listener {

    private static final String CHARGES_CONFIG_PATH = "mechanics.stormcleaver.charges_required";
    private static final String JUMP_MULTIPLIER_CONFIG_PATH = "mechanics.stormcleaver.jump_velocity_multiplier";
    private static final String CHARGE_DECAY_INTERVAL_CONFIG_PATH = "mechanics.stormcleaver.charge_decay_interval";
    private static final int STATUS_INTERVAL_TICKS = 5;
    private static final int MAX_LEAP_TICKS = 80;
    private static final int LEAP_ARMING_TICKS = 2;
    private static final int MAX_LEAP_CLEARANCE_BLOCKS = 8;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final ChargeManager chargeManager;
    private final Map<UUID, LeapState> activeLeaps = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTaskCompat> leapTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fallDamageSuppression = new ConcurrentHashMap<>();
    private final Set<UUID> suppressedChargePlayers = ConcurrentHashMap.newKeySet();
    private ScheduledTaskCompat statusTask;

    public StormcleaverListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.chargeManager = new ChargeManager(
            new org.bukkit.NamespacedKey(plugin, StormcleaverDefinition.CHARGES_KEY_STRING));
    }

    public void startStatusTask() {
        statusTask = SchedulerCompat.runTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                SchedulerCompat.runForPlayer(player, plugin, () -> updateChargeState(player));
            }
        }, 1L, STATUS_INTERVAL_TICKS);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        chargeManager.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)
                || target == player
                || !itemFactory.isStormcleaver(player.getInventory().getItemInMainHand())
                || !hasEmptyOffHand(player)
                || DamageFlow.isSecondaryDamage(event)
                || suppressedChargePlayers.contains(player.getUniqueId())) {
            return;
        }

        addCharge(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if ((action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
                || event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (activeLeaps.containsKey(player.getUniqueId())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!itemFactory.isStormcleaver(item) || !hasEmptyOffHand(player) || !player.isOnGround()) {
            return;
        }

        int requiredCharges = getRequiredCharges();
        if (chargeManager.getCharges(player) < requiredCharges) {
            return;
        }

        denyInteraction(event);
        chargeManager.reset(player);
        if (hasLeapClearance(player)) {
            startLeap(player);
        } else {
            triggerImpact(player);
        }
        player.setCooldown(item.getType(), 10);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (activeLeaps.containsKey(playerId) || fallDamageSuppression.containsKey(playerId)) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cleanupPlayer(event.getEntity().getUniqueId());
    }

    public void cleanup() {
        stop();
        for (UUID playerId : activeLeaps.keySet()) {
            cleanupPlayer(playerId);
        }
        activeLeaps.clear();
        fallDamageSuppression.clear();
        chargeManager.clear();
        suppressedChargePlayers.clear();
    }

    private void addCharge(Player player) {
        int requiredCharges = getRequiredCharges();
        int currentCharges = chargeManager.getCharges(player);
        if (currentCharges >= requiredCharges) {
            showCharge(player, currentCharges, requiredCharges);
            return;
        }

        int nextCharges = chargeManager.addCharge(player, requiredCharges, player.getWorld().getGameTime());
        showCharge(player, nextCharges, requiredCharges);

        if (currentCharges < requiredCharges && nextCharges == requiredCharges) {
            showFullChargeEffect(player);
        }
    }

    private void updateChargeState(Player player) {
        if (!player.isOnline()) {
            return;
        }

        int charges = chargeManager.getCharges(player);
        if (charges <= 0) {
            return;
        }

        long currentTick = player.getWorld().getGameTime();
        charges = chargeManager.decay(player, currentTick, getChargeDecayInterval(), 1);

        if (itemFactory.isStormcleaver(player.getInventory().getItemInMainHand()) && charges > 0) {
            showCharge(player, charges, getRequiredCharges());
        }
    }

    private void showFullChargeEffect(Player player) {
        Location center = player.getLocation().add(0.0D, 1.0D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.55f, 1.7f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 36,
                0.45D, 0.7D, 0.45D, 0.06D);
        player.getWorld().spawnParticle(Particle.END_ROD, center, 16,
                0.25D, 0.65D, 0.25D, 0.03D);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0D, 0.15D, 0.0D), 24,
                0.55D, 0.08D, 0.55D, 0.12D);
    }

    private void startLeap(Player player) {
        UUID playerId = player.getUniqueId();
        cleanupPlayer(playerId);

        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() > 0.001D) {
            direction.normalize();
        }
        player.setVelocity(direction.multiply(StormcleaverDefinition.LEAP_HORIZONTAL_SPEED)
                .setY(StormcleaverDefinition.CHARGE_JUMP_VELOCITY * getJumpVelocityMultiplier()));
        player.setFallDistance(0.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);

        LeapState state = new LeapState(player, 0);
        activeLeaps.put(playerId, state);
        ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, () -> tickLeap(state), 1L, 1L);
        leapTasks.put(playerId, task);
    }

    private void tickLeap(LeapState state) {
        Player player = state.player();
        UUID playerId = player.getUniqueId();
        int tick = state.tick() + 1;
        state.setTick(tick);

        if (!player.isOnline() || player.isDead() || tick > MAX_LEAP_TICKS) {
            cleanupPlayer(playerId);
            return;
        }

        player.setFallDistance(0.0F);
        if (tick > LEAP_ARMING_TICKS && player.isOnGround()) {
            triggerImpact(player);
            stopLeap(playerId);
        }
    }

    private boolean hasLeapClearance(Player player) {
        var bounds = player.getBoundingBox();
        int minX = (int) Math.floor(bounds.getMinX());
        int maxX = (int) Math.floor(bounds.getMaxX());
        int minZ = (int) Math.floor(bounds.getMinZ());
        int maxZ = (int) Math.floor(bounds.getMaxZ());
        int baseY = (int) Math.floor(bounds.getMinY());

        for (int y = baseY + 1; y <= baseY + MAX_LEAP_CLEARANCE_BLOCKS; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!player.getWorld().getBlockAt(x, y, z).isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void triggerImpact(Player player) {
        UUID playerId = player.getUniqueId();
        fallDamageSuppression.put(playerId, System.currentTimeMillis() + 1_000L);
        SchedulerCompat.runLaterForPlayer(player, plugin, () -> fallDamageSuppression.remove(playerId), 20L);

        Location impact = player.getLocation();
        player.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.25f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, impact.clone().add(0.0D, 0.2D, 0.0D), 1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impact.clone().add(0.0D, 0.5D, 0.0D),
                36, 1.0D, 0.35D, 1.0D, 0.08D);

        for (Entity entity : player.getWorld().getNearbyEntities(impact, StormcleaverDefinition.SHOCKWAVE_RADIUS,
                StormcleaverDefinition.SHOCKWAVE_RADIUS, StormcleaverDefinition.SHOCKWAVE_RADIUS)) {
            if (!(entity instanceof LivingEntity target) || target == player) {
                continue;
            }

            suppressedChargePlayers.add(player.getUniqueId());
            DamageFlow.Result result;
            try {
                result = DamageFlow.damage(target, StormcleaverDefinition.SHOCKWAVE_DAMAGE, player);
            } finally {
                suppressedChargePlayers.remove(player.getUniqueId());
            }

            if (!result.accepted()
                    || !SchedulerCompat.isOwnedByCurrentRegion(target)
                    || target.isDead()
                    || !target.isValid()) {
                continue;
            }

            Vector push = target.getLocation().toVector().subtract(impact.toVector());
            if (push.lengthSquared() < 0.001D) {
                push = player.getLocation().getDirection().clone();
            }
            target.setVelocity(push.normalize().multiply(StormcleaverDefinition.SHOCKWAVE_KNOCKBACK)
                    .setY(StormcleaverDefinition.SHOCKWAVE_LIFT));
        }

        for (Location location : new Location[] {
                impact.clone().add(1.2D, 0.0D, 0.0D),
                impact.clone().add(-1.2D, 0.0D, 0.0D),
                impact.clone().add(0.0D, 0.0D, 1.2D),
                impact.clone().add(0.0D, 0.0D, -1.2D),
                impact.clone()
        }) {
            player.getWorld().strikeLightningEffect(location);
        }
    }

    private void showCharge(Player player, int charges, int requiredCharges) {
        double progress = (double) charges / requiredCharges;
        String message = lang.text("messages.stormcleaver.charge")
                .replace("{bar}", ActionBarHelper.buildProgressBar(progress))
                .replace("{charges}", String.valueOf(charges))
                .replace("{required}", String.valueOf(requiredCharges));
        ActionBarHelper.showActionBar(player, message);
    }

    private int getRequiredCharges() {
        return Math.max(1, Math.min(10, plugin.getConfig().getInt(CHARGES_CONFIG_PATH,
                StormcleaverDefinition.DEFAULT_CHARGES_REQUIRED)));
    }

    private double getJumpVelocityMultiplier() {
        return Math.max(0.1D, Math.min(5.0D, plugin.getConfig().getDouble(JUMP_MULTIPLIER_CONFIG_PATH,
                StormcleaverDefinition.DEFAULT_JUMP_VELOCITY_MULTIPLIER)));
    }

    private int getChargeDecayInterval() {
        return Math.max(1, Math.min(1200, plugin.getConfig().getInt(CHARGE_DECAY_INTERVAL_CONFIG_PATH,
                StormcleaverDefinition.DEFAULT_CHARGE_DECAY_INTERVAL)));
    }

    private boolean hasEmptyOffHand(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return offHand == null || offHand.getType().isAir();
    }

    private void denyInteraction(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void cleanupPlayer(UUID playerId) {
        stopLeap(playerId);
        fallDamageSuppression.remove(playerId);
        chargeManager.clearTransientState(playerId);
    }

    private void stopLeap(UUID playerId) {
        ScheduledTaskCompat task = leapTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        activeLeaps.remove(playerId);
    }

    private static final class LeapState {
        private final Player player;
        private int tick;

        private LeapState(Player player, int tick) {
            this.player = player;
            this.tick = tick;
        }

        private void setTick(int tick) {
            this.tick = tick;
        }

        private Player player() {
            return player;
        }

        private int tick() {
            return tick;
        }
    }
}
