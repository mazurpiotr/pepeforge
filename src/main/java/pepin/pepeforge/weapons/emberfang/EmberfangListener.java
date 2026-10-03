package pepin.pepeforge.weapons.emberfang;

import org.bukkit.Sound;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class EmberfangListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final Set<UUID> selfBurningPlayers = ConcurrentHashMap.newKeySet();
    private ScheduledTaskCompat selfBurnTask;

    public EmberfangListener(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void start() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            SchedulerCompat.runForPlayer(player, plugin, () -> {
                refreshInventoryText(player);
                refreshSelfBurnState(player);
            });
        }
        selfBurnTask = SchedulerCompat.runTimer(
                plugin,
                this::tickSelfBurningPlayers,
                EmberfangDefinition.SELF_BURN_INTERVAL_TICKS,
                EmberfangDefinition.SELF_BURN_INTERVAL_TICKS);
    }

    public void stop() {
        if (selfBurnTask != null) {
            selfBurnTask.cancel();
            selfBurnTask = null;
        }
        selfBurningPlayers.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void addFireDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)
                || target == player
                || DamageFlow.isSecondaryDamage(event)
                || !itemFactory.isEmberfang(player.getInventory().getItemInMainHand())) {
            return;
        }

        double fireDamage = getMitigatedFireDamage(target);
        if (fireDamage <= 0.0D) {
            return;
        }

        // Keep the bonus inside the accepted melee event so it shares vanilla i-frames
        // and other plugins observe one damage event instead of a second synthetic hit.
        event.setDamage(event.getDamage() + fireDamage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSuccessfulHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)
                || target == player
                || DamageFlow.isSecondaryDamage(event)
                || !itemFactory.isEmberfang(player.getInventory().getItemInMainHand())) {
            return;
        }

        refreshSelfBurnState(player);
        target.getWorld().spawnParticle(
                org.bukkit.Particle.FLAME,
                target.getLocation().add(0.0D, 0.8D, 0.0D),
                5,
                0.22D,
                0.25D,
                0.22D,
                0.015D);
        target.getWorld().spawnParticle(
                org.bukkit.Particle.LAVA,
                target.getLocation().add(0.0D, 0.8D, 0.0D),
                1,
                0.18D,
                0.2D,
                0.18D,
                0.0D);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.45F, 1.35F);

        if (ThreadLocalRandom.current().nextDouble() < EmberfangDefinition.IGNITE_CHANCE) {
            target.setFireTicks(Math.max(target.getFireTicks(), EmberfangDefinition.IGNITE_DURATION_TICKS));
        }
    }

    @EventHandler
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        selfBurningPlayers.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selfBurningPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void tickSelfBurningPlayers() {
        for (UUID playerId : selfBurningPlayers) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) {
                selfBurningPlayers.remove(playerId);
                continue;
            }
            SchedulerCompat.runForPlayer(player, plugin, () -> {
                if (!refreshSelfBurnState(player)) {
                    return;
                }
                DamageSource damageSource = DamageSource.builder(DamageType.HOT_FLOOR).build();
                player.damage(EmberfangDefinition.SELF_BURN_DAMAGE, damageSource);
            });
        }
    }

    private void scheduleRefresh(Player player) {
        SchedulerCompat.runLater(plugin,
                () -> SchedulerCompat.runForPlayer(player, plugin, () -> {
                    refreshInventoryText(player);
                    refreshSelfBurnState(player);
                }), 1L);
    }

    private void refreshInventoryText(Player player) {
        ItemStack[] storageContents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storageContents.length; slot++) {
            ItemStack item = storageContents[slot];
            if (itemFactory.updateEmberfangText(item)) {
                player.getInventory().setItem(slot, item);
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (itemFactory.updateEmberfangText(offHand)) {
            player.getInventory().setItemInOffHand(offHand);
        }
        ItemStack cursor = player.getItemOnCursor();
        if (itemFactory.updateEmberfangText(cursor)) {
            player.setItemOnCursor(cursor);
        }
    }

    private boolean refreshSelfBurnState(Player player) {
        UUID playerId = player.getUniqueId();
        boolean shouldBurn = itemFactory.isEmberfang(player.getInventory().getItemInMainHand())
                || itemFactory.isEmberfang(player.getInventory().getItemInOffHand());
        if (shouldBurn && !hasFireProtectionArmor(player)) {
            selfBurningPlayers.add(playerId);
            return true;
        }
        selfBurningPlayers.remove(playerId);
        return false;
    }

    private boolean hasFireProtectionArmor(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return false;
        }
        for (ItemStack armorPiece : equipment.getArmorContents()) {
            if (armorPiece != null && armorPiece.containsEnchantment(Enchantment.FIRE_PROTECTION)) {
                return true;
            }
        }
        return false;
    }

    private double getMitigatedFireDamage(LivingEntity target) {
        if (target.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            return 0.0D;
        }

        int fireProtectionLevels = 0;
        EntityEquipment equipment = target.getEquipment();
        if (equipment != null) {
            for (ItemStack armorPiece : equipment.getArmorContents()) {
                if (armorPiece != null) {
                    fireProtectionLevels += armorPiece.getEnchantmentLevel(Enchantment.FIRE_PROTECTION);
                }
            }
        }
        double reduction = Math.min(
                EmberfangDefinition.MAX_FIRE_PROTECTION_REDUCTION,
                fireProtectionLevels * EmberfangDefinition.FIRE_PROTECTION_REDUCTION_PER_LEVEL);
        return itemFactory.getEmberfangFireDamage() * (1.0D - reduction);
    }
}
