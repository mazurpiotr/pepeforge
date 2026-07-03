package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.combat.CombatUtils;
import pepin.pepeforge.util.ui.ActionBarHelper;
import pepin.pepeforge.util.protection.ProtectionUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GreatswordListener implements Listener {

    private static final int COMBO_STAGE_MAX = 5;
    private static final int STATUS_INTERVAL_TICKS = 1;
    private static final int RHYTHM_BAR_SEGMENTS = 21;
    private static final int RHYTHM_CYCLE_TICKS = 30;
    private static final int RHYTHM_TARGET_TICK = 15;
    private static final int RHYTHM_INNER_START_TICK = 11;
    private static final int RHYTHM_INNER_END_TICK = 18;
    private static final int RHYTHM_GRACE_START_TICK = 9;
    private static final int RHYTHM_GRACE_END_TICK = 20;
    private static final int PENDING_SWING_DAMAGE_GRACE_TICKS = 1;
    private static final int NON_COMBAT_INTERACTION_GRACE_TICKS = 1;
    private static final double FIXED_REACH_BONUS = 0.45D;
    private static final double AREA_ATTACK_RANGE = 3.45D;
    private static final double AREA_BASE_ARC_DEGREES = 70.0D;
    private static final double AREA_STAGE_ARC_DEGREES = 14.0D;
    private static final int AREA_MAX_TARGETS = 6;
    private static final double AREA_DAMAGE_MULTIPLIER = 0.60D;
    private static final double AREA_KNOCKBACK_BASE = 0.25D;
    private static final double AREA_KNOCKBACK_PER_STAGE = 0.06D;
    private static final int COMBO_BREAK_FATIGUE_DURATION_TICKS = 40;
    private static final int COMBO_BREAK_FATIGUE_AMPLIFIER = 0;
    private static final double DEFLECT_DAMAGE_REDUCTION_PER_STAGE = 0.05D;
    private static final double DEFLECT_FRONT_DOT_THRESHOLD = 0.10D;
    private static final float RHYTHM_CUE_VOLUME = 0.55f;
    private static final float RHYTHM_CUE_PITCH = 1.7f;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final NamespacedKey reachModifierKey;
    private final Map<UUID, ComboState> comboStates = new ConcurrentHashMap<>();
    private final Map<UUID, PendingSwing> pendingSwings = new ConcurrentHashMap<>();
    private final Map<UUID, Long> resolvedHitTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nonCombatInteractionTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rhythmCueTicks = new ConcurrentHashMap<>();
    private final Set<UUID> rhythmBarShown = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cleavingPlayers = ConcurrentHashMap.newKeySet();
    private boolean transientModifierLogged = false;

    public GreatswordListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.reachModifierKey = new NamespacedKey(plugin, "greatsword_reach_bonus");
    }

    private ScheduledTaskCompat statusTask;

    public void startStatusTask() {
        statusTask = SchedulerCompat.runTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    GreatswordTier tier = itemFactory.getGreatswordTier(player.getInventory().getItemInMainHand());
                    if (tier == null) {
                        clearPlayerState(player);
                        return;
                    }

                    if (!CombatUtils.hasEmptyOffHand(player)) {
                        clearPlayerState(player);
                        ActionBarHelper.showActionBar(player, lang.text("messages.two_handed.offhand_required"));
                        return;
                    }

                    long currentTick = currentTick(player);
                    cleanupResolvedHitTick(player.getUniqueId(), currentTick);
                    cleanupNonCombatInteractionTick(player.getUniqueId(), currentTick);
                    expirePendingSwing(player, tier, currentTick);

                    ComboState state = comboStates.get(player.getUniqueId());
                    if (state != null && state.stage() > 0
                            && currentTick - state.lastSuccessTick() > RHYTHM_GRACE_END_TICK) {
                        breakCombo(player);
                        applyReachModifier(player, FIXED_REACH_BONUS);
                        return;
                    }

                    int currentStage = state == null ? 0 : state.stage();
                    applyReachModifier(player, FIXED_REACH_BONUS);
                    if (currentStage > 0) {
                        playRhythmCue(player, state, currentTick);
                        showRhythmActionBar(player, state, currentTick);
                    }
                });
            }
        }, 1L, STATUS_INTERVAL_TICKS);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        clearAllPlayerState();
    }

    public void clearAllPlayerState() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearPlayerState(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        GreatswordTier tier = itemFactory.getGreatswordTier(player.getInventory().getItemInMainHand());
        if (tier == null) {
            return;
        }

        if (!CombatUtils.hasEmptyOffHand(player)) {
            clearPlayerState(player);
            ActionBarHelper.showActionBar(player, lang.text("messages.two_handed.offhand_required"));
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTick = currentTick(player);
        if (wasHitResolvedRecently(playerId, currentTick)
                || wasNonCombatInteractionRecently(playerId, currentTick)) {
            return;
        }

        TimingResult timingResult = timingForSwing(player, currentTick);
        pendingSwings.put(playerId, new PendingSwing(currentTick, timingResult));

        if (!timingResult.canContinueCombo()) {
            breakCombo(player);
            applyReachModifier(player, FIXED_REACH_BONUS);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (cleavingPlayers.contains(player.getUniqueId())) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        GreatswordTier tier = itemFactory.getGreatswordTier(weapon);
        if (tier == null) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity primaryTarget) || primaryTarget == player) {
            return;
        }

        if (!CombatUtils.hasEmptyOffHand(player)) {
            clearPlayerState(player);
            ActionBarHelper.showActionBar(player, lang.text("messages.two_handed.offhand_required"));
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTick = currentTick(player);
        PendingSwing swing = consumePendingSwing(player, currentTick);
        resolvedHitTicks.put(playerId, currentTick);

        if (!swing.timingResult().canContinueCombo()) {
            breakCombo(player);
            applyReachModifier(player, FIXED_REACH_BONUS);
            return;
        }

        int currentStage = currentStage(playerId);
        List<LivingEntity> areaTargets = findAreaTargets(player, primaryTarget, currentStage);
        applyAreaAttack(player, areaTargets, event.getDamage() * AREA_DAMAGE_MULTIPLIER, currentStage, false);

        advanceCombo(player, currentStage, currentTick);
        applyReachModifier(player, FIXED_REACH_BONUS);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDefensiveDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (cleavingPlayers.contains(player.getUniqueId())) {
            return;
        }

        GreatswordTier tier = itemFactory.getGreatswordTier(player.getInventory().getItemInMainHand());
        if (tier == null || !CombatUtils.hasEmptyOffHand(player)) {
            return;
        }

        int stage = currentStage(player.getUniqueId());
        if (stage <= 0 || !isDamageSourceInFront(player, event.getDamager())) {
            return;
        }

        double reduction = Math.min(1.0D, stage * DEFLECT_DAMAGE_REDUCTION_PER_STAGE);
        event.setDamage(Math.max(0.0D, event.getDamage() * (1.0D - reduction)));
        playDeflectEffects(player, stage);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        markNonCombatInteractionIfGreatsword(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        markNonCombatInteractionIfGreatsword(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeld(PlayerItemHeldEvent event) {
        clearPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer());
    }

    private PendingSwing consumePendingSwing(Player player, long currentTick) {
        UUID playerId = player.getUniqueId();
        PendingSwing swing = pendingSwings.remove(playerId);
        if (swing == null) {
            return new PendingSwing(currentTick, timingForSwing(player, currentTick));
        }

        if (currentTick - swing.tick() > PENDING_SWING_DAMAGE_GRACE_TICKS) {
            return new PendingSwing(currentTick, TimingResult.LATE);
        }
        return swing;
    }

    private void expirePendingSwing(Player player, GreatswordTier tier, long currentTick) {
        UUID playerId = player.getUniqueId();
        PendingSwing swing = pendingSwings.get(playerId);
        if (swing == null || currentTick - swing.tick() <= PENDING_SWING_DAMAGE_GRACE_TICKS) {
            return;
        }

        pendingSwings.remove(playerId);
        ComboState state = comboStates.get(playerId);
        if (swing.timingResult().canContinueCombo()) {
            int currentStage = state == null ? 0 : state.stage();
            List<LivingEntity> areaTargets = findAreaTargets(player, null, currentStage);
            applyAreaAttack(
                    player,
                    areaTargets,
                    tier.attackDamage() * AREA_DAMAGE_MULTIPLIER,
                    currentStage,
                    true);
            advanceCombo(player, currentStage, swing.tick());
            applyReachModifier(player, FIXED_REACH_BONUS);
            return;
        }

        if (state != null || swing.timingResult() != TimingResult.OPENER) {
            breakCombo(player);
        }
    }

    private void cleanupResolvedHitTick(UUID playerId, long currentTick) {
        Long resolvedTick = resolvedHitTicks.get(playerId);
        if (resolvedTick != null && currentTick - resolvedTick > PENDING_SWING_DAMAGE_GRACE_TICKS) {
            resolvedHitTicks.remove(playerId);
        }
    }

    private boolean wasHitResolvedRecently(UUID playerId, long currentTick) {
        Long resolvedTick = resolvedHitTicks.get(playerId);
        return resolvedTick != null && currentTick - resolvedTick <= PENDING_SWING_DAMAGE_GRACE_TICKS;
    }

    private void cleanupNonCombatInteractionTick(UUID playerId, long currentTick) {
        Long interactionTick = nonCombatInteractionTicks.get(playerId);
        if (interactionTick != null && currentTick - interactionTick > NON_COMBAT_INTERACTION_GRACE_TICKS) {
            nonCombatInteractionTicks.remove(playerId);
        }
    }

    private boolean wasNonCombatInteractionRecently(UUID playerId, long currentTick) {
        Long interactionTick = nonCombatInteractionTicks.get(playerId);
        return interactionTick != null
                && currentTick - interactionTick <= NON_COMBAT_INTERACTION_GRACE_TICKS;
    }

    private void markNonCombatInteractionIfGreatsword(Player player) {
        if (itemFactory.getGreatswordTier(player.getInventory().getItemInMainHand()) == null) {
            return;
        }
        nonCombatInteractionTicks.put(player.getUniqueId(), currentTick(player));
    }

    private TimingResult timingForSwing(Player player, long currentTick) {
        ComboState state = comboStates.get(player.getUniqueId());
        if (state == null || state.stage() <= 0) {
            return TimingResult.OPENER;
        }

        long elapsed = currentTick - state.lastSuccessTick();
        if (elapsed < RHYTHM_GRACE_START_TICK) {
            return TimingResult.EARLY;
        }
        if (elapsed > RHYTHM_GRACE_END_TICK) {
            return TimingResult.LATE;
        }
        if (elapsed >= RHYTHM_INNER_START_TICK && elapsed <= RHYTHM_INNER_END_TICK) {
            return TimingResult.INNER_SUCCESS;
        }
        return TimingResult.GRACE_SUCCESS;
    }

    private int currentStage(UUID playerId) {
        ComboState state = comboStates.get(playerId);
        return state == null ? 0 : state.stage();
    }

    private void advanceCombo(Player player, int currentStage, long currentTick) {
        int nextStage = Math.min(COMBO_STAGE_MAX, currentStage + 1);
        comboStates.put(player.getUniqueId(), new ComboState(nextStage, currentTick));
    }

    private List<LivingEntity> findAreaTargets(
            Player player,
            LivingEntity excludedTarget,
            int stage) {
        Location eyeLocation = player.getEyeLocation();
        Vector facing = horizontalDirection(eyeLocation.getDirection());
        double range = AREA_ATTACK_RANGE;
        double maxDistanceSquared = range * range;
        double arcDegrees = AREA_BASE_ARC_DEGREES + AREA_STAGE_ARC_DEGREES * stage;
        double frontDotThreshold = Math.cos(Math.toRadians(arcDegrees / 2.0D));
        List<AreaCandidate> candidates = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity livingTarget)
                    || livingTarget == player
                    || livingTarget == excludedTarget
                    || livingTarget.isDead()
                    || !livingTarget.isValid()) {
                continue;
            }

            Vector toTarget = livingTarget.getEyeLocation().toVector().subtract(eyeLocation.toVector());
            double distanceSquared = toTarget.lengthSquared();
            if (distanceSquared > maxDistanceSquared || distanceSquared < 0.001D) {
                continue;
            }

            Vector horizontalToTarget = horizontalDirection(toTarget);
            if (facing.dot(horizontalToTarget) < frontDotThreshold) {
                continue;
            }

            candidates.add(new AreaCandidate(livingTarget, distanceSquared));
        }

        return candidates.stream()
            .sorted(Comparator.comparingDouble(candidate -> candidate.distanceSquared()))
            .limit(AREA_MAX_TARGETS)
            .map(candidate -> candidate.target())
            .toList();
    }

    private void applyAreaAttack(
            Player player,
            List<LivingEntity> targets,
            double damage,
            int stage,
            boolean playSwingEffects) {
        if (targets.isEmpty() && !playSwingEffects) {
            return;
        }

        double areaDamage = Math.max(0.0D, damage);
        double knockbackStrength = AREA_KNOCKBACK_BASE + AREA_KNOCKBACK_PER_STAGE * stage;

        cleavingPlayers.add(player.getUniqueId());
        try {
            for (LivingEntity target : targets) {
                if (!ProtectionUtil.canDamage(player, target)) {
                    continue;
                }
                target.setNoDamageTicks(0);
                target.damage(areaDamage, player);
                applyKnockback(player, target, knockbackStrength);
                playTargetAreaEffects(target.getWorld(), target.getLocation().add(0.0D, 1.0D, 0.0D));
            }
        } finally {
            cleavingPlayers.remove(player.getUniqueId());
        }

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.85f + 0.08f * stage);
        world.spawnParticle(
                Particle.SWEEP_ATTACK,
                player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.5D)),
                1,
                0.25D,
                0.12D,
                0.25D,
                0.0D);
    }

    private void applyKnockback(Player player, LivingEntity target, double strength) {
        Vector push = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0.0D);
        if (push.lengthSquared() < 0.001D) {
            push = horizontalDirection(player.getEyeLocation().getDirection());
        } else {
            push.normalize();
        }
        push.multiply(strength);

        Vector velocity = target.getVelocity().add(push);
        velocity.setY(Math.max(velocity.getY(), 0.12D));
        target.setVelocity(velocity);
    }

    private void playTargetAreaEffects(World world, Location location) {
        world.spawnParticle(Particle.SWEEP_ATTACK, location, 1, 0.08D, 0.08D, 0.08D, 0.0D);
        world.spawnParticle(Particle.CRIT, location, 4, 0.18D, 0.12D, 0.18D, 0.02D);
    }

    private boolean isDamageSourceInFront(Player player, Entity damager) {
        if (damager instanceof Projectile projectile) {
            return CombatUtils.isInFront(player, projectile.getLocation().toVector(), DEFLECT_FRONT_DOT_THRESHOLD);
        }
        return CombatUtils.isInFront(player, damager.getLocation().toVector(), DEFLECT_FRONT_DOT_THRESHOLD);
    }

    private void playDeflectEffects(Player player, int stage) {
        World world = player.getWorld();
        Location location = player.getLocation().add(0.0D, 1.0D, 0.0D);
        world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.45f, 1.15f + 0.05f * stage);
        world.spawnParticle(
                Particle.CRIT,
                location,
                2 + stage,
                0.18D,
                0.20D,
                0.18D,
                0.01D);
    }

    private void applyReachModifier(Player player, double amount) {
        AttributeInstance attribute = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attribute == null) {
            return;
        }

        for (AttributeModifier mod : attribute.getModifiers()) {
            if (reachModifierKey.equals(mod.getKey())) {
                if (Double.compare(mod.getAmount(), amount) == 0) {
                    return;
                } else {
                    attribute.removeModifier(mod);
                }
            }
        }

        addReachModifier(attribute, new AttributeModifier(
                reachModifierKey,
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY));
    }

    private void addReachModifier(AttributeInstance attribute, AttributeModifier modifier) {
        try {
            Method method = attribute.getClass().getMethod("addTransientModifier", AttributeModifier.class);
            method.invoke(attribute, modifier);
        } catch (ReflectiveOperationException | SecurityException e) {
            if (!transientModifierLogged) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to use addTransientModifier for Greatsword reach (likely not on Paper). Falling back to standard addModifier. This warning is printed only once.",
                        e);
                transientModifierLogged = true;
            }
            attribute.addModifier(modifier);
        }
    }

    private void removeReachModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attribute == null) {
            return;
        }
        removeReachModifier(attribute);
    }

    private void removeReachModifier(AttributeInstance attribute) {
        List<AttributeModifier> modifiers = new ArrayList<>(attribute.getModifiers());
        for (AttributeModifier modifier : modifiers) {
            if (reachModifierKey.equals(modifier.getKey())) {
                attribute.removeModifier(modifier);
            }
        }
    }

    private void resetCombo(Player player) {
        UUID playerId = player.getUniqueId();
        comboStates.remove(playerId);
        pendingSwings.remove(playerId);
        rhythmCueTicks.remove(playerId);
        clearRhythmActionBar(player);
    }

    private void breakCombo(Player player) {
        boolean hadCombo = currentStage(player.getUniqueId()) > 0;
        resetCombo(player);
        if (hadCombo) {
            applyComboBreakFatigue(player);
        }
    }

    private void applyComboBreakFatigue(Player player) {
        PotionEffect penalty = new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                COMBO_BREAK_FATIGUE_DURATION_TICKS,
                COMBO_BREAK_FATIGUE_AMPLIFIER,
                true,
                false,
                true);
        PotionEffect current = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (current != null
                && current.getAmplifier() >= penalty.getAmplifier()
                && current.getDuration() >= penalty.getDuration()) {
            return;
        }
        player.addPotionEffect(penalty);
    }

    private void clearPlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        comboStates.remove(playerId);
        pendingSwings.remove(playerId);
        resolvedHitTicks.remove(playerId);
        nonCombatInteractionTicks.remove(playerId);
        rhythmCueTicks.remove(playerId);
        removeReachModifier(player);
        clearRhythmActionBar(player);
    }

    private long currentTick(Player player) {
        return player.getWorld().getGameTime();
    }

    private Vector horizontalDirection(Vector direction) {
        Vector horizontal = direction.clone().setY(0.0D);
        if (horizontal.lengthSquared() < 0.001D) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return horizontal.normalize();
    }

    private void showRhythmActionBar(Player player, ComboState state, long currentTick) {
        String message = lang.text("messages.greatsword.rhythm")
                .replace("{bar}", buildRhythmBar(state, currentTick))
                .replace("{stage}", String.valueOf(state.stage()))
                .replace("{max_stage}", String.valueOf(COMBO_STAGE_MAX));
        rhythmBarShown.add(player.getUniqueId());
        ActionBarHelper.showActionBar(player, message);
    }

    private void playRhythmCue(Player player, ComboState state, long currentTick) {
        if (currentTick - state.lastSuccessTick() != RHYTHM_TARGET_TICK) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Long playedForTick = rhythmCueTicks.get(playerId);
        if (playedForTick != null && playedForTick == state.lastSuccessTick()) {
            return;
        }

        rhythmCueTicks.put(playerId, state.lastSuccessTick());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, RHYTHM_CUE_VOLUME, RHYTHM_CUE_PITCH);
    }

    private void clearRhythmActionBar(Player player) {
        if (rhythmBarShown.remove(player.getUniqueId())) {
            ActionBarHelper.showActionBar(player, "");
        }
    }

    private String buildRhythmBar(ComboState state, long currentTick) {
        long elapsed = Math.max(0L, Math.min(RHYTHM_CYCLE_TICKS, currentTick - state.lastSuccessTick()));
        int pointerIndex = rhythmIndex(elapsed);
        int targetIndex = rhythmIndex(RHYTHM_TARGET_TICK);
        int innerStartIndex = rhythmIndex(RHYTHM_INNER_START_TICK);
        int innerEndIndex = rhythmIndex(RHYTHM_INNER_END_TICK);
        int graceStartIndex = rhythmIndex(RHYTHM_GRACE_START_TICK);
        int graceEndIndex = rhythmIndex(RHYTHM_GRACE_END_TICK);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < RHYTHM_BAR_SEGMENTS; i++) {
            String zoneColor = rhythmZoneColor(i, innerStartIndex, innerEndIndex, graceStartIndex, graceEndIndex);
            char marker = rhythmMarker(i, pointerIndex, targetIndex);
            if (i == pointerIndex) {
                bar.append("&f").append(marker);
            } else {
                bar.append(zoneColor).append(marker);
            }
        }
        return bar.toString();
    }

    private String rhythmZoneColor(
            int index,
            int innerStartIndex,
            int innerEndIndex,
            int graceStartIndex,
            int graceEndIndex) {
        if (index >= innerStartIndex && index <= innerEndIndex) {
            return "&a";
        }
        if (index >= graceStartIndex && index <= graceEndIndex) {
            return "&e";
        }
        return "&7";
    }

    private char rhythmMarker(int index, int pointerIndex, int targetIndex) {
        if (index == pointerIndex && index == targetIndex) {
            return '*';
        }
        if (index == pointerIndex) {
            return '|';
        }
        if (index == targetIndex) {
            return 'X';
        }
        return '-';
    }

    private int rhythmIndex(long tick) {
        int maxIndex = RHYTHM_BAR_SEGMENTS - 1;
        int index = (int) Math.round((double) tick / RHYTHM_CYCLE_TICKS * maxIndex);
        return Math.max(0, Math.min(maxIndex, index));
    }

    private record ComboState(int stage, long lastSuccessTick) {
    }

    private record PendingSwing(long tick, TimingResult timingResult) {
    }

    private enum TimingResult {
        OPENER(true),
        INNER_SUCCESS(true),
        GRACE_SUCCESS(true),
        EARLY(false),
        LATE(false);

        private final boolean canContinueCombo;

        TimingResult(boolean canContinueCombo) {
            this.canContinueCombo = canContinueCombo;
        }

        private boolean canContinueCombo() {
            return canContinueCombo;
        }
    }

    private record AreaCandidate(LivingEntity target, double distanceSquared) {
    }
}
