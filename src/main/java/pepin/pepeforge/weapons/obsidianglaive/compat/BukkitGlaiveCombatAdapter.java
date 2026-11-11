package pepin.pepeforge.weapons.obsidianglaive.compat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

public class BukkitGlaiveCombatAdapter implements GlaiveCombatAdapter {
    static final Attack DENIED = new Attack() {
        @Override
        public boolean sweepAllowed() {
            return false;
        }

        @Override
        public double sweepDamage(LivingEntity secondary) {
            return 0.0D;
        }
    };

    protected final ItemFactory factory;
    protected final NativeGlaiveCombat nativeCombat;
    protected volatile boolean running = true;
    private final Map<UUID, Movement> movements = new ConcurrentHashMap<>();

    public BukkitGlaiveCombatAdapter(JavaPlugin plugin, ItemFactory factory, NativeGlaiveCombat nativeCombat) {
        this(plugin, factory, nativeCombat, true);
    }

    protected BukkitGlaiveCombatAdapter(JavaPlugin plugin, ItemFactory factory,
            NativeGlaiveCombat nativeCombat, boolean bukkitChargeTiming) {
        this.factory = factory;
        this.nativeCombat = nativeCombat;
        if (bukkitChargeTiming) {
            plugin.getLogger().warning("Obsidian Glaive on Bukkit-only servers reads attack charge during damage "
                    + "dispatch; verify cooldown timing on the installed Spigot build.");
        }
    }

    @Override
    public Attack capture(Player player, LivingEntity primary) {
        if (!canCapture(player, primary)) {
            return DENIED;
        }
        return snapshot(player, primary, false);
    }

    protected boolean canCapture(Player player, LivingEntity primary) {
        return running && SchedulerCompat.isOwnedByCurrentRegion(player)
                && SchedulerCompat.isOwnedByCurrentRegion(primary)
                && player.isOnline() && primary.isValid() && !primary.isDead()
                && primary.getWorld() == player.getWorld()
                && factory.isObsidianGlaive(player.getInventory().getItemInMainHand());
    }

    protected Attack snapshot(Player player, LivingEntity primary, boolean nativeCharge) {
        if (nativeCombat != null) {
            return nativeCombat.capture(player, primary, nativeCharge);
        }
        float strength = player.getAttackCooldown();
        Movement movement = movements.get(player.getUniqueId());
        double horizontalMovement = movement != null && movement.tick() == player.getTicksLived()
                ? movement.horizontalSquared() : 0.0D;
        double speed = NativeGlaiveCombat.attribute(player, Attribute.MOVEMENT_SPEED, 0.1D) * 2.5D;
        boolean allowed = Float.isFinite(strength) && strength > 0.9F && !player.isSprinting()
                && player.isOnGround() && horizontalMovement < speed * speed;
        double attackDamage = NativeGlaiveCombat.attribute(player, Attribute.ATTACK_DAMAGE, 1.0D);
        double baseDamage = attackDamage * (0.2D + 0.8D * strength * strength);
        boolean critical = Float.isFinite(strength) && strength > 0.9F && player.getFallDistance() > 0.0F
                && !player.isOnGround() && !player.isClimbing() && !player.isInWater()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS) && !player.isInsideVehicle()
                && !player.isSprinting();
        double sweepBase = 1.0D + NativeGlaiveCombat.attribute(player, Attribute.SWEEPING_DAMAGE_RATIO, 0.0D)
                * baseDamage;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Map<Enchantment, Integer> enchantments = Map.copyOf(weapon.getEnchantments());
        boolean valid = allowed && Double.isFinite(sweepBase) && sweepBase > 0.0D;
        boolean validStab = Float.isFinite(strength) && strength >= 0.0F
                && Double.isFinite(attackDamage) && Double.isFinite(baseDamage);
        org.bukkit.World world = player.getWorld();
        return new Attack() {
            @Override
            public boolean sweepAllowed() {
                return valid;
            }

            @Override
            public double sweepDamage(LivingEntity secondary) {
                if (!valid || !SchedulerCompat.isOwnedByCurrentRegion(player)
                        || !SchedulerCompat.isOwnedByCurrentRegion(secondary)
                        || !secondary.isValid() || secondary.isDead()
                        || player.getWorld() != world || secondary.getWorld() != world) {
                    return 0.0D;
                }
                double damage = (sweepBase + standardEnchantmentBonus(enchantments, secondary.getType())) * strength;
                return Double.isFinite(damage) && damage > 0.0D ? damage : 0.0D;
            }

            @Override
            public double stabDamage(LivingEntity secondary) {
                if (!validStab || !SchedulerCompat.isOwnedByCurrentRegion(player)
                        || !SchedulerCompat.isOwnedByCurrentRegion(secondary)
                        || !secondary.isValid() || secondary.isDead()
                        || player.getWorld() != world || secondary.getWorld() != world) {
                    return 0.0D;
                }
                double damage = baseDamage * (critical ? 1.5D : 1.0D)
                        + strength * standardEnchantmentBonus(enchantments, secondary.getType());
                return Double.isFinite(damage) && damage > 0.0D ? damage : 0.0D;
            }
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!running || nativeCombat != null || event.getTo() == null
                || event.getFrom().getWorld() != event.getTo().getWorld()) {
            return;
        }
        if (!factory.isObsidianGlaive(event.getPlayer().getInventory().getItemInMainHand())) {
            movements.remove(event.getPlayer().getUniqueId());
            return;
        }
        double x = event.getTo().getX() - event.getFrom().getX();
        double z = event.getTo().getZ() - event.getFrom().getZ();
        movements.put(event.getPlayer().getUniqueId(),
                new Movement(event.getPlayer().getTicksLived(), x * x + z * z));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    @Override
    public void clear(Player player) {
        movements.remove(player.getUniqueId());
    }

    @Override
    public void stop() {
        running = false;
        movements.clear();
    }

    private static double standardEnchantmentBonus(Map<Enchantment, Integer> enchantments, EntityType type) {
        int sharpness = enchantments.getOrDefault(Enchantment.SHARPNESS, 0);
        double damage = sharpness > 0 ? 0.5D * sharpness + 0.5D : 0.0D;
        boolean undead = switch (type) {
            case ZOMBIE, HUSK, DROWNED, ZOMBIFIED_PIGLIN, ZOMBIE_VILLAGER, ZOMBIE_HORSE,
                    SKELETON_HORSE, SKELETON, STRAY, BOGGED, WITHER_SKELETON, WITHER, PHANTOM,
                    CAMEL_HUSK, ZOMBIE_NAUTILUS -> true;
            default -> false;
        };
        if (undead) {
            damage += 2.5D * enchantments.getOrDefault(Enchantment.SMITE, 0);
        }
        boolean arthropod = switch (type) {
            case SPIDER, CAVE_SPIDER, SILVERFISH, ENDERMITE, BEE -> true;
            default -> false;
        };
        if (arthropod) {
            damage += 2.5D * enchantments.getOrDefault(Enchantment.BANE_OF_ARTHROPODS, 0);
        }
        return damage;
    }

    private record Movement(int tick, double horizontalSquared) {
    }
}
