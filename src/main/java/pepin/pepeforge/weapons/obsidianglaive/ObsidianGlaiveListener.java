package pepin.pepeforge.weapons.obsidianglaive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TwoHandedEquipmentHandler;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.weapons.obsidianglaive.compat.GlaiveCombatAdapter;

public final class ObsidianGlaiveListener implements Listener, TwoHandedEquipmentHandler {
    private final JavaPlugin plugin;
    private final ItemFactory factory;
    private final ObsidianGlaiveSettings settings;
    private final GlaiveCombatAdapter combat;
    private final Object taskLock = new Object();
    private final Map<UUID, Equipment> equipment = new ConcurrentHashMap<>();
    private final Map<EntityDamageByEntityEvent, Attempt> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, InventoryReset> inventoryResets = new ConcurrentHashMap<>();
    private final ThreadLocal<SecondaryHit> secondaryHit = new ThreadLocal<>();
    private volatile boolean running;
    private volatile long generation;

    public ObsidianGlaiveListener(JavaPlugin plugin, ItemFactory factory, ObsidianGlaiveSettings settings,
            GlaiveCombatAdapter combat) {
        this.plugin = plugin;
        this.factory = factory;
        this.settings = settings;
        this.combat = combat;
    }

    public void start() {
        synchronized (taskLock) {
            generation++;
            running = true;
        }
    }

    public void stop() {
        Set<Player> players = new HashSet<>();
        Map<Player, List<InventoryAccess>> transferredItems = new ConcurrentHashMap<>();
        synchronized (taskLock) {
            running = false;
            generation++;
            equipment.values().forEach(state -> {
                players.add(state.player);
                state.cancelExpiry();
            });
            inventoryResets.values().forEach(reset -> {
                players.add(reset.player);
                transferredItems.put(reset.player, List.copyOf(reset.inventories));
                reset.cancel();
            });
            inventoryResets.clear();
            equipment.clear();
            attempts.clear();
        }
        players.addAll(plugin.getServer().getOnlinePlayers());
        for (Player player : players) {
            Runnable cleanup = () -> {
                restoreInventory(player, true);
                for (InventoryAccess access : transferredItems.getOrDefault(player, List.of())) {
                    cleanupTransferred(access);
                }
            };
            if (SchedulerCompat.isOwnedByCurrentRegion(player)) {
                cleanup.run();
            } else if (plugin.isEnabled()) {
                SchedulerCompat.runForPlayer(player, plugin, cleanup);
            }
        }
    }

    @Override
    public void refreshEquipment(Player player, boolean notify) {
        if (!running || !SchedulerCompat.isOwnedByCurrentRegion(player) || !player.isOnline()) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        UUID playerId = player.getUniqueId();
        if (inventoryResets.containsKey(playerId)) {
            return;
        }
        Equipment current = equipment.get(playerId);
        if (!factory.isObsidianGlaive(held) || player.isDead()) {
            if (current != null) {
                reset(player);
            }
            return;
        }
        boolean refreshed = factory.refreshObsidianGlaive(held);
        String identity = factory.getObsidianGlaiveIdentity(held);
        int slot = player.getInventory().getHeldItemSlot();
        if (!refreshed && current != null && current.slot == slot && current.world == player.getWorld()
                && current.identity.equals(identity) && current.stage == factory.getObsidianGlaiveStage(held)) {
            return;
        }
        reset(player);
        held = player.getInventory().getItemInMainHand();
        if (running && factory.isObsidianGlaive(held)) {
            factory.beginObsidianGlaiveCombo(held);
            player.getInventory().setItemInMainHand(held);
            equipment.put(playerId, new Equipment(player, slot, factory.getObsidianGlaiveIdentity(held),
                    player.getWorld()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!running || event.isCancelled() || DamageFlow.isSecondaryDamage(event)
                || !(event.getDamager() instanceof Player player)
                || !SchedulerCompat.isOwnedByCurrentRegion(player)
                || !factory.isObsidianGlaive(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
        }
        if (inventoryResets.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            combat.clear(player);
            return;
        }
        if (event.getCause() != DamageCause.ENTITY_ATTACK || !(event.getEntity() instanceof LivingEntity target)
                || target == player || !SchedulerCompat.isOwnedByCurrentRegion(target)
                || !target.isValid() || target.isDead() || player.isDead() || !player.isOnline()
                || target.getWorld() != player.getWorld()) {
            return;
        }
        refreshEquipment(player, false);
        Equipment state = equipment.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.expiresAt > 0L && player.getWorld().getGameTime() >= state.expiresAt) {
            resetCombo(state);
        }
        Location origin = player.getEyeLocation();
        BoundingBox bounds = target.getBoundingBox();
        double distanceSquared = GlaiveGeometry.distanceSquared(origin.toVector(), bounds);
        boolean stab = settings.stabEnabled() && state.stage == 2;
        if (!GlaiveGeometry.inRange(distanceSquared, stab, player.getGameMode() == GameMode.CREATIVE)
                || !visible(origin, bounds)) {
            event.setCancelled(true);
            combat.clear(player);
            return;
        }
        GlaiveCombatAdapter.Attack attack = combat.capture(player, target);
        double stabRange = player.getGameMode() == GameMode.CREATIVE
                ? ObsidianGlaiveDefinition.STAB_CREATIVE_RANGE : ObsidianGlaiveDefinition.STAB_RANGE;
        attempts.put(event, new Attempt(state, generation, target, origin.clone(), bounds.clone(), distanceSquared,
                stab, stabRange, attack));
        if (!stab) {
            event.setDamage(event.getDamage() * GlaiveGeometry.damageMultiplier(distanceSquared));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSuccessfulDamage(EntityDamageByEntityEvent event) {
        if (DamageFlow.isSecondaryDamage(event)) {
            SecondaryHit hit = secondaryHit.get();
            if (hit != null && event.getEntity() == hit.target) {
                hit.event = event;
            }
            return;
        }
        Attempt attempt = attempts.remove(event);
        if (attempt == null || event.isCancelled() || !isCurrent(attempt)) {
            return;
        }
        Player player = attempt.state.player;
        if (event.getFinalDamage() > 0.0D && validTarget(attempt, attempt.target)) {
            if (attempt.stab && settings.stabLiftEnabled()) {
                lift(attempt, attempt.target);
            } else if (!attempt.stab && GlaiveGeometry.damageMultiplier(attempt.distanceSquared) < 1.0D) {
                push(player, attempt.target, ObsidianGlaiveDefinition.CLOSE_KNOCKBACK);
            }
        }
        if (!isCurrent(attempt)) {
            return;
        }
        advance(attempt.state);
        if (attempt.stab && isCurrent(attempt)) {
            pierce(attempt);
        } else if (attempt.attack.sweepAllowed() && isCurrent(attempt)) {
            sweep(attempt);
        }
    }

    private void lift(Attempt attempt, LivingEntity target) {
        Vector velocity = target.getVelocity();
        velocity.setY(Math.max(velocity.getY(), ObsidianGlaiveDefinition.STAB_LIFT));
        target.setVelocity(velocity);
        if (validTarget(attempt, target)) {
            Location burst = target.getLocation().add(0.0D, 0.35D, 0.0D);
            burst.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, burst, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void pierce(Attempt attempt) {
        World world = attempt.origin.getWorld();
        Vector origin = attempt.origin.toVector();
        Vector direction = attempt.origin.getDirection();
        BoundingBox area = BoundingBox.of(origin, origin.clone().add(direction.clone().multiply(attempt.stabRange)))
                .expand(0.125D);
        if (!canAccess(world, area)) {
            return;
        }
        RayTraceResult obstruction = world.rayTraceBlocks(attempt.origin, direction, attempt.stabRange,
                FluidCollisionMode.NEVER, true);
        double range = obstruction == null ? attempt.stabRange : origin.distance(obstruction.getHitPosition());
        List<LineTarget> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(area)) {
            if (!isCurrent(attempt) || !canAccess(world, area)) {
                return;
            }
            if (!(entity instanceof LivingEntity target) || target == attempt.state.player || target == attempt.target
                    || !validTarget(attempt, target) || target instanceof ArmorStand stand && stand.isMarker()
                    || allied(attempt.state.player, target)) {
                continue;
            }
            BoundingBox bounds = target.getBoundingBox();
            double distance = GlaiveGeometry.rayDistance(origin, direction, bounds.clone().expand(0.125D), range);
            if (Double.isFinite(distance) && (obstruction == null || distance < range)
                    && GlaiveGeometry.distanceSquared(origin, bounds) <= attempt.stabRange * attempt.stabRange) {
                targets.add(new LineTarget(target, distance));
            }
        }
        targets.sort(Comparator.comparingDouble(LineTarget::distance));
        for (LineTarget candidate : targets) {
            LivingEntity target = candidate.target;
            if (!isCurrent(attempt) || !canAccess(world, area)) {
                return;
            }
            if (!validTarget(attempt, target)) {
                continue;
            }
            BoundingBox bounds = target.getBoundingBox();
            double distance = GlaiveGeometry.rayDistance(origin, direction, bounds.clone().expand(0.125D), range);
            if (!Double.isFinite(distance) || obstruction != null && distance >= range
                    || GlaiveGeometry.distanceSquared(origin, bounds) > attempt.stabRange * attempt.stabRange
                    || !visible(attempt.origin, origin.clone().add(direction.clone().multiply(distance)))) {
                continue;
            }
            double damage = attempt.attack.stabDamage(target);
            if (!Double.isFinite(damage) || damage <= 0.0D) {
                continue;
            }
            SecondaryHit observed = damageSecondary(attempt, target, damage, attempt.attack.stabDamageSource());
            if (observed.event == null || observed.event.isCancelled() || !validTarget(attempt, target)) {
                continue;
            }
            if (observed.event.getFinalDamage() > 0.0D && settings.stabLiftEnabled()) {
                lift(attempt, target);
            }
            if (validTarget(attempt, target)) {
                attempt.attack.afterStabHit(target);
            }
        }
    }

    private SecondaryHit damageSecondary(Attempt attempt, LivingEntity target, double damage,
            DamageSource damageSource) {
        SecondaryHit observed = new SecondaryHit(target);
        SecondaryHit previous = secondaryHit.get();
        secondaryHit.set(observed);
        try {
            DamageFlow.Result result = damageSource == null
                    ? DamageFlow.damage(target, damage, attempt.state.player)
                    : DamageFlow.damage(target, damage, attempt.state.player, damageSource);
            if (!result.accepted()) {
                observed.event = null;
            }
        } finally {
            if (previous == null) {
                secondaryHit.remove();
            } else {
                secondaryHit.set(previous);
            }
        }
        return observed;
    }

    private void sweep(Attempt attempt) {
        BoundingBox area = attempt.bounds.clone().expand(1.0D, 0.25D, 1.0D);
        World world = attempt.origin.getWorld();
        if (!canAccess(world, area)) {
            return;
        }
        Player player = attempt.state.player;
        for (Entity entity : world.getNearbyEntities(area)) {
            if (!isCurrent(attempt) || !canAccess(world, area)) {
                return;
            }
            if (!(entity instanceof LivingEntity target) || target == player || target == attempt.target
                    || !validTarget(attempt, target) || target instanceof ArmorStand stand && stand.isMarker()
                    || allied(player, target)) {
                continue;
            }
            BoundingBox bounds = target.getBoundingBox();
            double distanceSquared = GlaiveGeometry.distanceSquared(attempt.origin.toVector(), bounds);
            if (!GlaiveGeometry.inRange(distanceSquared, false, false) || !visible(attempt.origin, bounds)) {
                continue;
            }
            double damage = attempt.attack.sweepDamage(target) * GlaiveGeometry.damageMultiplier(distanceSquared);
            if (!Double.isFinite(damage) || damage <= 0.0D) {
                continue;
            }
            SecondaryHit observed = damageSecondary(attempt, target, damage, attempt.attack.sweepDamageSource());
            if (observed.event == null || !isCurrent(attempt) || !validTarget(attempt, target)) {
                continue;
            }
            if (observed.event.getFinalDamage() > 0.0D) {
                double strength = ObsidianGlaiveDefinition.SWEEP_KNOCKBACK;
                if (GlaiveGeometry.damageMultiplier(distanceSquared) < 1.0D) {
                    strength += ObsidianGlaiveDefinition.CLOSE_KNOCKBACK;
                }
                push(player, target, strength);
            }
            if (isCurrent(attempt) && validTarget(attempt, target)) {
                attempt.attack.afterSweepHit(target);
            }
        }
    }

    private static boolean allied(Player player, LivingEntity target) {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        return team != null && team.hasEntry(target instanceof Player other ? other.getName() : target.getUniqueId().toString());
    }

    private void advance(Equipment state) {
        synchronized (taskLock) {
            if (!running || equipment.get(state.playerId) != state) {
                return;
            }
            state.cancelExpiry();
            state.stage = settings.stabEnabled() ? (state.stage + 1) % 3 : 0;
            prepareHeld(state);
            state.expiresAt = state.world.getGameTime() + ObsidianGlaiveDefinition.COMBO_TIMEOUT_TICKS;
            Expiry expiry = new Expiry(state, generation);
            state.expiry = expiry;
            expiry.task = SchedulerCompat.runLaterForPlayer(state.player, plugin, expiry,
                    expiry::retire, ObsidianGlaiveDefinition.COMBO_TIMEOUT_TICKS);
            if (state.expiry != expiry || !running) {
                expiry.cancel();
            }
        }
    }

    private void resetCombo(Equipment state) {
        state.cancelExpiry();
        state.stage = 0;
        state.expiresAt = 0L;
        if (running && equipment.get(state.playerId) == state) {
            prepareHeld(state);
        }
    }

    private void prepareHeld(Equipment state) {
        ItemStack held = state.player.getInventory().getItemInMainHand();
        if (state.identity.equals(factory.getObsidianGlaiveIdentity(held))) {
            factory.prepareObsidianGlaive(held, state.stage);
            state.player.getInventory().setItemInMainHand(held);
        }
    }

    private boolean isCurrent(Attempt attempt) {
        Equipment state = attempt.state;
        if (!running || generation != attempt.generation || equipment.get(state.playerId) != state
                || !SchedulerCompat.isOwnedByCurrentRegion(state.player)
                || !state.player.isOnline() || state.player.isDead() || state.player.getWorld() != state.world
                || state.player.getInventory().getHeldItemSlot() != state.slot) {
            return false;
        }
        ItemStack held = state.player.getInventory().getItemInMainHand();
        return state.identity.equals(factory.getObsidianGlaiveIdentity(held))
                && state.stage == factory.getObsidianGlaiveStage(held);
    }

    private boolean validTarget(Attempt attempt, LivingEntity target) {
        return isCurrent(attempt) && SchedulerCompat.isOwnedByCurrentRegion(target)
                && target.isValid() && !target.isDead() && target.getWorld() == attempt.origin.getWorld();
    }

    private static boolean visible(Location origin, BoundingBox bounds) {
        return visible(origin, GlaiveGeometry.closestPoint(origin.toVector(), bounds));
    }

    private static boolean visible(Location origin, Vector destination) {
        BoundingBox rayBounds = BoundingBox.of(origin.toVector(), destination);
        World world = origin.getWorld();
        if (!canAccess(world, rayBounds)) {
            return false;
        }
        Vector ray = destination.subtract(origin.toVector());
        double distance = ray.length();
        return distance < 1.0E-9D || world.rayTraceBlocks(origin, ray.multiply(1.0D / distance), distance,
                FluidCollisionMode.NEVER, true) == null;
    }

    private static boolean canAccess(World world, BoundingBox bounds) {
        int minX = ((int) Math.floor(bounds.getMinX())) >> 4;
        int minZ = ((int) Math.floor(bounds.getMinZ())) >> 4;
        int maxX = ((int) Math.floor(bounds.getMaxX())) >> 4;
        int maxZ = ((int) Math.floor(bounds.getMaxZ())) >> 4;
        if (!SchedulerCompat.isOwnedByCurrentRegion(world, minX, minZ, maxX, maxZ)) {
            return false;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void push(Player player, LivingEntity target, double strength) {
        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0.0D);
        if (direction.lengthSquared() < 1.0E-9D) {
            double yaw = Math.toRadians(player.getLocation().getYaw());
            direction = new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        if (direction.lengthSquared() > 1.0E-9D) {
            target.setVelocity(target.getVelocity().add(direction.normalize().multiply(strength)));
        }
    }

    private void reset(Player player) {
        Equipment state = equipment.remove(player.getUniqueId());
        if (state != null) {
            state.cancelExpiry();
        }
        // Pre-attack snapshots are consumed separately; equipment refresh can run during damage dispatch.
        restoreInventory(player, false);
    }

    private void restoreInventory(Player player, boolean currentRevisionOnly) {
        PlayerInventory inventory = player.getInventory();
        restoreItems(inventory, currentRevisionOnly);
        ItemStack cursor = player.getItemOnCursor();
        if (ItemIds.OBSIDIAN_GLAIVE.equals(factory.getItemId(cursor))
                && (!currentRevisionOnly || factory.hasCurrentGlaiveRevision(cursor))) {
            factory.prepareObsidianGlaive(cursor, 0);
            player.setItemOnCursor(cursor);
        }
    }

    private void restoreItems(Inventory inventory, boolean currentRevisionOnly) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (ItemIds.OBSIDIAN_GLAIVE.equals(factory.getItemId(item))
                    && (!currentRevisionOnly || factory.hasCurrentGlaiveRevision(item))) {
                factory.prepareObsidianGlaive(item, 0);
                inventory.setItem(slot, item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (running && event.getWhoClicked() instanceof Player player
                && (equipment.containsKey(player.getUniqueId())
                || ItemIds.OBSIDIAN_GLAIVE.equals(factory.getItemId(event.getCurrentItem()))
                || ItemIds.OBSIDIAN_GLAIVE.equals(factory.getItemId(event.getCursor())))) {
            queueInventoryReset(player, event.getView().getTopInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (running && event.getWhoClicked() instanceof Player player
                && (equipment.containsKey(player.getUniqueId())
                || ItemIds.OBSIDIAN_GLAIVE.equals(factory.getItemId(event.getOldCursor())))) {
            queueInventoryReset(player, event.getView().getTopInventory());
        }
    }

    private void queueInventoryReset(Player player, Inventory topInventory) {
        UUID playerId = player.getUniqueId();
        Equipment state = equipment.remove(playerId);
        if (state != null) {
            state.cancelExpiry();
        }
        combat.clear(player);
        InventoryAccess access = new InventoryAccess(topInventory, topInventory.getLocation(),
                topInventory.getHolder() instanceof Entity entity ? entity : null);
        synchronized (taskLock) {
            if (!running) {
                return;
            }
            InventoryReset reset = inventoryResets.get(playerId);
            if (reset == null) {
                reset = new InventoryReset(player, generation);
                inventoryResets.put(playerId, reset);
                reset.inventories.add(access);
                InventoryReset scheduled = reset;
                reset.task = SchedulerCompat.runLaterForPlayer(player, plugin, reset,
                        () -> inventoryResets.remove(playerId, scheduled), 1L);
            } else if (reset.inventories.stream().noneMatch(entry -> entry.inventory == topInventory)) {
                reset.inventories.add(access);
            }
        }
    }

    private void cancelInventoryReset(Player player) {
        InventoryReset reset = inventoryResets.remove(player.getUniqueId());
        if (reset != null) {
            reset.cancel();
            reset.inventories.forEach(this::cleanupTransferred);
        }
    }

    private void cleanupTransferred(InventoryAccess access) {
        if (access.accessible()) {
            restoreItems(access.inventory, true);
        } else if (plugin.isEnabled()) {
            Runnable cleanup = () -> {
                if (access.accessible()) {
                    restoreItems(access.inventory, true);
                }
            };
            if (access.holder != null) {
                SchedulerCompat.runForEntity(access.holder, plugin, cleanup);
            } else if (access.location != null) {
                SchedulerCompat.runAtLocation(access.location, plugin, cleanup);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) {
        if (running) {
            reset(event.getPlayer());
            combat.clear(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (running) {
            reset(event.getPlayer());
            combat.clear(event.getPlayer());
            factory.prepareObsidianGlaive(event.getMainHandItem(), 0);
            factory.prepareObsidianGlaive(event.getOffHandItem(), 0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (running) {
            reset(event.getPlayer());
            combat.clear(event.getPlayer());
            ItemStack item = event.getItemDrop().getItemStack();
            factory.prepareObsidianGlaive(item, 0);
            event.getItemDrop().setItemStack(item);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelInventoryReset(event.getPlayer());
        reset(event.getPlayer());
        combat.clear(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancelInventoryReset(event.getPlayer());
        reset(event.getPlayer());
        combat.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        cancelInventoryReset(event.getEntity());
        reset(event.getEntity());
        combat.clear(event.getEntity());
        event.getDrops().forEach(item -> factory.prepareObsidianGlaive(item, 0));
    }

    private static final class Equipment {
        private final Player player;
        private final UUID playerId;
        private final int slot;
        private final String identity;
        private final World world;
        private int stage;
        private long expiresAt;
        private Expiry expiry;

        private Equipment(Player player, int slot, String identity, World world) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.slot = slot;
            this.identity = identity;
            this.world = world;
        }

        private void cancelExpiry() {
            Expiry old = expiry;
            expiry = null;
            if (old != null) {
                old.cancel();
            }
        }
    }

    private final class Expiry implements Runnable {
        private final Equipment state;
        private final long expectedGeneration;
        private ScheduledTaskCompat task;

        private Expiry(Equipment state, long expectedGeneration) {
            this.state = state;
            this.expectedGeneration = expectedGeneration;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                if (running && generation == expectedGeneration && equipment.get(state.playerId) == state
                        && state.expiry == this && state.player.isOnline()) {
                    resetCombo(state);
                }
            }
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
            }
        }

        private void retire() {
            synchronized (taskLock) {
                if (state.expiry == this) {
                    equipment.remove(state.playerId, state);
                }
            }
        }
    }

    private final class InventoryReset implements Runnable {
        private final Player player;
        private final UUID playerId;
        private final long expectedGeneration;
        private final List<InventoryAccess> inventories = new ArrayList<>();
        private ScheduledTaskCompat task;

        private InventoryReset(Player player, long expectedGeneration) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.expectedGeneration = expectedGeneration;
        }

        @Override
        public void run() {
            synchronized (taskLock) {
                if (!running || generation != expectedGeneration || !inventoryResets.remove(playerId, this)
                        || !player.isOnline()) {
                    return;
                }
                restoreInventory(player, false);
                for (InventoryAccess access : inventories) {
                    if (access.accessible()) {
                        restoreItems(access.inventory, true);
                    }
                }
                refreshEquipment(player, false);
            }
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
    }

    private record InventoryAccess(Inventory inventory, Location location, Entity holder) {
        private boolean accessible() {
            if (holder != null) {
                return SchedulerCompat.isOwnedByCurrentRegion(holder) && holder.isValid();
            }
            return location == null || canAccess(location.getWorld(), new BoundingBox(
                    location.getX() - 1, location.getY() - 1, location.getZ() - 1,
                    location.getX() + 1, location.getY() + 1, location.getZ() + 1));
        }
    }

    private record Attempt(Equipment state, long generation, LivingEntity target, Location origin,
            BoundingBox bounds, double distanceSquared, boolean stab, double stabRange, GlaiveCombatAdapter.Attack attack) {
    }

    private record LineTarget(LivingEntity target, double distance) {
    }

    private static final class SecondaryHit {
        private final LivingEntity target;
        private EntityDamageByEntityEvent event;

        private SecondaryHit(LivingEntity target) {
            this.target = target;
        }
    }
}
