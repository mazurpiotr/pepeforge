package pepin.pepeforge.listeners;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.items.crimson.CrimsonSwordManager;

public class CrimsonSwordListener implements Listener {
    private final CrimsonSwordManager manager;

    public CrimsonSwordListener(CrimsonSwordManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (manager.getLevel(item) > 0) {
            manager.addXp(item, event.getFinalDamage());
            
            // Handle Lifesteal if level 10+ and Aura is active
            // Logic to be implemented in Step 4 (Aura System)
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        int level = manager.getLevel(item);
        if (level <= 0) return;

        LivingEntity victim = event.getEntity();

        // Level 1+: Crimson Legacy
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 
            20, 0.5, 0.5, 0.5, new Particle.DustOptions(org.bukkit.Color.RED, 1));

        // Level 5+: Crimson Spark
        if (level >= 5) {
            double health = Math.min(killer.getMaxHealth(), killer.getHealth() + 2.0);
            killer.setHealth(health);
        }

        // Level 10+: Activate Aura
        if (level >= 10) {
            // manager.activateAura(killer, level);
        }
    }
}