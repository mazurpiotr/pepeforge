package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class GreatswordRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public GreatswordRecipeDiscoveryListener(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        discoverFor(event.getPlayer());
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> discoverFor(player));
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();

        if (inventory.contains(Material.IRON_INGOT) && inventory.contains(Material.STICK)) {
            player.discoverRecipe(GreatswordRecipeKeys.IRON_GREATSWORD);
        }
        if (inventory.contains(Material.DIAMOND) && inventory.contains(Material.STICK)) {
            player.discoverRecipe(GreatswordRecipeKeys.DIAMOND_GREATSWORD);
        }
        if (inventory.contains(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                && inventory.contains(Material.NETHERITE_INGOT)
                && itemFactory.hasGreatsword(inventory, GreatswordTier.DIAMOND)) {
            player.discoverRecipe(GreatswordRecipeKeys.NETHERITE_GREATSWORD);
        }
    }
}
