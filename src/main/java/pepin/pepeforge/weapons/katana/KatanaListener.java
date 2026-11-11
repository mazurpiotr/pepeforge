package pepin.pepeforge.weapons.katana;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.combat.CombatUtils;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TwoHandedEquipmentHandler;
import pepin.pepeforge.util.ui.ActionBarHelper;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KatanaListener implements Listener, TwoHandedEquipmentHandler {
    private static final String PARRY_COOLDOWN_KEY = "katana:parry";

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final CooldownManager cooldownManager;
    private final NamespacedKey reflectUntilKey;
    private final NamespacedKey parrySessionKey;
    private final Map<UUID, ParrySession> activeParries = new ConcurrentHashMap<>();
    private final Map<UUID, CooldownTask> cooldownTasks = new ConcurrentHashMap<>();
    private final Set<UUID> invalidEquipment = ConcurrentHashMap.newKeySet();
    private final Object taskLock = new Object();
    private volatile boolean running;
    private volatile long lifecycleGeneration;

    public KatanaListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang,
            CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.cooldownManager = cooldownManager;
        this.reflectUntilKey = new NamespacedKey(plugin, "katana_reflect_until");
        this.parrySessionKey = new NamespacedKey(plugin, "katana_parry_session");
    }

    public void start() {
        synchronized (taskLock) {
            if (!running) {
                lifecycleGeneration++;
                running = true;
            }
        }
    }

    public void stop() {
        synchronized (taskLock) {
            running = false;
            lifecycleGeneration++;
            cooldownTasks.values().forEach(CooldownTask::cancel);
            cooldownTasks.clear();
            for (ParrySession session : activeParries.values()) {
                session.cancel();
                Runnable reset = () -> resetParryVisual(session);
                if (SchedulerCompat.isOwnedByCurrentRegion(session.player)) {
                    reset.run();
                } else {
                    SchedulerCompat.runForPlayer(session.player, plugin, reset);
                }
            }
            activeParries.clear();
            invalidEquipment.clear();
        }
    }

    @Override
    public void refreshEquipment(Player player, boolean notify) {
        if (!running || !player.isOnline()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        ParrySession session = activeParries.get(playerId);
        if (session != null && !holdsParryingKatana(session)) {
            clearActiveParry(session);
            session = null;
        }
        if (!itemFactory.isKatana(player.getInventory().getItemInMainHand()) || player.isDead()) {
            invalidEquipment.remove(playerId);
            stopCooldown(playerId);
            return;
        }
        if (session == null || !isParryActive(player, System.currentTimeMillis())) {
            boolean invalid = !CombatUtils.hasEmptyOffHand(player);
            if (invalid) {
                if (invalidEquipment.add(playerId) && notify) {
                    showOffhandWarning(player);
                }
            } else {
                invalidEquipment.remove(playerId);
            }
        }
        ensureCooldownTask(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!itemFactory.isKatana(mainHandItem)) {
            return;
        }

        if (!running) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean attemptedParry = event.getHand() != EquipmentSlot.OFF_HAND
                && isAttemptedParryInput(player, action, event.getClickedBlock());
        if (isParryActive(player, now)) {
            if (attemptedParry || event.getHand() == EquipmentSlot.OFF_HAND) {
                denyInteraction(event);
            }
            return;
        }
        if (!CombatUtils.hasEmptyOffHand(player)) {
            if (attemptedParry) {
                invalidEquipment.add(player.getUniqueId());
                showOffhandWarning(player);
            }
            return;
        }
        invalidEquipment.remove(player.getUniqueId());
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            denyInteraction(event);
            return;
        }
        if (!attemptedParry) {
            return;
        }

        long remainingMillis = cooldownManager.getRemainingCooldownMillis(player, PARRY_COOLDOWN_KEY);
        if (remainingMillis > 0L) {
            showCooldownActionBar(player, remainingMillis);
            denyInteraction(event);
            return;
        }

        activateParry(player, mainHandItem, now);
        denyInteraction(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!SchedulerCompat.isOwnedByCurrentRegion(player)
                || !SchedulerCompat.isOwnedByCurrentRegion(event.getDamager())) {
            return;
        }

        long now = System.currentTimeMillis();
        ParrySession session = getActiveParry(player, now);
        if (!running || session == null) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile) {
            if (!isProjectileReflectable(player, projectile, now)) {
                return;
            }
            event.setCancelled(true);
            reflectProjectile(player, projectile, now);
            registerProjectileReflection(session, player.getWorld().getGameTime());
            playReflectEffects(player.getWorld(), player.getEyeLocation());
            return;
        }

        if (!(damager instanceof LivingEntity attacker)) {
            return;
        }

        if (!CombatUtils.isInFront(player, attacker.getEyeLocation().toVector(),
                KatanaDefinition.PARRY_FRONT_DOT_THRESHOLD)) {
            return;
        }

        event.setCancelled(true);
        World parryWorld = player.getWorld();
        Location parryLocation = player.getLocation();
        if (!DamageFlow.isCounterattack(event)
                && DamageFlow.counterattack(attacker, KatanaDefinition.COUNTERATTACK_DAMAGE, player).accepted()
                && SchedulerCompat.isOwnedByCurrentRegion(player) && SchedulerCompat.isOwnedByCurrentRegion(attacker)
                && player.isOnline() && !player.isDead() && attacker.isValid() && !attacker.isDead()
                && player.getWorld() == attacker.getWorld()) {
            knockBackAttacker(player, attacker);
        }
        playMeleeParryEffects(parryWorld, parryLocation);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer());
        cooldownManager.clearCooldown(event.getPlayer(), PARRY_COOLDOWN_KEY);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        ParrySession session = activeParries.get(event.getEntity().getUniqueId());
        if (session != null) {
            event.getDrops().forEach(item -> resetMatchingStack(item, session));
        }
        clearPlayer(event.getEntity());
    }

    private void clearPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        ParrySession session = activeParries.get(playerId);
        if (session != null) {
            clearActiveParry(session);
        }
        stopCooldown(playerId);
        invalidEquipment.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item droppedEntity = event.getItemDrop();
        ItemStack droppedItem = droppedEntity.getItemStack();
        if (!itemFactory.isKatana(droppedItem)) {
            return;
        }

        ParrySession session = activeParries.get(event.getPlayer().getUniqueId());
        boolean activeStackDropped = session != null && matchesSession(droppedItem, session);
        itemFactory.setKatanaVisual(droppedItem, KatanaDefinition.VisualState.NORMAL);
        if (droppedItem.hasItemMeta()) {
            var meta = droppedItem.getItemMeta();
            meta.getPersistentDataContainer().remove(parrySessionKey);
            droppedItem.setItemMeta(meta);
        }
        droppedEntity.setItemStack(droppedItem);
        if (activeStackDropped) {
            clearActiveParry(session);
        }
    }

    private void activateParry(Player player, ItemStack katana, long now) {
        long durationMillis = KatanaDefinition.PARRY_DURATION_TICKS * 50L;
        long cooldownMillis = KatanaDefinition.COOLDOWN_TICKS * 50L;
        long activeUntil = now + durationMillis;
        long maxUntil = now + KatanaDefinition.PARRY_MAX_DURATION_TICKS * 50L;

        cooldownManager.setCooldown(player, PARRY_COOLDOWN_KEY, cooldownMillis);
        ParrySession session = new ParrySession(player, katana, activeUntil, maxUntil, lifecycleGeneration);
        itemFactory.setKatanaVisual(katana, session.visualState);
        synchronized (taskLock) {
            if (!running || session.generation != lifecycleGeneration) {
                resetMatchingStack(katana, session);
                return;
            }
            ParrySession previous = activeParries.put(player.getUniqueId(), session);
            if (previous != null) {
                previous.cancel();
                resetParryVisual(previous);
            }
            session.task = SchedulerCompat.runTimerForEntity(player, plugin, session,
                    session::retire, 0L, 1L);
            if (session.cancelled) {
                session.cancel();
            }
        }
        ensureCooldownTask(player);

        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        world.spawnParticle(Particle.SWEEP_ATTACK, eyeLocation, 1, 0.2, 0.2, 0.2, 0.0);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.2f);
        world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.75f, 1.4f);
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.35f, 1.7f);
    }

    private void reflectNearbyProjectiles(ParrySession session, long now) {
        Player player = session.player;
        Location eyeLocation = player.getEyeLocation();
        double radius = KatanaDefinition.PROJECTILE_SCAN_RADIUS;
        long worldTick = player.getWorld().getGameTime();
        for (Entity entity : player.getWorld().getNearbyEntities(eyeLocation, radius, radius, radius)) {
            if (!(entity instanceof Projectile projectile)) {
                continue;
            }
            if (!isProjectileReflectable(player, projectile, now)) {
                continue;
            }
            reflectProjectile(player, projectile, now);
            registerProjectileReflection(session, worldTick);
            playReflectEffects(player.getWorld(), projectile.getLocation());
        }
    }

    private void registerProjectileReflection(ParrySession session, long worldTick) {
        if (session.lastReflectionTick != worldTick) {
            session.lastReflectionTick = worldTick;
            session.until = Math.min(session.until
                    + KatanaDefinition.PARRY_EXTENSION_TICKS * 50L, session.maxUntil);
        }
        session.visualState = session.visualState == KatanaDefinition.VisualState.PARRY_A
                ? KatanaDefinition.VisualState.PARRY_B
                : KatanaDefinition.VisualState.PARRY_A;
        itemFactory.setKatanaVisual(session.katana, session.visualState);
    }

    private boolean isProjectileReflectable(Player player, Projectile projectile, long now) {
        if (!SchedulerCompat.isOwnedByCurrentRegion(projectile)) {
            return false;
        }
        if (!projectile.isValid() || projectile.isDead()) {
            return false;
        }
        if (projectile.getShooter() == player) {
            return false;
        }

        Long reflectUntil = projectile.getPersistentDataContainer().get(reflectUntilKey, PersistentDataType.LONG);
        if (reflectUntil != null && reflectUntil > now) {
            return false;
        }

        Vector velocity = projectile.getVelocity();
        if (velocity.lengthSquared() < 0.01D) {
            return false;
        }

        if (!CombatUtils.isInFront(player, projectile.getLocation().toVector(),
                KatanaDefinition.PARRY_FRONT_DOT_THRESHOLD)) {
            return false;
        }

        Vector toPlayer = player.getEyeLocation().toVector().subtract(projectile.getLocation().toVector());
        if (toPlayer.lengthSquared() < 0.001D) {
            return true;
        }

        return velocity.normalize()
                .dot(toPlayer.normalize()) >= KatanaDefinition.PROJECTILE_TOWARD_PLAYER_DOT_THRESHOLD;
    }

    private void reflectProjectile(Player player, Projectile projectile, long now) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double speed = Math.max(projectile.getVelocity().length(), KatanaDefinition.REFLECT_MIN_SPEED);
        projectile.getPersistentDataContainer().set(reflectUntilKey, PersistentDataType.LONG, now + 250L);
        projectile.setShooter(player);
        SchedulerCompat.teleport(projectile, player.getEyeLocation().add(direction.clone().multiply(0.9D)));
        projectile.setVelocity(direction.multiply(speed));

        if (projectile instanceof AbstractArrow arrow) {
            arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }
    }

    private ParrySession getActiveParry(Player player, long now) {
        ParrySession session = activeParries.get(player.getUniqueId());
        if (session == null || session.cancelled || session.until <= now
                || session.generation != lifecycleGeneration || !holdsParryingKatana(session)) {
            return null;
        }
        return session;
    }

    private boolean isParryActive(Player player, long now) {
        return getActiveParry(player, now) != null;
    }

    private boolean isAttemptedParryInput(Player player, Action action, Block clickedBlock) {
        if (action == Action.RIGHT_CLICK_AIR) {
            return true;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }
        return player.isSneaking()
                || clickedBlock == null
                || !clickedBlock.getType().isInteractable();
    }

    private boolean holdsParryingKatana(ParrySession session) {
        PlayerInventory inventory = session.player.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        return inventory.getHeldItemSlot() == session.slot
                && itemFactory.isKatana(held) && matchesSession(held, session);
    }

    private void clearActiveParry(ParrySession session) {
        if (activeParries.remove(session.playerId, session)) {
            session.cancel();
            resetParryVisual(session);
        }
    }

    private void resetParryVisual(ParrySession session) {
        resetMatchingStack(session.katana, session);
        PlayerInventory inventory = session.player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (resetMatchingStack(item, session)) {
                inventory.setItem(slot, item);
            }
        }
        ItemStack cursor = session.player.getItemOnCursor();
        if (resetMatchingStack(cursor, session)) {
            session.player.setItemOnCursor(cursor);
        }
        var view = session.player.getOpenInventory();
        if (view != null) {
            var top = view.getTopInventory();
            for (int slot = 0; slot < top.getSize(); slot++) {
                ItemStack item = top.getItem(slot);
                if (resetMatchingStack(item, session)) {
                    top.setItem(slot, item);
                }
            }
        }
    }

    private boolean matchesSession(ItemStack item, ParrySession session) {
        return item != null && item.hasItemMeta()
                && session.token.equals(item.getItemMeta().getPersistentDataContainer()
                        .get(parrySessionKey, PersistentDataType.STRING));
    }

    private boolean resetMatchingStack(ItemStack item, ParrySession session) {
        if (!matchesSession(item, session)) {
            return false;
        }
        itemFactory.setKatanaVisual(item, KatanaDefinition.VisualState.NORMAL);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(parrySessionKey);
        item.setItemMeta(meta);
        return true;
    }

    private void ensureCooldownTask(Player player) {
        synchronized (taskLock) {
            if (!running || cooldownTasks.containsKey(player.getUniqueId())
                    || cooldownManager.getRemainingCooldownMillis(player, PARRY_COOLDOWN_KEY) <= 0L) {
                return;
            }
            CooldownTask task = new CooldownTask(player, lifecycleGeneration);
            cooldownTasks.put(task.playerId, task);
            task.task = SchedulerCompat.runTimerForEntity(player, plugin, task, task::retire, 1L, 2L);
            if (task.cancelled) {
                task.cancel();
            }
        }
    }

    private void stopCooldown(UUID playerId) {
        CooldownTask task = cooldownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void showOffhandWarning(Player player) {
        ActionBarHelper.showActionBar(player, lang.text("messages.two_handed.offhand_required"));
    }

    private final class ParrySession implements Runnable {
        private final Player player;
        private final UUID playerId;
        private final ItemStack katana;
        private final String token = UUID.randomUUID().toString();
        private final int slot;
        private volatile long until;
        private final long maxUntil;
        private final long generation;
        private volatile long lastReflectionTick = Long.MIN_VALUE;
        private volatile KatanaDefinition.VisualState visualState = KatanaDefinition.VisualState.PARRY_A;
        private volatile ScheduledTaskCompat task;
        private volatile boolean cancelled;

        private ParrySession(Player player, ItemStack katana, long until, long maxUntil, long generation) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.katana = katana;
            var meta = katana.getItemMeta();
            meta.getPersistentDataContainer().set(parrySessionKey, PersistentDataType.STRING, token);
            katana.setItemMeta(meta);
            this.slot = player.getInventory().getHeldItemSlot();
            this.until = until;
            this.maxUntil = maxUntil;
            this.generation = generation;
        }

        @Override
        public void run() {
            if (cancelled || !running || generation != lifecycleGeneration
                    || activeParries.get(playerId) != this) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now >= until || !player.isOnline() || player.isDead() || !holdsParryingKatana(this)) {
                clearActiveParry(this);
                return;
            }
            reflectNearbyProjectiles(this, now);
        }

        private void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
            }
        }

        private void retire() {
            activeParries.remove(playerId, this);
            cancel();
        }
    }

    private final class CooldownTask implements Runnable {
        private final Player player;
        private final UUID playerId;
        private final long generation;
        private volatile ScheduledTaskCompat task;
        private volatile boolean cancelled;

        private CooldownTask(Player player, long generation) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.generation = generation;
        }

        @Override
        public void run() {
            if (cancelled || !running || generation != lifecycleGeneration
                    || cooldownTasks.get(playerId) != this) {
                return;
            }
            long remaining = cooldownManager.getRemainingCooldownMillis(player, PARRY_COOLDOWN_KEY);
            if (!player.isOnline() || player.isDead()
                    || !itemFactory.isKatana(player.getInventory().getItemInMainHand()) || remaining <= 0L) {
                retire();
                return;
            }
            showCooldownActionBar(player, remaining);
        }

        private void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
            }
        }

        private void retire() {
            cooldownTasks.remove(playerId, this);
            cancel();
        }
    }

    private void denyInteraction(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void knockBackAttacker(Player defender, LivingEntity attacker) {
        Vector push = attacker.getLocation().toVector().subtract(defender.getLocation().toVector());
        if (push.lengthSquared() < 0.001D) {
            push = defender.getEyeLocation().getDirection().multiply(-1.0D);
        }
        push.normalize().multiply(KatanaDefinition.MELEE_KNOCKBACK_STRENGTH).setY(0.22D);
        attacker.setVelocity(push);
    }

    private void playReflectEffects(World world, Location location) {
        world.playSound(location, Sound.ITEM_SHIELD_BLOCK, 0.95f, 1.55f);
        world.playSound(location, Sound.BLOCK_ANVIL_LAND, 0.4f, 1.75f);
        world.spawnParticle(Particle.ELECTRIC_SPARK, location, 10, 0.12, 0.12, 0.12, 0.01);
    }

    private void playMeleeParryEffects(World world, Location location) {
        world.playSound(location, Sound.ITEM_SHIELD_BLOCK, 0.95f, 1.35f);
        world.playSound(location, Sound.BLOCK_ANVIL_LAND, 0.45f, 1.6f);
        world.spawnParticle(Particle.SWEEP_ATTACK, location.add(0.0, 1.0, 0.0), 1, 0.1, 0.1, 0.1, 0.0);
    }

    private void showCooldownActionBar(Player player, long remainingMillis) {
        double seconds = remainingMillis / 1000.0D;
        long cooldownMillis = KatanaDefinition.COOLDOWN_TICKS * 50L;
        double progress = Math.max(0.0D, Math.min(1.0D, 1.0D - ((double) remainingMillis / cooldownMillis)));
        String bar = ActionBarHelper.buildProgressBar(progress);
        String message = lang.text("messages.katana.cooldown")
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", seconds));
        ActionBarHelper.showActionBar(player, message);
    }

}
