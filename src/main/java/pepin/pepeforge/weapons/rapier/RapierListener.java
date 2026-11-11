package pepin.pepeforge.weapons.rapier;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.weapons.rapier.compat.RapierAttackCharge;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;
import pepin.pepeforge.util.ui.ActionBarHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RapierListener implements Listener {
    private final ItemFactory itemFactory;
    private final PluginLang lang;
    private final RapierSettings settings;
    private final RapierAttackCharge attackCharge;
    private final Map<UUID, Integer> switchTicks = new ConcurrentHashMap<>();
    private volatile boolean running;

    public RapierListener(ItemFactory itemFactory, PluginLang lang, RapierSettings settings) {
        this(itemFactory, lang, settings, RapierAttackCharge.bukkit());
    }

    public RapierListener(ItemFactory itemFactory, PluginLang lang, RapierSettings settings,
            RapierAttackCharge attackCharge) {
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.settings = settings;
        this.attackCharge = attackCharge;
    }

    public synchronized void start() {
        running = true;
    }

    public synchronized void stop() {
        running = false;
        switchTicks.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public synchronized void onRightClick(PlayerInteractEvent event) {
        if (!running || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player player = event.getPlayer();
        if (!SchedulerCompat.isOwnedByCurrentRegion(player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int tick = player.getTicksLived();
        boolean switchedThisTick = Integer.valueOf(tick).equals(switchTicks.get(playerId));
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            if (switchedThisTick) {
                denyInteraction(event);
            }
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !player.isSneaking()) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (itemFactory.getRapierTier(item) == null) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY
                || (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.isCancelled())) {
            return;
        }
        if (!switchedThisTick) {
            RapierMode mode = itemFactory.getRapierMode(item).next();
            itemFactory.updateRapierMode(item, mode);
            player.getInventory().setItemInMainHand(item);
            switchTicks.put(playerId, tick);
            ActionBarHelper.showActionBar(player, lang.text("messages.rapier.mode_changed")
                    .replace("{mode}", lang.text("messages.rapier.modes." + mode.modeKey())));
        }
        denyInteraction(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!running || event.isCancelled() || !(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity victim)
                || !SchedulerCompat.isOwnedByCurrentRegion(attacker)
                || DamageFlow.isSecondaryDamage(event)) {
            return;
        }
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (itemFactory.getRapierTier(item) == null || itemFactory.getRapierMode(item) != RapierMode.THRUST) {
            return;
        }
        if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
        }
        if (!settings.frontalBonusEnabled() || event.getCause() != DamageCause.ENTITY_ATTACK
                || !SchedulerCompat.isOwnedByCurrentRegion(victim)
                || !(attackCharge.consume(attacker, victim) >= 0.9F) || !attacker.isOnline() || attacker.isDead()
                || !victim.isValid() || victim.isDead() || attacker.getWorld() != victim.getWorld()
                || !isInFrontalCone(victim.getLocation(), attacker.getLocation(), settings.frontalConeDegrees())) {
            return;
        }
        double damage = event.getDamage();
        double boostedDamage = damage * (1.0D + settings.frontalBonusPercent() / 100.0D);
        if (Double.isFinite(boostedDamage) && damage > 0.0D) {
            event.setDamage(boostedDamage);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        switchTicks.remove(event.getPlayer().getUniqueId());
        attackCharge.clear(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        switchTicks.remove(event.getEntity().getUniqueId());
        attackCharge.clear(event.getEntity());
    }

    static boolean isInFrontalCone(Location victim, Location attacker, double coneDegrees) {
        double dx = attacker.getX() - victim.getX();
        double dz = attacker.getZ() - victim.getZ();
        double distanceSquared = dx * dx + dz * dz;
        if (distanceSquared < 1.0E-12D || !Double.isFinite(distanceSquared)) {
            return false;
        }
        double yaw = Math.toRadians(victim.getYaw());
        double dot = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / Math.sqrt(distanceSquared);
        return dot + 1.0E-12D >= Math.cos(Math.toRadians(coneDegrees / 2.0D));
    }

    private static void denyInteraction(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
    }
}
