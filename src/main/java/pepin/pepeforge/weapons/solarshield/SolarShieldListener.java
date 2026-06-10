package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pepin.pepeforge.item.ItemFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SolarShieldListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    private final Map<UUID, ItemStack> cachedShields = new HashMap<>();
    private final Map<UUID, Integer> activeTicks = new HashMap<>();
    private BukkitTask statusTask;

    public SolarShieldListener(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void startStatusTask() {
        statusTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                ItemStack offHand = player.getInventory().getItemInOffHand();

                ItemStack activeShield = null;
                if (itemFactory.isSolarShield(offHand)) {
                    activeShield = offHand;
                } else if (itemFactory.isSolarShield(mainHand)) {
                    activeShield = mainHand;
                }

                ItemStack previousShield = cachedShields.get(playerId);

                if (previousShield != null && previousShield != activeShield) {
                    if (itemFactory.isSolarShield(previousShield)) {
                        itemFactory.updateSolarShieldVisuals(previousShield, 0);
                    }
                    cachedShields.remove(playerId);
                    activeTicks.remove(playerId);
                }

                if (activeShield != null) {
                    cachedShields.put(playerId, activeShield);
                    
                    int charges = getCharges(activeShield);
                    int currentTicks = activeTicks.getOrDefault(playerId, 0);

                    if (SolarPower.isSunlit(player)) {
                        if (charges < SolarShieldDefinition.MAX_CHARGES) {
                            if (currentTicks < 0) {
                                currentTicks = 0;
                            }
                            currentTicks += 10;
                            if (currentTicks >= SolarShieldDefinition.CHARGE_TICKS) {
                                currentTicks = 0;
                                itemFactory.updateSolarShieldVisuals(activeShield, charges + 1);
                                player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                            }
                            activeTicks.put(playerId, currentTicks);
                        }
                    } else {
                        if (charges > 0) {
                            if (currentTicks > 0) {
                                currentTicks = 0;
                            }
                            currentTicks -= 10;
                            if (currentTicks <= -SolarShieldDefinition.DISCHARGE_TICKS) {
                                currentTicks = 0;
                                itemFactory.updateSolarShieldVisuals(activeShield, charges - 1);
                            }
                            activeTicks.put(playerId, currentTicks);
                        }
                    }
                }
            }
        }, 10L, 10L);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        cachedShields.clear();
        activeTicks.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.isBlocking()) {
            return;
        }

        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        ItemStack activeShield = null;
        if (itemFactory.isSolarShield(offHand)) {
            activeShield = offHand;
        } else if (itemFactory.isSolarShield(mainHand)) {
            activeShield = mainHand;
        }

        if (activeShield == null) {
            return;
        }

        int charges = getCharges(activeShield);
        if (charges > 0) {
            itemFactory.updateSolarShieldVisuals(activeShield, charges - 1);
            activeTicks.put(player.getUniqueId(), 0);
            
            attacker.setFireTicks(80);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            
            player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        }
    }

    private int getCharges(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer charges = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, SolarShieldDefinition.CHARGES_KEY_STRING),
                PersistentDataType.INTEGER
        );
        return charges == null ? 0 : charges;
    }
}
