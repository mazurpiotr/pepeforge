package pepin.pepeforge.tools.scythe;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScytheRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;

    public ScytheRecipeDiscoveryListener(JavaPlugin plugin) {
        this.plugin = plugin;
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
        pepin.pepeforge.util.scheduler.SchedulerCompat.runForPlayer(player, plugin, () -> discoverFor(player));
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();

        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(ScytheRecipeKeys.IRON_SCYTHE);
        }
        if (inventory.contains(Material.DIAMOND)) {
            player.discoverRecipe(ScytheRecipeKeys.DIAMOND_SCYTHE);
        }
        if (inventory.contains(Material.NETHERITE_INGOT)) {
            player.discoverRecipe(ScytheRecipeKeys.NETHERITE_SCYTHE);
        }
    }
}
