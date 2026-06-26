package pepin.pepeforge.weapons.katana;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.CombatUtils;
import pepin.pepeforge.util.CooldownManager;
import pepin.pepeforge.util.ScheduledTaskCompat;
import pepin.pepeforge.util.SchedulerCompat;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KatanaListener implements Listener {

    private static final int COOLDOWN_BAR_SEGMENTS = 20;
    private static final int OFF_HAND_INVENTORY_SLOT = 40;
    private static final String PARRY_COOLDOWN_KEY = "katana:parry";

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final CooldownManager cooldownManager;
    private final NamespacedKey reflectUntilKey;
    private final Map<UUID, Long> activeParryUntil = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTaskCompat> activeTasks = new ConcurrentHashMap<>();

    public KatanaListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.cooldownManager = cooldownManager;
        this.reflectUntilKey = new NamespacedKey(plugin, "katana_reflect_until");
    }

    private ScheduledTaskCompat statusTask;

    public void startStatusTask() {
        statusTask = SchedulerCompat.runTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                SchedulerCompat.runForPlayer(player, plugin, () -> {
                    if (!itemFactory.isKatana(player.getInventory().getItemInMainHand())) {
                        clearActiveParry(player);
                        return;
                    }

                    if (!CombatUtils.hasEmptyOffHand(player)) {
                        clearActiveParry(player);
                        showActionBar(player, lang.text("messages.two_handed.offhand_required"));
                        return;
                    }

                    long remainingMillis = cooldownManager.getRemainingCooldownMillis(player, PARRY_COOLDOWN_KEY);
                    if (remainingMillis > 0L) {
                        showCooldownActionBar(player, remainingMillis);
                    }
                });
            }
        }, 1L, 2L);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        for (ScheduledTaskCompat task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        activeParryUntil.clear();
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

        if (!CombatUtils.hasEmptyOffHand(player)) {
            // Katana stays equipable with an occupied off-hand, but its custom
            // mechanics are disabled until the player goes back to a valid 2H state.
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            denyInteraction(event);
            return;
        }

        boolean attemptedParry = isAttemptedParryInput(player, action, event.getClickedBlock());
        if (!attemptedParry) {
            return;
        }

        long now = System.currentTimeMillis();
        if (isParryActive(player, now)) {
            denyInteraction(event);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack newMainHandItem = inventory.getItem(event.getNewSlot());
        if (itemFactory.isKatana(newMainHandItem) && !CombatUtils.hasEmptyOffHand(player)) {
            SchedulerCompat.runForPlayer(player, plugin, () ->
                    showActionBar(player, lang.text("messages.two_handed.offhand_required"))
            );
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (itemFactory.isKatana(player.getInventory().getItemInMainHand())
                || itemFactory.isKatana(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean mainHandKatana = itemFactory.isKatana(player.getInventory().getItemInMainHand());
        boolean offHandClick = event.getClick() == ClickType.SWAP_OFFHAND || isPlayerOffHandSlotClick(event);
        if ((mainHandKatana && offHandClick) || isMovingKatanaToOffHand(event, player)) {
            event.setCancelled(true);
            return;
        }

        if (mainHandKatana && !CombatUtils.hasEmptyOffHand(player)) {
            SchedulerCompat.runForPlayer(player, plugin, () ->
                    showActionBar(player, lang.text("messages.two_handed.offhand_required"))
            );
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean dragsKatanaToOffHand = itemFactory.isKatana(event.getOldCursor())
                && event.getRawSlots().stream().anyMatch(rawSlot -> isPlayerOffHandRawSlot(event, rawSlot));
        if (dragsKatanaToOffHand) {
            event.setCancelled(true);
            return;
        }

        if (itemFactory.isKatana(player.getInventory().getItemInMainHand()) && !CombatUtils.hasEmptyOffHand(player)) {
            SchedulerCompat.runForPlayer(player, plugin, () ->
                    showActionBar(player, lang.text("messages.two_handed.offhand_required"))
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!isParryActive(player, now) || !CombatUtils.hasEmptyOffHand(player)) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile) {
            if (!isProjectileReflectable(player, projectile, now)) {
                return;
            }
            event.setCancelled(true);
            reflectProjectile(player, projectile, now);
            playReflectEffects(player.getWorld(), player.getEyeLocation());
            return;
        }

        if (!(damager instanceof LivingEntity attacker)) {
            return;
        }

        if (!CombatUtils.isInFront(player, attacker.getEyeLocation().toVector(), KatanaDefinition.PARRY_FRONT_DOT_THRESHOLD)) {
            return;
        }

        event.setCancelled(true);
        knockBackAttacker(player, attacker);
        playMeleeParryEffects(player.getWorld(), player.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        ScheduledTaskCompat task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        activeParryUntil.remove(playerId);
        cooldownManager.clearCooldown(event.getPlayer(), PARRY_COOLDOWN_KEY);
    }

    private void activateParry(Player player, ItemStack katana, long now) {
        long durationMillis = KatanaDefinition.PARRY_DURATION_TICKS * 50L;
        long cooldownMillis = KatanaDefinition.COOLDOWN_TICKS * 50L;
        long activeUntil = now + durationMillis;

        activeParryUntil.put(player.getUniqueId(), activeUntil);
        cooldownManager.setCooldown(player, PARRY_COOLDOWN_KEY, cooldownMillis);
        itemFactory.setKatanaParryVisual(katana, true);

        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        world.spawnParticle(Particle.SWEEP_ATTACK, eyeLocation, 1, 0.2, 0.2, 0.2, 0.0);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.2f);
        world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.75f, 1.4f);
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.35f, 1.7f);

        ScheduledTaskCompat previousTask = activeTasks.remove(player.getUniqueId());
        if (previousTask != null) {
            previousTask.cancel();
        }

        ScheduledTaskCompat task = SchedulerCompat.runTimerForEntity(player, plugin, () -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= activeParryUntil.getOrDefault(player.getUniqueId(), 0L) || !player.isOnline()) {
                clearActiveParry(player, katana);
                return;
            }

            if (!CombatUtils.hasEmptyOffHand(player) || !itemFactory.isKatana(player.getInventory().getItemInMainHand())) {
                clearActiveParry(player, katana);
                return;
            }
            reflectNearbyProjectiles(player, currentTime);
        }, 0L, 1L);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void reflectNearbyProjectiles(Player player, long now) {
        Location eyeLocation = player.getEyeLocation();
        double radius = KatanaDefinition.PROJECTILE_SCAN_RADIUS;
        for (Entity entity : player.getWorld().getNearbyEntities(eyeLocation, radius, radius, radius)) {
            if (!(entity instanceof Projectile projectile)) {
                continue;
            }
            if (!isProjectileReflectable(player, projectile, now)) {
                continue;
            }
            reflectProjectile(player, projectile, now);
            playReflectEffects(player.getWorld(), projectile.getLocation());
        }
    }

    private boolean isProjectileReflectable(Player player, Projectile projectile, long now) {
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

        if (!CombatUtils.isInFront(player, projectile.getLocation().toVector(), KatanaDefinition.PARRY_FRONT_DOT_THRESHOLD)) {
            return false;
        }

        Vector toPlayer = player.getEyeLocation().toVector().subtract(projectile.getLocation().toVector());
        if (toPlayer.lengthSquared() < 0.001D) {
            return true;
        }

        return velocity.normalize().dot(toPlayer.normalize()) >= KatanaDefinition.PROJECTILE_TOWARD_PLAYER_DOT_THRESHOLD;
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

    private boolean isParryActive(Player player, long now) {
        return activeParryUntil.getOrDefault(player.getUniqueId(), 0L) > now;
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

    private boolean isPlayerOffHandSlotClick(InventoryClickEvent event) {
        return event.getClickedInventory() instanceof PlayerInventory
                && event.getSlot() == OFF_HAND_INVENTORY_SLOT;
    }

    private boolean isMovingKatanaToOffHand(InventoryClickEvent event, Player player) {
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return itemFactory.isKatana(event.getCurrentItem());
        }

        if (!isPlayerOffHandSlotClick(event)) {
            return false;
        }

        if (itemFactory.isKatana(event.getCursor())) {
            return true;
        }

        return event.getClick() == ClickType.NUMBER_KEY
                && event.getHotbarButton() >= 0
                && itemFactory.isKatana(player.getInventory().getItem(event.getHotbarButton()));
    }

    private boolean isPlayerOffHandRawSlot(InventoryDragEvent event, int rawSlot) {
        return event.getView().getInventory(rawSlot) instanceof PlayerInventory
                && event.getView().convertSlot(rawSlot) == OFF_HAND_INVENTORY_SLOT;
    }


    private void clearActiveParry(Player player) {
        clearActiveParry(player, player.getInventory().getItemInMainHand());
    }

    private void clearActiveParry(Player player, ItemStack katana) {
        UUID playerId = player.getUniqueId();
        if (!activeParryUntil.containsKey(playerId) && !activeTasks.containsKey(playerId)) {
            return;
        }

        if (itemFactory.isKatana(katana)) {
            itemFactory.setKatanaParryVisual(katana, false);
        }
        activeParryUntil.remove(playerId);
        ScheduledTaskCompat activeTask = activeTasks.remove(playerId);
        if (activeTask != null) {
            activeTask.cancel();
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
        String bar = buildProgressBar(progress);
        String message = lang.text("messages.katana.cooldown")
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", seconds));
        showActionBar(player, message);
    }

    private void showActionBar(Player player, String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
    }

    private String buildProgressBar(double progress) {
        int filledSegments = (int) Math.round(progress * COOLDOWN_BAR_SEGMENTS);
        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < COOLDOWN_BAR_SEGMENTS; i++) {
            if (i == filledSegments) {
                bar.append("&7");
            }
            bar.append('|');
        }
        return bar.toString();
    }
}
