package pepin.pepeforge.weapons.throwingknife;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.util.protection.ProtectionUtil;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ThrowingKnifeListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final NamespacedKey projectileKey;
    private final Set<ItemDisplay> activeDisplays = ConcurrentHashMap.newKeySet();

    public ThrowingKnifeListener(JavaPlugin plugin, ItemFactory itemFactory, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.cooldownManager = cooldownManager;
        this.projectileKey = new NamespacedKey(plugin, "throwing_knife_projectile");
    }

    public void cleanup() {
        for (ItemDisplay display : activeDisplays) {
            if (display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!itemFactory.isThrowingKnife(item)) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Allow opening chests, doors, crafting tables, etc. if player is not sneaking
            if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable() && !player.isSneaking()) {
                return;
            }
            event.setCancelled(true);
            throwKnife(player, item);
        }
    }

    private void throwKnife(Player player, ItemStack item) {
        if (cooldownManager.isOnCooldown(player, "throwing_knife")) {
            return;
        }

        if (!itemFactory.isItemEnabled(ThrowingKnifeDefinition.ITEM_ID)) {
            return;
        }

        cooldownManager.setCooldown(player, "throwing_knife", ThrowingKnifeDefinition.COOLDOWN_MILLIS);

        player.swingMainHand();

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        Location eyeLoc = player.getEyeLocation();

        Snowball snowball = player.getWorld().spawn(eyeLoc, Snowball.class, sb -> {
            sb.setShooter(player);
            sb.setRotation(eyeLoc.getYaw(), eyeLoc.getPitch());
            sb.setItem(new ItemStack(Material.AIR));
            sb.setVelocity(eyeLoc.getDirection().multiply(1.6D));
            sb.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        });

        ItemStack visualItem = itemFactory.createThrowingKnife();
        visualItem.setAmount(1);

        Location spawnLoc = snowball.getLocation();
        spawnLoc.setYaw(eyeLoc.getYaw());
        spawnLoc.setPitch(eyeLoc.getPitch());

        // Spawn non-billboard ItemDisplay to act as custom projectile model
        ItemDisplay itemDisplay = snowball.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(visualItem);
            display.setBillboard(Display.Billboard.FIXED);
            display.setGravity(false);
            display.setPersistent(false);

            Transformation trans = display.getTransformation();
            // Offset vertically to center item inside the snowball passenger seat
            trans.getTranslation().set(0f, -0.2f, 0f);
            // Rotate local Yaw, Pitch, and Roll based on ThrowingKnifeDefinition configuration
            trans.getLeftRotation()
                    .identity()
                    .rotateY((float) Math.toRadians(ThrowingKnifeDefinition.ROTATION_YAW))
                    .rotateX((float) Math.toRadians(ThrowingKnifeDefinition.ROTATION_PITCH))
                    .rotateZ((float) Math.toRadians(ThrowingKnifeDefinition.ROTATION_ROLL));
            display.setTransformation(trans);
        });

        snowball.addPassenger(itemDisplay);
        activeDisplays.add(itemDisplay);

        // Schedule timeout cleanup in case entity despawns abnormally
        SchedulerCompat.runLaterForEntity(itemDisplay, plugin, () -> {
            if (itemDisplay.isValid()) {
                itemDisplay.remove();
            }
            activeDisplays.remove(itemDisplay);
        }, 100L);

        player.sendEquipmentChange(player, EquipmentSlot.HAND, new ItemStack(Material.AIR));

        SchedulerCompat.runLaterForPlayer(player, plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            ItemStack currentInHand = player.getInventory().getItemInMainHand();
            player.sendEquipmentChange(player, EquipmentSlot.HAND, currentInHand);
        }, 3L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        if (!snowball.getPersistentDataContainer().has(projectileKey, PersistentDataType.BYTE)) {
            return;
        }

        // Clean up visual display passengers
        snowball.getPassengers().forEach(passenger -> {
            passenger.remove();
            activeDisplays.remove(passenger);
        });

        if (event.getHitEntity() != null) {
            if (event.getHitEntity() instanceof LivingEntity target) {
                if (snowball.getShooter() instanceof Player shooter) {
                    if (ProtectionUtil.canDamage(shooter, target)) {
                        target.damage(ThrowingKnifeDefinition.DAMAGE, snowball);
                        target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                    }
                }
            }
        } else if (event.getHitBlock() != null) {
            Location hitLoc = snowball.getLocation();

            snowball.getWorld().playSound(hitLoc, Sound.BLOCK_METAL_HIT, 0.5f, 1.8f);
            snowball.getWorld().dropItemNaturally(hitLoc, itemFactory.createThrowingKnife());
        }
    }

}
