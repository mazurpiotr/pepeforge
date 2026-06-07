package pepin.pepeforge.weapons.windblade;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.AuraManager;
import pepin.pepeforge.util.CooldownManager;
import pepin.pepeforge.weapons.windblade.WindAuraEffect;

import java.util.Locale;

public final class WindBladeListener implements Listener {

    private static final int HOLDING_SPEED_DURATION_TICKS = 200;
    private static final int COOLDOWN_BAR_SEGMENTS = 20;
    private static final String DASH_COOLDOWN_KEY = "wind_blade:dash";
    private static final long DASH_COOLDOWN_MILLIS = 5_000L;
    private static final double DASH_STRENGTH = 1.5D;
    private static final double DASH_LIFT = 0.3D;
    private static final PotionEffect HOLDING_SPEED_EFFECT = new PotionEffect(
            PotionEffectType.SPEED,
            HOLDING_SPEED_DURATION_TICKS,
            0,
            true,
            false,
            true
    );

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final CooldownManager cooldownManager;
    private final AuraManager auraManager;

    public WindBladeListener(
            JavaPlugin plugin,
            ItemFactory itemFactory,
            PluginLang lang,
            CooldownManager cooldownManager,
            AuraManager auraManager
    ) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.cooldownManager = cooldownManager;
        this.auraManager = auraManager;
    }

    private org.bukkit.scheduler.BukkitTask holdingTask;

    public void startHoldingTask() {
        holdingTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                ItemStack held = player.getInventory().getItemInMainHand();
                WindBladeTier tier = itemFactory.getWindBladeTier(held);
                if (tier == null || !tier.grantsHoldingSpeed()) {
                    continue;
                }
                applySpeedEffectIfBetter(player, HOLDING_SPEED_EFFECT);
            }
        }, 1L, 20L);
    }

    public void stop() {
        if (holdingTask != null) {
            holdingTask.cancel();
            holdingTask = null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
        if (itemFactory.getWindBladeTier(mainHandItem) == null) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType().isInteractable()
                && !player.isSneaking()) {
            return;
        }

        denyInteraction(event);

        long remainingMillis = cooldownManager.getRemainingCooldownMillis(player, DASH_COOLDOWN_KEY);
        if (remainingMillis > 0L) {
            showCooldownActionBar(player, remainingMillis);
            return;
        }

        cooldownManager.setCooldown(player, DASH_COOLDOWN_KEY, DASH_COOLDOWN_MILLIS);
        dash(player);
        player.setCooldown(mainHandItem.getType(), 10);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Entity target = event.getEntity();
        if (target == player) {
            return;
        }

        WindBladeTier tier = itemFactory.getWindBladeTier(player.getInventory().getItemInMainHand());
        if (tier == null || tier.hitSpeedDurationTicks() <= 0) {
            return;
        }

        applySpeedEffectIfBetter(player, new PotionEffect(
                PotionEffectType.SPEED,
                tier.hitSpeedDurationTicks(),
                tier.hitSpeedAmplifier(),
                true,
                false,
                true
        ));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownManager.clearCooldown(event.getPlayer(), DASH_COOLDOWN_KEY);
        auraManager.clearPlayer(event.getPlayer());
    }

    private void dash(Player player) {
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);

        if (direction.lengthSquared() > 0.001D) {
            direction.normalize();
        } else {
            direction = player.getEyeLocation().getDirection().normalize();
        }

        player.setVelocity(direction.multiply(DASH_STRENGTH).setY(DASH_LIFT));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0.0D, 1.0D, 0.0D), 3, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private void applySpeedEffectIfBetter(Player player, PotionEffect candidate) {
        // Wind Blade speed should feel persistent without stomping on stronger
        // potion effects or repeatedly refreshing an equal-or-better buff.
        PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
        if (current != null) {
            if (current.getAmplifier() > candidate.getAmplifier()) {
                return;
            }
            if (current.getAmplifier() == candidate.getAmplifier()
                    && current.getDuration() >= candidate.getDuration()) {
                return;
            }
        }
        player.addPotionEffect(candidate);
        auraManager.addOrExtendActiveAura(player, new WindAuraEffect(itemFactory), candidate.getDuration());
    }

    private void denyInteraction(PlayerInteractEvent event) {
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void showCooldownActionBar(Player player, long remainingMillis) {
        double seconds = remainingMillis / 1000.0D;
        double progress = Math.max(0.0D, Math.min(1.0D, 1.0D - ((double) remainingMillis / DASH_COOLDOWN_MILLIS)));
        String bar = buildProgressBar(progress);
        String message = lang.text("messages.wind_blade.cooldown")
                .replace("{bar}", bar)
                .replace("{seconds}", String.format(Locale.US, "%.1f", seconds));
        showActionBar(player, message);
    }

    private void showActionBar(Player player, String message) {
        player.sendActionBar(
            LegacyComponentSerializer.legacyAmpersand().deserialize(message)
        );
    }

    private String buildProgressBar(double progress) {
        int filled = (int) Math.round(progress * COOLDOWN_BAR_SEGMENTS);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < COOLDOWN_BAR_SEGMENTS; i++) {
            bar.append(i < filled ? "&a|" : "&8|");
        }

        return bar.toString();
    }
}
