package pepin.pepeforge.tools.scythe;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.item.ItemFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ScytheListener implements Listener {

    private static final Map<Material, Material> REPLANT_ITEMS = new EnumMap<>(Material.class);
    private static final Map<Material, Material> REQUIRED_SOIL = new EnumMap<>(Material.class);

    static {
        REPLANT_ITEMS.put(Material.WHEAT, Material.WHEAT_SEEDS);
        REPLANT_ITEMS.put(Material.CARROTS, Material.CARROT);
        REPLANT_ITEMS.put(Material.POTATOES, Material.POTATO);
        REPLANT_ITEMS.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        REPLANT_ITEMS.put(Material.NETHER_WART, Material.NETHER_WART);

        REQUIRED_SOIL.put(Material.WHEAT, Material.FARMLAND);
        REQUIRED_SOIL.put(Material.CARROTS, Material.FARMLAND);
        REQUIRED_SOIL.put(Material.POTATOES, Material.FARMLAND);
        REQUIRED_SOIL.put(Material.BEETROOTS, Material.FARMLAND);
        REQUIRED_SOIL.put(Material.NETHER_WART, Material.SOUL_SAND);
    }

    private static final ThreadLocal<Boolean> PROTECTION_CHECK = ThreadLocal.withInitial(() -> false);

    private final ItemFactory itemFactory;

    public ScytheListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (PROTECTION_CHECK.get()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        ScytheTier tier = itemFactory.getScytheTier(tool);
        if (tier == null) {
            return;
        }

        Block center = event.getBlock();
        if (!isRipeSupportedCrop(center)) {
            return;
        }

        event.setCancelled(true);

        World world = center.getWorld();
        List<HarvestEntry> harvestedBlocks = new ArrayList<>();
        for (int x = -tier.radius(); x <= tier.radius(); x++) {
            for (int z = -tier.radius(); z <= tier.radius(); z++) {
                Block block = world.getBlockAt(center.getX() + x, center.getY(), center.getZ() + z);
                if (!canBreakBlock(player, block)) {
                    continue;
                }
                HarvestEntry entry = prepareHarvest(block, player, tool);
                if (entry != null) {
                    harvestedBlocks.add(entry);
                }
            }
        }

        int harvested = harvestedBlocks.size();
        if (harvested == 0) {
            return;
        }

        Map<Material, Integer> pooledDrops = collectDrops(harvestedBlocks);
        for (HarvestEntry entry : harvestedBlocks) {
            entry.shouldReplant = shouldReplant(entry, pooledDrops, player);
        }

        executeHarvest(harvestedBlocks, pooledDrops, player);

        world.playSound(center.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 1.15f);
        world.playSound(center.getLocation(), Sound.ITEM_HOE_TILL, 0.55f, 1.35f);
        world.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0, 1.0, 0.0), 3, 0.25, 0.2, 0.25, 0.0);

        if (player.getGameMode() != GameMode.CREATIVE) {
            damageTool(tool, harvested);
        }
    }

    private HarvestEntry prepareHarvest(Block block, Player player, ItemStack tool) {
        if (!isRipeSupportedCrop(block)) {
            return null;
        }

        return new HarvestEntry(block, block.getType(), cloneDrops(block.getDrops(tool, player)));
    }

    private Map<Material, Integer> collectDrops(List<HarvestEntry> harvestedBlocks) {
        Map<Material, Integer> pooledDrops = new EnumMap<>(Material.class);
        for (HarvestEntry entry : harvestedBlocks) {
            for (ItemStack drop : entry.drops) {
                if (drop.getAmount() <= 0) {
                    continue;
                }
                pooledDrops.merge(drop.getType(), drop.getAmount(), (oldAmount, incomingAmount) -> oldAmount + incomingAmount);
            }
        }
        return pooledDrops;
    }

    private boolean shouldReplant(HarvestEntry entry, Map<Material, Integer> pooledDrops, Player player) {
        Material replantItem = REPLANT_ITEMS.get(entry.cropType);
        if (replantItem == null) {
            return false;
        }
        if (entry.block.getRelative(0, -1, 0).getType() != requiredSoil(entry.cropType)) {
            return false;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (consumeOne(pooledDrops, replantItem)) {
            return true;
        }
        return removeOneFromInventory(player, replantItem);
    }

    private void executeHarvest(List<HarvestEntry> harvestedBlocks, Map<Material, Integer> pooledDrops, Player player) {
        for (HarvestEntry entry : harvestedBlocks) {
            if (!canBreakBlock(player, entry.block)) {
                continue;
            }

            entry.block.setType(Material.AIR, false);
            entry.block.getWorld().spawnParticle(
                    Particle.BLOCK,
                    entry.block.getLocation().add(0.5, 0.5, 0.5),
                    20,
                    0.3, 0.3, 0.3,
                    0.0,
                    entry.cropType.createBlockData()
            );

            if (entry.shouldReplant) {
                Material replantItem = REPLANT_ITEMS.get(entry.cropType);
                if (replantItem == null) {
                    continue;
                }
                if (!canPlaceBlock(player, entry.block, replantItem)) {
                    continue;
                }
                entry.block.setType(entry.cropType, false);
                if (entry.block.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(0);
                    entry.block.setBlockData(ageable, false);
                }
            }
        }

        for (Map.Entry<Material, Integer> pooledDrop : pooledDrops.entrySet()) {
            if (pooledDrop.getValue() <= 0) {
                continue;
            }

            ItemStack stack = new ItemStack(pooledDrop.getKey(), pooledDrop.getValue());
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation().add(0.0, 0.5, 0.0), leftover);
            }
        }
    }

    private boolean isRipeSupportedCrop(Block block) {
        if (!REPLANT_ITEMS.containsKey(block.getType())) {
            return false;
        }
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private List<ItemStack> cloneDrops(java.util.Collection<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            result.add(drop.clone());
        }
        return result;
    }

    private boolean consumeOne(Map<Material, Integer> pooledDrops, Material material) {
        Integer amount = pooledDrops.get(material);
        if (amount == null || amount <= 0) {
            return false;
        }
        pooledDrops.put(material, amount - 1);
        return true;
    }

    private boolean removeOneFromInventory(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            if (stack == null || stack.getType() != material || stack.getAmount() <= 0) {
                continue;
            }

            int nextAmount = stack.getAmount() - 1;
            if (nextAmount <= 0) {
                player.getInventory().setItem(index, null);
            } else {
                stack.setAmount(nextAmount);
            }
            return true;
        }
        return false;
    }

    private Material requiredSoil(Material cropType) {
        return REQUIRED_SOIL.getOrDefault(cropType, Material.FARMLAND);
    }

    private boolean canBreakBlock(Player player, Block block) {
        if (block == null || player == null) {
            return false;
        }

        PROTECTION_CHECK.set(true);
        try {
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            Bukkit.getPluginManager().callEvent(breakEvent);
            return !breakEvent.isCancelled();
        } finally {
            PROTECTION_CHECK.set(false);
        }
    }

    private boolean canPlaceBlock(Player player, Block block, Material material) {
        if (block == null || player == null) {
            return false;
        }

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                block,
                block.getState(),
                block.getRelative(BlockFace.DOWN),
                new ItemStack(material),
                player,
                true,
                EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(placeEvent);
        return !placeEvent.isCancelled();
    }

    private void damageTool(ItemStack tool, int amount) {
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int maxDamage = tool.getType().getMaxDurability();
        int nextDamage = damageable.getDamage() + amount;
        if (nextDamage >= maxDamage) {
            tool.setAmount(0);
            return;
        }

        damageable.setDamage(nextDamage);
        tool.setItemMeta(meta);
    }

    private static final class HarvestEntry {
        private final Block block;
        private final Material cropType;
        private final List<ItemStack> drops;
        private boolean shouldReplant;

        private HarvestEntry(Block block, Material cropType, List<ItemStack> drops) {
            this.block = block;
            this.cropType = cropType;
            this.drops = drops;
            this.shouldReplant = false;
        }
    }
}
