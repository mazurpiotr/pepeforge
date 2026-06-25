package pepin.pepeforge.weapons.anchor;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pepin.pepeforge.util.ProtectionUtil;
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
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.CooldownManager;
import pepin.pepeforge.util.ScheduledTaskCompat;
import pepin.pepeforge.util.SchedulerCompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnchorListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final PluginLang lang;
    private final NamespacedKey cooldownKey;
    private final Set<ItemDisplay> activeDisplays = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ItemStack> activeThrows = new ConcurrentHashMap<>();

    private static final String ABILITY_COOLDOWN_KEY = "anchor:hook";

    public AnchorListener(JavaPlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager, PluginLang lang) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.lang = lang;
        this.cooldownKey = new NamespacedKey(plugin, "anchor_snare_cooldown");
    }

    public void cleanup() {
        for (ItemDisplay display : activeDisplays) {
            if (display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();

        for (Map.Entry<UUID, ItemStack> entry : activeThrows.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline() && !player.isDead()) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    player.getInventory().setItemInMainHand(entry.getValue());
                } else {
                    HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(entry.getValue());
                    if (!remaining.isEmpty()) {
                        for (ItemStack rest : remaining.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), rest);
                        }
                    }
                }
            } else if (player != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), entry.getValue());
            }
        }
        activeThrows.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
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

        PersistentDataContainer pdc = target.getPersistentDataContainer();
        long now = System.currentTimeMillis();
        Long cooldownUntil = pdc.get(cooldownKey, PersistentDataType.LONG);

        if (cooldownUntil == null || now >= cooldownUntil) {
            // Apply Snare (SLOWNESS 10 + JUMP_BOOST 250) for the configured duration (e.g. 40 ticks = 2s)
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, AnchorDefinition.SNARE_DURATION_TICKS, 9, false, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, AnchorDefinition.SNARE_DURATION_TICKS, 249, false, false, false));

            // Set per-target cooldown
            pdc.set(cooldownKey, PersistentDataType.LONG, now + AnchorDefinition.SNARE_COOLDOWN_MILLIS);

            // Audio-Visual feedback
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);

            // Spawn Tube Coral wrapping the feet
            ItemDisplay coral = target.getWorld().spawn(target.getLocation().add(0, 0.1, 0), ItemDisplay.class, entity -> {
                entity.setItemStack(new ItemStack(Material.TUBE_CORAL));
                entity.setGravity(false);
                entity.setPersistent(false);
                Transformation trans = entity.getTransformation();
                trans.getScale().set(1.3f, 1.3f, 1.3f);
                entity.setTransformation(trans);
            });
            activeDisplays.add(coral);

            class SnareEffectTask implements Runnable {
                private int tick = 0;
                private ScheduledTaskCompat taskRef;

                @Override
                public void run() {
                    tick++;
                    if (!target.isValid() || target.isDead() || tick > AnchorDefinition.SNARE_DURATION_TICKS) {
                        cleanup();
                        return;
                    }

                    Location loc = target.getLocation().add(0, 0.1, 0);
                    SchedulerCompat.teleport(coral, loc);
                }

                private void cleanup() {
                    if (taskRef != null) {
                        taskRef.cancel();
                    }
                    if (coral.isValid()) {
                        coral.remove();
                    }
                    activeDisplays.remove(coral);
                }
            }

            SnareEffectTask snareTask = new SnareEffectTask();
            ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(target, plugin, snareTask, 1L, 1L);
            snareTask.taskRef = task;
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

        cooldownManager.setCooldown(player, ABILITY_COOLDOWN_KEY, AnchorDefinition.ABILITY_COOLDOWN_MILLIS);
        player.setCooldown(mainHandItem.getType(), 20);
        player.swingMainHand();

        executeAnchorThrow(player, mainHandItem);
    }

    private void executeAnchorThrow(Player player, ItemStack item) {
        Location startLoc = player.getEyeLocation().add(0, -0.3, 0);
        Vector direction = player.getLocation().getDirection().normalize();

        // Clone item for safety and temporarily clear it from the hand
        ItemStack anchorItem = item.clone();
        player.getInventory().setItemInMainHand(null);
        activeThrows.put(player.getUniqueId(), anchorItem);

        ItemDisplay display = player.getWorld().spawn(startLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(anchorItem);
            entity.setGravity(false);
            entity.setPersistent(false);
            // Rotate the model internally by 90 degrees around the Y-axis (yaw) so the narrow side (handle)
            // faces the player. This lets the entity's actual pitch rotate correctly along the flight path.
            Transformation trans = entity.getTransformation();
            trans.getLeftRotation().rotateY((float) Math.toRadians(90.0));
            trans.getScale().set(2.0f, 2.0f, 2.0f);
            entity.setTransformation(trans);
        });

        activeDisplays.add(display);

        class AnchorFlightTask implements Runnable {
            private int tick = 0;
            private ScheduledTaskCompat taskRef;
            private final Location currentLoc = startLoc.clone();
            private final Vector velocity = direction.multiply(AnchorDefinition.THROW_SPEED); // Fired with configured velocity

            @Override
            public void run() {
                tick++;
                if (!player.isOnline() || display.isDead() || !display.isValid() || tick > 60) {
                    cleanup();
                    return;
                }

                // Capped by ability range (10 blocks)
                if (currentLoc.distance(startLoc) > AnchorDefinition.ABILITY_RANGE) {
                    cleanup();
                    return;
                }

                Location oldLoc = currentLoc.clone();

                // Apply physics (gravity + drag)
                currentLoc.add(velocity);
                velocity.setY(velocity.getY() - 0.05D); // gravity (0.05 blocks/tick²)
                velocity.multiply(0.99D); // drag (0.99 multiplier)

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
                            entity -> entity != player && entity instanceof LivingEntity
                    );
                }

                if (hit != null && (hit.getHitBlock() != null || hit.getHitEntity() != null)) {
                    Location impactLoc = hit.getHitPosition().toLocation(player.getWorld());
                    onHit(hit, impactLoc);
                    cleanup();
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
                    // Protection PvP/PvE check
                    if (ProtectionUtil.canDamage(player, target)) {
                        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
                        double distance = toTarget.length();
                        if (distance > 2.2D) { // Only pull if they are outside the 2-block gap
                            Vector dir = toTarget.clone().normalize();

                            // Each entity covers half of the remaining distance after leaving a 2.0 block gap
                            double pullDistance = (distance - 2.0D) / 2.0D;
                            double speed = Math.min(AnchorDefinition.PULL_FORCE * 0.7D, pullDistance * 0.35D);

                            Vector playerVel = dir.clone().multiply(speed).setY(AnchorDefinition.PULL_LIFT);
                            Vector targetVel = dir.clone().multiply(-speed).setY(AnchorDefinition.PULL_LIFT);

                            player.setVelocity(playerVel);
                            SchedulerCompat.runForEntity(target, plugin, () -> target.setVelocity(targetVel));
                        }
                        target.getWorld().playSound(impactLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
                        target.getWorld().playSound(impactLoc, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);
                    }
                } else if (hit.getHitBlock() != null) {
                    // continuous grapple pull task
                    class PlayerPullTask implements Runnable {
                        private int pullTick = 0;
                        private ScheduledTaskCompat pullTaskRef;

                        @Override
                        public void run() {
                            pullTick++;
                            if (!player.isOnline() || player.isDead() || pullTick > 10) {
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
                            }
                        }
                    }

                    PlayerPullTask pullTask = new PlayerPullTask();
                    ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, pullTask, 0L, 1L);
                    pullTask.pullTaskRef = task;

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
                            hit.getHitBlock().getBlockData()
                    );
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
                                new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 110, 70), 0.8f)
                        );
                    }
                }
            }

            private void cleanup() {
                if (taskRef != null) {
                    taskRef.cancel();
                }
                display.remove();
                activeDisplays.remove(display);

                activeThrows.remove(player.getUniqueId());
                if (player.isOnline() && !player.isDead()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType().isAir()) {
                        player.getInventory().setItemInMainHand(anchorItem);
                    } else {
                        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(anchorItem);
                        if (!remaining.isEmpty()) {
                            for (ItemStack rest : remaining.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), rest);
                            }
                        }
                    }
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), anchorItem);
                }
            }
        }

        AnchorFlightTask flightTask = new AnchorFlightTask();
        ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, flightTask, 1L, 1L);
        flightTask.taskRef = task;
    }

    private void denyInteraction(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void showCooldownActionBar(Player player, long remainingMillis) {
        double seconds = remainingMillis / 1000.0D;
        double progress = Math.max(0.0D, Math.min(1.0D, 1.0D - ((double) remainingMillis / AnchorDefinition.ABILITY_COOLDOWN_MILLIS)));
        String bar = buildProgressBar(progress);
        String message = lang.text("messages.anchor.cooldown")
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", seconds));
        showActionBar(player, message);
    }

    private void showActionBar(Player player, String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
    }

    private String buildProgressBar(double progress) {
        int filled = (int) Math.round(progress * 20);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? "&a|" : "&8|");
        }
        return bar.toString();
    }
}
