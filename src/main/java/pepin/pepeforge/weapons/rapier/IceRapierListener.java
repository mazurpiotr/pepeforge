package pepin.pepeforge.weapons.rapier;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Objects;

public final class IceRapierListener implements Listener {
    private final ItemFactory itemFactory;
    private final IceRapierSettings settings;
    private final Tag<EntityType> immuneEntityTypes;
    private final Tag<Material> immuneWearables;
    private volatile boolean running;

    public IceRapierListener(ItemFactory itemFactory, IceRapierSettings settings) {
        this(itemFactory, settings,
                Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, NamespacedKey.minecraft("freeze_immune_entity_types"), EntityType.class),
                Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft("freeze_immune_wearables"), Material.class));
    }

    IceRapierListener(ItemFactory itemFactory, IceRapierSettings settings,
            Tag<EntityType> immuneEntityTypes, Tag<Material> immuneWearables) {
        this.itemFactory = Objects.requireNonNull(itemFactory);
        this.settings = Objects.requireNonNull(settings);
        this.immuneEntityTypes = Objects.requireNonNull(immuneEntityTypes, "Freeze-immune entity tag");
        this.immuneWearables = Objects.requireNonNull(immuneWearables, "Freeze-immune wearable tag");
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!running || event.isCancelled()
                || (event.getCause() != DamageCause.ENTITY_ATTACK && event.getCause() != DamageCause.ENTITY_SWEEP_ATTACK)
                || DamageFlow.isSecondaryDamage(event)
                || !(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity victim) || attacker == victim
                || !SchedulerCompat.isOwnedByCurrentRegion(attacker)
                || !SchedulerCompat.isOwnedByCurrentRegion(victim)) {
            return;
        }
        double damage = event.getFinalDamage();
        if (!Double.isFinite(damage) || damage <= 0.0D || !attacker.isOnline() || !attacker.isValid()
                || attacker.isDead() || !victim.isValid() || victim.isDead()
                || attacker.getWorld() != victim.getWorld()) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (itemFactory.getRapierTier(weapon) != RapierTier.ICE
                || (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK
                        && itemFactory.getRapierMode(weapon) != RapierMode.CUT)
                || isImmune(victim)) {
            return;
        }
        int currentTicks = victim.getFreezeTicks();
        if (currentTicks < settings.freezeTicks()) {
            victim.setFreezeTicks(settings.freezeTicks());
        }
    }

    private boolean isImmune(LivingEntity victim) {
        if (victim.getType() == EntityType.SKELETON || immuneEntityTypes.isTagged(victim.getType())
                || victim instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        EntityEquipment equipment = victim.getEquipment();
        if (equipment != null) {
            for (ItemStack armor : equipment.getArmorContents()) {
                if (isImmuneWearable(armor)) {
                    return true;
                }
            }
        }
        return victim instanceof Horse horse && isImmuneWearable(horse.getInventory().getArmor());
    }

    private boolean isImmuneWearable(ItemStack item) {
        return item != null && immuneWearables.isTagged(item.getType());
    }
}
