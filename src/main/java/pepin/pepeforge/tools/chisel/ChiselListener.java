package pepin.pepeforge.tools.chisel;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.item.ItemFactory;

import java.util.EnumMap;
import java.util.Map;

public final class ChiselListener implements Listener {

    private static final Map<Material, Material[]> FAMILIES = new EnumMap<>(Material.class);

    static {
        addFamily(
                Material.STONE,
                Material.STONE_BRICKS,
                Material.CRACKED_STONE_BRICKS,
                Material.CHISELED_STONE_BRICKS
        );
        addFamily(
                Material.DEEPSLATE,
                Material.COBBLED_DEEPSLATE,
                Material.POLISHED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS,
                Material.CRACKED_DEEPSLATE_BRICKS,
                Material.DEEPSLATE_TILES,
                Material.CRACKED_DEEPSLATE_TILES,
                Material.CHISELED_DEEPSLATE
        );
        addFamily(
                Material.BLACKSTONE,
                Material.POLISHED_BLACKSTONE,
                Material.CHISELED_POLISHED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE_BRICKS,
                Material.CRACKED_POLISHED_BLACKSTONE_BRICKS
        );
        addFamily(
                Material.SANDSTONE,
                Material.CHISELED_SANDSTONE,
                Material.CUT_SANDSTONE,
                Material.SMOOTH_SANDSTONE
        );
        addFamily(
                Material.RED_SANDSTONE,
                Material.CHISELED_RED_SANDSTONE,
                Material.CUT_RED_SANDSTONE,
                Material.SMOOTH_RED_SANDSTONE
        );
        addFamily(
                Material.QUARTZ_BLOCK,
                Material.CHISELED_QUARTZ_BLOCK,
                Material.QUARTZ_BRICKS,
                Material.QUARTZ_PILLAR,
                Material.SMOOTH_QUARTZ
        );
    }

    private final ItemFactory itemFactory;

    public ChiselListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!itemFactory.isChisel(tool)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Material[] family = FAMILIES.get(block.getType());
        if (family == null) {
            return;
        }

        Material next = nextVariant(block.getType(), family, player.isSneaking());
        if (next == block.getType()) {
            return;
        }

        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);

        block.setType(next, false);
        playEffects(block);

        if (player.getGameMode() != GameMode.CREATIVE) {
            damageTool(tool, ChiselDefinition.DURABILITY_COST);
        }
    }

    private static void addFamily(Material... family) {
        for (Material material : family) {
            FAMILIES.put(material, family);
        }
    }

    private Material nextVariant(Material current, Material[] family, boolean reverse) {
        for (int i = 0; i < family.length; i++) {
            if (family[i] != current) {
                continue;
            }
            int nextIndex = reverse ? (i - 1 + family.length) % family.length : (i + 1) % family.length;
            return family[nextIndex];
        }
        return current;
    }

    private void playEffects(Block block) {
        World world = block.getWorld();
        world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 0.85f, 1.45f);
        world.playSound(block.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.55f, 1.8f);
        world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 12, 0.2, 0.2, 0.2, 0.0, block.getBlockData());
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
}
