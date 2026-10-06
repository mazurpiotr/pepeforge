package pepin.pepeforge.weapons.anchor;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pepin.pepeforge.util.ui.ActionBarHelper;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnchorListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final PluginLang lang;
    private final @NonNull NamespacedKey cooldownKey;
    private final Set<ItemDisplay> activeDisplays = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ItemStack> activeThrows = new ConcurrentHashMap<>();
    private final Map<UUID, Location> throwOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, ItemDisplay> flightDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTaskCompat> flightTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTaskCompat> pullTasks = new ConcurrentHashMap<>();
    private final Set<ScheduledTaskCompat> activeTasks = ConcurrentHashMap.newKeySet();

    private static final String ABILITY_COOLDOWN_KEY = "anchor:hook";
    private static final String CONFIG_PATH = "mechanics.anchor";

    public AnchorListener(JavaPlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager,
            PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.lang = lang;
        this.cooldownKey = new NamespacedKey(plugin, "anchor_snare_cooldown");
    }

    private long getAbilityCooldownMillis() {
        return Math.max(500L, Math.min(30_000L, plugin.getConfig().getLong(
                CONFIG_PATH + ".ability_cooldown", AnchorDefinition.DEFAULT_ABILITY_COOLDOWN_MILLIS)));
    }

    private int getSnareDurationTicks() {
        return Math.max(1, Math.min(200, plugin.getConfig().getInt(
                CONFIG_PATH + ".snare_duration", AnchorDefinition.DEFAULT_SNARE_DURATION_TICKS)));
    }

    private long getSnareCooldownMillis() {
        return Math.max(1_000L, Math.min(60_000L, plugin.getConfig().getLong(
                CONFIG_PATH + ".snare_cooldown", AnchorDefinition.DEFAULT_SNARE_COOLDOWN_MILLIS)));
    }

    private double getAbilityRange() {
        double configured = plugin.getConfig().getDouble(
                CONFIG_PATH + ".ability_range", AnchorDefinition.DEFAULT_ABILITY_RANGE);
        return Double.isFinite(configured) ? Math.max(5.0D, Math.min(50.0D, configured))
                : AnchorDefinition.DEFAULT_ABILITY_RANGE;
    }

    private boolean isSnareEnabled() {
        return plugin.getConfig().getBoolean(CONFIG_PATH + ".snare_enabled", AnchorDefinition.DEFAULT_SNARE_ENABLED);
    }

    private boolean isHookEnabled() {
        return plugin.getConfig().getBoolean(CONFIG_PATH + ".hook_enabled", AnchorDefinition.DEFAULT_HOOK_ENABLED);
    }

    public void cleanup() {
        for (ScheduledTaskCompat task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
        flightTasks.clear();
        pullTasks.clear();

        if (!SchedulerCompat.isServerStopping()) {
            for (ItemDisplay display : activeDisplays) {
                removeDisplay(display);
            }
        }
        activeDisplays.clear();
        flightDisplays.clear();

        for (UUID uuid : activeThrows.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null) {
                ItemStack stored = takeStoredAnchor(uuid);
                Location origin = throwOrigins.remove(uuid);
                if (stored != null && origin != null) {
                    dropAtLocation(origin, stored);
                }
                continue;
            }

            SchedulerCompat.runForPlayer(player, plugin, () -> {
                ItemStack stored = takeStoredAnchor(uuid);
                if (stored != null) {
                    throwOrigins.remove(uuid);
                    returnItemToPlayer(player, stored, player.getLocation());
                }
            }, () -> {
                ItemStack stored = takeStoredAnchor(uuid);
                Location origin = throwOrigins.remove(uuid);
                if (stored != null && origin != null) {
                    dropAtLocation(origin, stored);
                }
            });
        }
    }

    private ItemStack takeStoredAnchor(UUID uuid) {
        return activeThrows.remove(uuid);
    }

    private void removeDisplay(ItemDisplay display) {
        activeDisplays.remove(display);
        Runnable remove = () -> {
            if (display.isValid()) {
                display.remove();
            }
        };
        if (SchedulerCompat.isOwnedByCurrentRegion(display)) {
            remove.run();
        } else {
            SchedulerCompat.runForEntity(display, plugin, remove);
        }
    }

    private void dropAtLocation(Location location, ItemStack item) {
        Location dropLocation = location.clone();
        SchedulerCompat.runAtLocation(dropLocation, plugin, () -> {
            World world = dropLocation.getWorld();
            if (world != null) {
                world.dropItemNaturally(dropLocation, item);
            }
        });
    }

    private void cancelPlayerTasks(UUID uuid) {
        ScheduledTaskCompat flightTask = flightTasks.remove(uuid);
        if (flightTask != null) {
            flightTask.cancel();
            activeTasks.remove(flightTask);
        }

        ScheduledTaskCompat pullTask = pullTasks.remove(uuid);
        if (pullTask != null) {
            pullTask.cancel();
            activeTasks.remove(pullTask);
        }

        ItemDisplay display = flightDisplays.remove(uuid);
        if (display != null) {
            removeDisplay(display);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (DamageFlow.isSecondaryDamage(event)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target == player) {
            return;
        }

        if (!itemFactory.isAnchor(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (!isSnareEnabled()) {
            return;
        }

        PersistentDataContainer pdc = target.getPersistentDataContainer();
        long now = System.currentTimeMillis();
        @NonNull PersistentDataType<Long, Long> longType = Objects.requireNonNull(PersistentDataType.LONG);
        Long cooldownUntil = pdc.get(cooldownKey, longType);

        if (cooldownUntil == null || now >= cooldownUntil) {
            // Apply Snare (SLOWNESS 10) for the configured duration
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, getSnareDurationTicks(), 9,
                    false, false, true));

            // Set per-target cooldown
            pdc.set(cooldownKey, PersistentDataType.LONG, now + getSnareCooldownMillis());

            // Audio-Visual feedback
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);

            // Spawn Horn Coral wrapping the feet
            @NonNull World targetWorld = Objects.requireNonNull(target.getWorld());
            @NonNull ItemDisplay coral = Objects.requireNonNull(
                    targetWorld.spawn(target.getLocation().add(0, 0.1, 0), ItemDisplay.class, entity -> {
                        entity.setItemStack(new ItemStack(Material.HORN_CORAL));
                        entity.setGravity(false);
                        entity.setPersistent(false);
                        Transformation trans = entity.getTransformation();
                        trans.getScale().set(1.3f, 1.3f, 1.3f);
                        entity.setTransformation(trans);
                    }));
            activeDisplays.add(coral);

            class SnareEffectTask implements Runnable {
                private int tick = 0;
                private ScheduledTaskCompat taskRef;

                @Override
                public void run() {
                    tick++;
                    if (!target.isValid() || target.isDead() || tick > getSnareDurationTicks()) {
                        cleanup();
                        return;
                    }

                    // Lock horizontal movement while preserving vertical Y velocity
                    Vector v = target.getVelocity();
                    target.setVelocity(new Vector(0.0, v.getY(), 0.0));

                    Location loc = target.getLocation().add(0, 0.1, 0);
                    SchedulerCompat.teleport(coral, loc);
                }

                private void cleanup() {
                    if (taskRef != null) {
                        taskRef.cancel();
                        activeTasks.remove(taskRef);
                    }
                    removeDisplay(coral);
                }
            }

            SnareEffectTask snareTask = new SnareEffectTask();
            ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(target, plugin, snareTask, 1L, 1L);
            snareTask.taskRef = task;
            activeTasks.add(task);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!itemFactory.isAnchor(mainHandItem)) {
            return;
        }

        if (!isHookEnabled()) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType().isInteractable()
                && !player.isSneaking()) {
            return;
        }

        denyInteraction(event);

        long remainingMillis = cooldownManager.getRemainingCooldownMillis(player, ABILITY_COOLDOWN_KEY);
        if (remainingMillis > 0L) {
            showCooldownActionBar(player, remainingMillis);
            return;
        }

        @NonNull Material mainHandMaterial = Objects.requireNonNull(mainHandItem.getType());
        if (!executeAnchorThrow(player, mainHandItem)) {
            return;
        }

        cooldownManager.setCooldown(player, ABILITY_COOLDOWN_KEY, getAbilityCooldownMillis());
        player.setCooldown(mainHandMaterial, 20);
        player.swingMainHand();
    }

    private boolean executeAnchorThrow(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        ItemStack anchorItem = item.clone();
        if (activeThrows.putIfAbsent(playerId, anchorItem) != null) {
            return false;
        }

        Location startLoc = player.getEyeLocation().add(0, -0.3, 0);
        Vector direction = player.getLocation().getDirection().normalize();

        // Clone item for safety and temporarily clear it from the hand
        player.getInventory().setItemInMainHand(null);
        throwOrigins.put(playerId, startLoc.clone());

        @NonNull World playerWorld = Objects.requireNonNull(player.getWorld());
        @NonNull ItemDisplay display = Objects.requireNonNull(
                playerWorld.spawn(startLoc, ItemDisplay.class, entity -> {
                    entity.setItemStack(anchorItem);
                    entity.setGravity(false);
                    entity.setPersistent(false);
                    // Rotate the model internally by 90 degrees around the Y-axis (yaw) so the
                    // narrow side (handle)
                    // faces the player. This lets the entity's actual pitch rotate correctly along
                    // the flight path.
                    Transformation trans = entity.getTransformation();
                    trans.getLeftRotation().rotateY((float) Math.toRadians(90.0));
                    trans.getScale().set(2.0f, 2.0f, 2.0f);
                    entity.setTransformation(trans);
                }));

        activeDisplays.add(display);
        flightDisplays.put(player.getUniqueId(), display);

        class AnchorFlightTask implements Runnable {
            private int tick = 0;
            private ScheduledTaskCompat taskRef;
            private final Location currentLoc = startLoc.clone();
            private final Vector velocity = direction.multiply(AnchorDefinition.THROW_SPEED); // Fired with configured
                                                                                              // velocity
            private Location hitLocation;

            @Override
            public void run() {
                tick++;
                if (!player.isOnline() || player.isDead() || player.getWorld() != startLoc.getWorld()
                        || !SchedulerCompat.isOwnedByCurrentRegion(display)
                        || display.isDead() || !display.isValid() || tick > 60) {
                    cleanup();
                    return;
                }

                // Capped by ability range
                if (currentLoc.distance(startLoc) > getAbilityRange()) {
                    cleanup();
                    return;
                }

                Location oldLoc = currentLoc.clone();

                // Apply physics (gravity + drag)
                currentLoc.add(velocity);
                velocity.setY(velocity.getY() - 0.05D); // gravity (0.05 blocks/tick²)
                velocity.multiply(0.99D); // drag (0.99 multiplier)

                if (SchedulerCompat.isRegionized() && (!Bukkit.isOwnedByCurrentRegion(oldLoc, 1)
                        || !Bukkit.isOwnedByCurrentRegion(currentLoc, 1))) {
                    cleanup();
                    return;
                }

                // Check collision from old location to new location
                Vector movement = currentLoc.toVector().subtract(oldLoc.toVector());
                double dist = movement.length();
                RayTraceResult hit = null;
                if (dist > 0.01D) {
                    hit = player.getWorld().rayTrace(
                            oldLoc,
                            movement.clone().normalize(),
                            dist,
                            FluidCollisionMode.NEVER,
                            true,
                            0.4D,
                            entity -> entity != player && entity instanceof LivingEntity);
                }

                if (hit != null && (hit.getHitBlock() != null || hit.getHitEntity() != null)) {
                    Location impactLoc = hit.getHitPosition().toLocation(player.getWorld());
                    hitLocation = impactLoc.clone();
                    try {
                        onHit(hit, impactLoc);
                    } finally {
                        cleanup();
                    }
                    return;
                }

                // Update display position and angle
                Location teleportLoc = currentLoc.clone();
                if (velocity.lengthSquared() > 0.01D) {
                    teleportLoc.setDirection(velocity);
                }

                SchedulerCompat.teleport(display, teleportLoc);
                drawChain(player.getEyeLocation().add(0, -0.3, 0), currentLoc);
            }

            private void onHit(RayTraceResult hit, Location impactLoc) {
                if (hit.getHitEntity() != null && hit.getHitEntity() instanceof LivingEntity target) {
                    hitEntity(player, target, impactLoc);
                } else if (hit.getHitBlock() != null) {
                    // continuous grapple pull task
                    class PlayerPullTask implements Runnable {
                        private int pullTick = 0;
                        private ScheduledTaskCompat pullTaskRef;

                        @Override
                        public void run() {
                            pullTick++;
                            if (!player.isOnline() || player.isDead() || player.getWorld() != impactLoc.getWorld()
                                    || pullTick > 10) {
                                cleanupPull();
                                return;
                            }

                            Vector toBlock = impactLoc.toVector().subtract(player.getLocation().toVector());
                            double dist = toBlock.length();
                            if (dist < 1.8D) {
                                cleanupPull();
                                return;
                            }

                            Vector dir = toBlock.normalize();
                            double speed = AnchorDefinition.PULL_FORCE * 0.75D;
                            double yVel = dir.getY() * speed;
                            if (yVel < 0.2D) {
                                yVel += AnchorDefinition.PULL_LIFT * 0.45D;
                            }

                            Vector vel = new Vector(dir.getX() * speed, yVel, dir.getZ() * speed);
                            player.setVelocity(vel);
                        }

                        private void cleanupPull() {
                            if (pullTaskRef != null) {
                                pullTaskRef.cancel();
                                activeTasks.remove(pullTaskRef);
                                pullTasks.remove(player.getUniqueId(), pullTaskRef);
                            }
                        }
                    }

                    PlayerPullTask pullTask = new PlayerPullTask();
                    ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, pullTask, 0L, 1L);
                    pullTask.pullTaskRef = task;
                    activeTasks.add(task);
                    pullTasks.put(player.getUniqueId(), task);

                    player.getWorld().playSound(impactLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
                    player.getWorld().playSound(impactLoc, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);
                    player.getWorld().spawnParticle(
                            Particle.BLOCK,
                            impactLoc,
                            15,
                            0.2,
                            0.2,
                            0.2,
                            0.0,
                            hit.getHitBlock().getBlockData());
                }
            }

            private void drawChain(Location start, Location end) {
                Vector direction = end.toVector().subtract(start.toVector());
                double distance = direction.length();
                if (distance > 0.1D) {
                    direction.normalize();
                    double step = 0.4D;
                    for (double d = 0.0D; d < distance; d += step) {
                        Location point = start.clone().add(direction.clone().multiply(d));
                        player.getWorld().spawnParticle(
                                Particle.DUST,
                                point,
                                1,
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 110, 70), 0.8f));
                    }
                }
            }

            private void cleanup() {
                if (taskRef != null) {
                    taskRef.cancel();
                    activeTasks.remove(taskRef);
                    flightTasks.remove(player.getUniqueId(), taskRef);
                }
                flightDisplays.remove(player.getUniqueId(), display);
                removeDisplay(display);

                Runnable returnAnchor = () -> {
                    ItemStack stored = activeThrows.remove(player.getUniqueId());
                    if (stored != null) {
                        throwOrigins.remove(player.getUniqueId());
                        Location fallbackLocation = hitLocation == null ? player.getLocation() : hitLocation;
                        returnItemToPlayer(player, stored, fallbackLocation);
                    }
                };
                if (SchedulerCompat.isOwnedByCurrentRegion(player)) {
                    returnAnchor.run();
                } else {
                    SchedulerCompat.runForPlayer(player, plugin, returnAnchor);
                }
            }
        }

        AnchorFlightTask flightTask = new AnchorFlightTask();
        ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, flightTask, 1L, 1L);
        flightTask.taskRef = task;
        flightTasks.put(player.getUniqueId(), task);
        activeTasks.add(task);
        return true;
    }

    private void hitEntity(Player player, LivingEntity target, Location impactLoc) {
        if (!SchedulerCompat.isOwnedByCurrentRegion(player) || !SchedulerCompat.isOwnedByCurrentRegion(target)
                || !player.isOnline() || player.isDead() || !target.isValid() || target.isDead()
                || player.getWorld() != target.getWorld()) {
            return;
        }

        Location playerLocation = player.getLocation();
        Location targetLocation = target.getLocation();
        Vector toTarget = targetLocation.toVector().subtract(playerLocation.toVector());
        double distance = toTarget.length();
        if (!DamageFlow.damage(target, AnchorDefinition.HOOK_DAMAGE, player).accepted()) {
            return;
        }

        // Keep the original hit positions even when this hit kills the target.
        if (distance > 2.2D) {
            double pullDistance = (distance - 2.0D) / 2.0D;
            double speed = Math.min(AnchorDefinition.PULL_FORCE * 0.7D, pullDistance * 0.35D);
            Vector direction = toTarget.normalize();
            if (SchedulerCompat.isOwnedByCurrentRegion(player) && player.isOnline() && !player.isDead()
                    && player.getWorld() == playerLocation.getWorld()) {
                player.setVelocity(direction.clone().multiply(speed).setY(AnchorDefinition.PULL_LIFT));
            }
            if (SchedulerCompat.isOwnedByCurrentRegion(target) && target.isValid() && !target.isDead()
                    && target.getWorld() == targetLocation.getWorld()) {
                target.setVelocity(direction.clone().multiply(-speed).setY(AnchorDefinition.PULL_LIFT));
            }
        }
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
        impactLoc.getWorld().playSound(impactLoc, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);
    }

    private void denyInteraction(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void showCooldownActionBar(Player player, long remainingMillis) {
        double seconds = remainingMillis / 1000.0D;
        double progress = Math.max(0.0D,
                Math.min(1.0D, 1.0D - ((double) remainingMillis / getAbilityCooldownMillis())));
        String bar = ActionBarHelper.buildProgressBar(progress);
        String message = lang.text("messages.anchor.cooldown")
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", seconds));
        ActionBarHelper.showActionBar(player, message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location quitLocation = player.getLocation().clone();
        cancelPlayerTasks(uuid);

        ItemStack stored = takeStoredAnchor(uuid);
        if (stored != null) {
            throwOrigins.remove(uuid);
            returnItemOnQuit(player, stored, quitLocation);
        }
    }

    private void returnItemOnQuit(Player player, ItemStack item, Location quitLocation) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.getInventory().setItemInMainHand(item);
            return;
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        for (ItemStack rest : remaining.values()) {
            World world = quitLocation.getWorld();
            if (world != null) {
                world.dropItemNaturally(quitLocation, rest);
            }
        }
    }

    private void returnItemToPlayer(Player player, ItemStack item, Location dropLocation) {
        if (player.isOnline() && !player.isDead()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.getInventory().setItemInMainHand(item);
                return;
            }

            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            if (remaining.isEmpty()) {
                return;
            }

            for (ItemStack rest : remaining.values()) {
                Location fallbackLocation = dropLocation == null ? player.getLocation() : dropLocation;
                dropAtLocation(fallbackLocation, rest);
            }
            return;
        }

        if (dropLocation != null) {
            dropAtLocation(dropLocation, item);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        cancelPlayerTasks(uuid);
        ItemStack stored = takeStoredAnchor(uuid);
        if (stored != null) {
            throwOrigins.remove(uuid);
            event.getDrops().add(stored);
        }
    }
}
