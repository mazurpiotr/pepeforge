package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Item;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.SchedulerCompat;
import pepin.pepeforge.util.ScheduledTaskCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SolarShieldListener implements Listener {

    private static final int CHARGE_BAR_SEGMENTS = 20;
    private static final int OFF_HAND_INVENTORY_SLOT = 40;

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final PluginLang lang;

    private final Map<UUID, Double> activeProgress = new HashMap<>();
    private final Map<UUID, Integer> passiveTicks = new HashMap<>();
    
    private final Set<Item> droppedShields = new HashSet<>();
    private final Map<UUID, Double> droppedShieldProgress = new HashMap<>();
    
    private ScheduledTaskCompat statusTask;
    private final pepin.pepeforge.util.ui.BossBarManager bossBarManager;

    public SolarShieldListener(JavaPlugin plugin, ItemFactory itemFactory, PluginLang lang, pepin.pepeforge.util.ui.BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.lang = lang;
        this.bossBarManager = bossBarManager;
    }

    public void startStatusTask() {
        // Run every 2 ticks for smooth UI
        statusTask = SchedulerCompat.runTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                
                int pTicks = passiveTicks.getOrDefault(playerId, 0) + 2;
                boolean doPassiveDischarge = false;
                if (pTicks >= SolarShieldDefinition.DISCHARGE_TICKS) {
                    pTicks = 0;
                    doPassiveDischarge = true;
                }
                passiveTicks.put(playerId, pTicks);

                PlayerInventory inv = player.getInventory();
                ItemStack mainHand = inv.getItemInMainHand();
                ItemStack offHand = inv.getItemInOffHand();

                ItemStack activeShield = null;
                boolean isMainHand = false;
                if (itemFactory.isSolarShield(offHand)) {
                    activeShield = offHand;
                } else if (itemFactory.isSolarShield(mainHand)) {
                    activeShield = mainHand;
                    isMainHand = true;
                }

                // Slowly discharge ALL other solar shields in inventory when not equipped
                if (doPassiveDischarge) {
                    for (int i = 0; i < inv.getSize(); i++) {
                        if (i == inv.getHeldItemSlot() || i == OFF_HAND_INVENTORY_SLOT) {
                            continue;
                        }
                        ItemStack item = inv.getItem(i);
                        if (itemFactory.isSolarShield(item)) {
                            int currentCharges = getCharges(item);
                            if (currentCharges > 0) {
                                itemFactory.updateSolarShieldVisuals(item, currentCharges - 1);
                                inv.setItem(i, item);
                            }
                        }
                    }
                }

                if (activeShield != null) {
                    int charges = getCharges(activeShield);
                    double progress = activeProgress.getOrDefault(playerId, 0.0);

                    if (SolarPower.isSunlit(player)) {
                        if (charges < SolarShieldDefinition.MAX_CHARGES) {
                            progress += 2.0 / SolarShieldDefinition.CHARGE_TICKS;
                            
                            if (progress >= 1.0) {
                                progress -= 1.0;
                                int newCharges = charges + 1;
                                itemFactory.updateSolarShieldVisuals(activeShield, newCharges);
                                if (isMainHand) {
                                    inv.setItemInMainHand(activeShield);
                                } else {
                                    inv.setItemInOffHand(activeShield);
                                }
                                charges = newCharges;
                                player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                            }
                            activeProgress.put(playerId, progress);
                            
                            if (charges >= SolarShieldDefinition.MAX_CHARGES) {
                                showReadyBossBar(player);
                            } else {
                                updateBossBar(player, progress, charges, true);
                            }
                        } else {
                            if (progress < SolarShieldDefinition.OVERCHARGE_BUFFER) {
                                progress += 2.0 / SolarShieldDefinition.CHARGE_TICKS;
                                progress = Math.min(progress, SolarShieldDefinition.OVERCHARGE_BUFFER);
                                activeProgress.put(playerId, progress);
                            }
                            showReadyBossBar(player);
                        }
                    } else {
                        if (charges > 0 || progress > 0.0) {
                            if (progress <= 0.0 && charges > 0) {
                                int newCharges = charges - 1;
                                itemFactory.updateSolarShieldVisuals(activeShield, newCharges);
                                if (isMainHand) {
                                    inv.setItemInMainHand(activeShield);
                                } else {
                                    inv.setItemInOffHand(activeShield);
                                }
                                charges = newCharges;
                                progress = 1.0;
                            }
                            
                            if (progress > 0.0) {
                                progress -= 2.0 / SolarShieldDefinition.DISCHARGE_TICKS;
                                if (progress <= 0.0) {
                                    if (charges > 0) {
                                        int newCharges = charges - 1;
                                        itemFactory.updateSolarShieldVisuals(activeShield, newCharges);
                                        if (isMainHand) {
                                            inv.setItemInMainHand(activeShield);
                                        } else {
                                            inv.setItemInOffHand(activeShield);
                                        }
                                        charges = newCharges;
                                        progress = 1.0;
                                    } else {
                                        progress = 0.0;
                                    }
                                }
                            }
                            activeProgress.put(playerId, progress);
                            updateBossBar(player, progress, charges, false);
                        } else {
                            activeProgress.remove(playerId);
                            bossBarManager.removeBar(player, "solar_shield");
                        }
                    }
                } else {
                    activeProgress.remove(playerId);
                    bossBarManager.removeBar(player, "solar_shield");
                }
            }

            // Process dropped shields
            Iterator<Item> iterator = droppedShields.iterator();
            while (iterator.hasNext()) {
                Item itemEntity = iterator.next();
                if (!itemEntity.isValid() || itemEntity.isDead()) {
                    droppedShieldProgress.remove(itemEntity.getUniqueId());
                    iterator.remove();
                    continue;
                }
                
                ItemStack shield = itemEntity.getItemStack();
                int charges = getCharges(shield);
                UUID entityId = itemEntity.getUniqueId();
                double progress = droppedShieldProgress.getOrDefault(entityId, 0.0);
                boolean updated = false;

                if (SolarPower.isSunlit(itemEntity.getLocation())) {
                    if (charges < SolarShieldDefinition.MAX_CHARGES) {
                        progress += 2.0 / SolarShieldDefinition.CHARGE_TICKS;
                        if (progress >= 1.0) {
                            progress -= 1.0;
                            charges++;
                            itemFactory.updateSolarShieldVisuals(shield, charges);
                            updated = true;
                            itemEntity.getWorld().spawnParticle(Particle.WAX_ON, itemEntity.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.05);
                        }
                        droppedShieldProgress.put(entityId, progress);
                    } else {
                        if (progress < SolarShieldDefinition.OVERCHARGE_BUFFER) {
                            progress += 2.0 / SolarShieldDefinition.CHARGE_TICKS;
                            progress = Math.min(progress, SolarShieldDefinition.OVERCHARGE_BUFFER);
                            droppedShieldProgress.put(entityId, progress);
                        }
                    }
                } else {
                    if (charges > 0 || progress > 0.0) {
                        if (progress <= 0.0 && charges > 0) {
                            charges--;
                            itemFactory.updateSolarShieldVisuals(shield, charges);
                            updated = true;
                            progress = 1.0;
                        }
                        if (progress > 0.0) {
                            progress -= 2.0 / SolarShieldDefinition.DISCHARGE_TICKS;
                            if (progress <= 0.0) {
                                if (charges > 0) {
                                    charges--;
                                    itemFactory.updateSolarShieldVisuals(shield, charges);
                                    updated = true;
                                    progress = 1.0;
                                } else {
                                    progress = 0.0;
                                }
                            }
                        }
                        droppedShieldProgress.put(entityId, progress);
                    } else {
                        droppedShieldProgress.remove(entityId);
                    }
                }
                
                if (updated) {
                    itemEntity.setItemStack(shield);
                }
            }
        }, 2L, 2L);
    }

    public void stop() {
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        activeProgress.clear();
        passiveTicks.clear();
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
        boolean isMainHand = false;
        if (itemFactory.isSolarShield(offHand)) {
            activeShield = offHand;
        } else if (itemFactory.isSolarShield(mainHand)) {
            activeShield = mainHand;
            isMainHand = true;
        }

        if (activeShield == null) {
            return;
        }

        int charges = getCharges(activeShield);
        if (charges > 0) {
            itemFactory.updateSolarShieldVisuals(activeShield, charges - 1);
            if (isMainHand) {
                player.getInventory().setItemInMainHand(activeShield);
            } else {
                player.getInventory().setItemInOffHand(activeShield);
            }
            attacker.setFireTicks(80);
            
            if (attacker instanceof org.bukkit.entity.Mob mob) {
                mob.setTarget(null);
                mob.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1));
            }
            //player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
            
            class FlashbangRunnable implements Runnable {
                ScheduledTaskCompat taskRef;
                int ticksElapsed = 0;

                @Override
                public void run() {
                    if (!attacker.isValid() || attacker.isDead() || ticksElapsed >= 40) {
                        if (taskRef != null) {
                            taskRef.cancel();
                        }
                        return;
                    }
                    ticksElapsed += 2;
                    org.bukkit.Location eyeLoc = attacker.getEyeLocation();
                    org.bukkit.util.Vector dir = eyeLoc.getDirection().multiply(0.5);
                    org.bukkit.Location targetLoc = eyeLoc.add(dir);
                    
                    Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.WHITE, 4.0f);
                    attacker.getWorld().spawnParticle(Particle.DUST, targetLoc, 25, 0.4, 0.4, 0.4, 0.0, dust);
                    //attacker.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, targetLoc, 10, 0.3, 0.3, 0.3, 0.02);
                }
            }
            FlashbangRunnable runner = new FlashbangRunnable();
            runner.taskRef = SchedulerCompat.runTimerForEntity(attacker, plugin, runner, 1L, 2L);
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) {
            return;
        }

        CraftingInventory inv = event.getInventory();
        for (ItemStack item : inv.getMatrix()) {
            if (itemFactory.isSolarShield(item)) {
                inv.setResult(null);
                break;
            }
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

    private void updateBossBar(Player player, double progress, int charges, boolean charging) {
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        
        String langKey = charging ? "messages.solar_shield.charge" : "messages.solar_shield.discharging";
        String message = lang.text(langKey)
                .replace("{bar}", "")
                .replace("{charges}", String.valueOf(charges))
                .replace("  ", " ").trim();
                
        bossBarManager.updateBar(player, "solar_shield", message, progress, org.bukkit.boss.BarColor.YELLOW);
    }

    private void showReadyBossBar(Player player) {
        String message = lang.text("messages.solar_shield.ready")
                .replace("{bar}", "")
                .replace("  ", " ").trim();
        bossBarManager.updateBar(player, "solar_shield", message, 1.0D, org.bukkit.boss.BarColor.YELLOW);
    }

    private void resetShield(ItemStack shield) {
        itemFactory.updateSolarShieldVisuals(shield, 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack item = itemEntity.getItemStack();
        if (itemFactory.isSolarShield(item)) {
            droppedShields.add(itemEntity);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;
        
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getType() == InventoryType.PLAYER || topInv.getType() == InventoryType.CRAFTING) {
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.getType() == InventoryType.PLAYER) {
            ItemStack item = event.getCurrentItem();
            if (itemFactory.isSolarShield(item) && getCharges(item) > 0) {
                resetShield(item);
                event.setCurrentItem(item);
            }
        }
        
        if (clickedInv.equals(topInv)) {
            ItemStack cursorItem = event.getCursor();
            if (itemFactory.isSolarShield(cursorItem) && getCharges(cursorItem) > 0) {
                resetShield(cursorItem);
            }
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                ItemStack hotbarItem = event.getView().getBottomInventory().getItem(event.getHotbarButton());
                if (itemFactory.isSolarShield(hotbarItem) && getCharges(hotbarItem) > 0) {
                    resetShield(hotbarItem);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getType() == InventoryType.PLAYER || topInv.getType() == InventoryType.CRAFTING) return;
        
        ItemStack oldCursor = event.getOldCursor();
        if (itemFactory.isSolarShield(oldCursor) && getCharges(oldCursor) > 0) {
            for (java.util.Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                if (entry.getKey() < topInv.getSize()) {
                    resetShield(entry.getValue());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (itemFactory.isSolarShield(item) && getCharges(item) > 0) {
            resetShield(item);
            event.setItem(item);
        }
    }
}
