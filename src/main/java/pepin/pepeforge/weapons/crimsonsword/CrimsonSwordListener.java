package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pepin.pepeforge.item.ItemFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CrimsonSwordListener implements Listener {

    private static final long TICK_MILLIS = 50L;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final CrimsonSwordManager manager;
    private final Map<UUID, AuraState> crimsonAuras = new HashMap<>();
    private BukkitTask auraTask;

    public CrimsonSwordListener(JavaPlugin plugin, ItemFactory itemFactory, CrimsonSwordManager manager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.manager = manager;
    }

    public void startAuraTask() {
        if (auraTask != null) {
            return;
        }

        auraTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, AuraState>> iterator = crimsonAuras.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, AuraState> entry = iterator.next();
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player == null || !player.isOnline() || entry.getValue().expiresAtMillis() <= now) {
                    iterator.remove();
                    continue;
                }
                if (!itemFactory.isCrimsonSword(player.getInventory().getItemInMainHand())) {
                    continue;
                }
                playCrimsonAura(player);
            }
        }, 1L, 2L);
    }

    public void stop() {
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
        crimsonAuras.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() == player) {
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

        AuraState aura = activeAura(player);
        if (aura != null) {
            double multiplier = 1.0D + damageBonus(level) + chainDamageBonus(aura);
            event.setDamage(event.getDamage() * multiplier);
            heal(player, event.getFinalDamage() * lifesteal(level));
        }

        manager.addXp(player, weapon, event.getFinalDamage());
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
        if (level >= 5) {
            heal(killer, level >= 15 && activeAura(killer) != null ? 4.0D : 2.0D);
        }
        if (level >= 10) {
            activateAura(killer, level);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        crimsonAuras.remove(event.getPlayer().getUniqueId());
    }

    private void activateAura(Player player, int level) {
        int durationTicks = auraDurationTicks(level);
        AuraState currentState = crimsonAuras.get(player.getUniqueId());
        int stacks = currentState == null ? 0 : currentState.chainStacks();
        if (level >= 25) {
            stacks = Math.min(CrimsonSwordDefinition.CHAIN_MAX_STACKS, stacks + 1);
        }

        crimsonAuras.put(
                player.getUniqueId(),
                new AuraState(System.currentTimeMillis() + (durationTicks * TICK_MILLIS), stacks)
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_HURT, 0.45f, 0.55f);
    }

    private AuraState activeAura(Player player) {
        AuraState state = crimsonAuras.get(player.getUniqueId());
        if (state == null || state.expiresAtMillis() <= System.currentTimeMillis()) {
            crimsonAuras.remove(player.getUniqueId());
            return null;
        }
        return state;
    }

    private double damageBonus(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_DAMAGE_BONUS;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_DAMAGE_BONUS;
        }
        if (level >= 10) {
            return CrimsonSwordDefinition.LEVEL_10_DAMAGE_BONUS;
        }
        return 0.0D;
    }

    private double lifesteal(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_LIFESTEAL;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_LIFESTEAL;
        }
        if (level >= 10) {
            return CrimsonSwordDefinition.LEVEL_10_LIFESTEAL;
        }
        return 0.0D;
    }

    private int auraDurationTicks(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_AURA_TICKS;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_AURA_TICKS;
        }
        return CrimsonSwordDefinition.LEVEL_10_AURA_TICKS;
    }

    private double chainDamageBonus(AuraState aura) {
        return aura.chainStacks() * CrimsonSwordDefinition.CHAIN_DAMAGE_BONUS_PER_STACK;
    }

    private void heal(Player player, double amount) {
        if (amount <= 0.0D || player.isDead()) {
            return;
        }

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();
        player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
    }

    private void playLegacyBurst(LivingEntity victim) {
        Location location = victim.getLocation().add(0.0D, 1.0D, 0.0D);
        victim.getWorld().spawnParticle(
                Particle.DUST,
                location,
                35,
                0.55D,
                0.45D,
                0.55D,
                new Particle.DustOptions(Color.fromRGB(150, 0, 18), 1.25f)
        );
        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, location, 8, 0.35D, 0.25D, 0.35D, 0.02D);
    }

    private void playCrimsonAura(Player player) {
        Location base = player.getLocation();
        long time = player.getTicksLived();
        for (int i = 0; i < 5; i++) {
            double angle = (time * 0.22D) + (i * 1.35D);
            double radius = 0.35D + ((time + i) % 7) * 0.035D;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle * 0.85D) * radius;
            double y = 0.15D + (i * 0.18D) + Math.sin((time + i) * 0.2D) * 0.08D;

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    base.clone().add(x, y, z),
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    new Particle.DustOptions(Color.fromRGB(185, 8, 24), 0.8f)
            );
            player.getWorld().spawnParticle(
                    Particle.ENTITY_EFFECT,
                    base.clone().add(x * 0.55D, y + 0.05D, z * 0.55D),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    Color.fromRGB(105, 0, 12)
            );
        }
    }

    private record AuraState(long expiresAtMillis, int chainStacks) {
    }
}
