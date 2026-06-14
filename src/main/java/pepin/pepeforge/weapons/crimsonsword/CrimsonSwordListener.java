package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.AuraManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CrimsonSwordListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final CrimsonSwordManager manager;
    private final AuraManager auraManager;
    private final Map<UUID, Double> auraDamageProgress = new HashMap<>();
    private final Set<UUID> auraDrainingPlayers = new HashSet<>();

    public CrimsonSwordListener(JavaPlugin plugin, ItemFactory itemFactory, CrimsonSwordManager manager, AuraManager auraManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.manager = manager;
        this.auraManager = auraManager;
    }

    public CrimsonSwordManager getManager() {
        return manager;
    }

    public void stop() {
        auraDamageProgress.clear();
        auraDrainingPlayers.clear();
    }

    public boolean isHoldingCrimsonSword(Player player) {
        return itemFactory.isCrimsonSword(player.getInventory().getItemInMainHand());
    }

    public void setAuraDraining(UUID playerId, boolean draining) {
        if (draining) {
            auraDrainingPlayers.add(playerId);
        } else {
            auraDrainingPlayers.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (auraDrainingPlayers.contains(player.getUniqueId())) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target) || target == player) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!itemFactory.isCrimsonSword(weapon)) {
            return;
        }

        int level = manager.getLevel(weapon);
        if (level <= 0) {
            return;
        }

        event.setDamage(event.getDamage() * (1.0D + damageBonus(level)));
        double finalDamage = event.getFinalDamage();
        double effectiveDamage = Math.min(finalDamage, target.getHealth());
        
        // This is handled in CrimsonAuraEffect for drain, but lifesteal on hit is here
        if (manager.lifesteal(level) > 0.0D) {
            manager.heal(player, effectiveDamage * manager.lifesteal(level));
        }

        manager.addXp(player, weapon, effectiveDamage);
        addAuraDamageProgress(player, level, effectiveDamage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!itemFactory.isCrimsonSword(weapon)) {
            return;
        }

        int level = manager.getLevel(weapon);
        if (level <= 0) {
            return;
        }

        playLegacyBurst(event.getEntity());
    }

    private void addAuraDamageProgress(Player player, int level, double damage) {
        if (level < 10 || damage <= 0.0D) {
            return;
        }

        UUID playerId = player.getUniqueId();
        double progress = auraDamageProgress.getOrDefault(playerId, 0.0D) + damage;
        while (progress >= CrimsonSwordDefinition.AURA_TRIGGER_DAMAGE) {
            progress -= CrimsonSwordDefinition.AURA_TRIGGER_DAMAGE;
            activateAura(player, level);
        }
        auraDamageProgress.put(playerId, progress);
    }

    private void activateAura(Player player, int level) {
        int durationTicks = manager.auraDurationTicks(level);
        auraManager.addOrExtendActiveAura(player, new CrimsonAuraEffect(this, level, durationTicks, player.getTicksLived()), durationTicks);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SKELETON_HURT, 0.45f, 0.55f);
    }

    private double damageBonus(int level) {
        return Math.min(level, CrimsonSwordDefinition.MAX_LEVEL) * CrimsonSwordDefinition.DAMAGE_BONUS_PER_LEVEL;
    }

    private void playLegacyBurst(LivingEntity victim) {
        org.bukkit.Location location = victim.getLocation().add(0.0D, 1.0D, 0.0D);
        victim.getWorld().spawnParticle(
                org.bukkit.Particle.DUST,
                location,
                35,
                0.55D,
                0.45D,
                0.55D,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 18), 1.25f)
        );
        victim.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, location, 8, 0.35D, 0.25D, 0.35D, 0.02D);
    }
}
