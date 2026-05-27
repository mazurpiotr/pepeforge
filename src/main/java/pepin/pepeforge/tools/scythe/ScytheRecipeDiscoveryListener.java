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
        plugin.getServer().getScheduler().runTask(plugin, () -> discoverFor(player));
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        discoverIfReady(player, inventory, ScytheTier.IRON, Material.IRON_INGOT);
        discoverIfReady(player, inventory, ScytheTier.DIAMOND, Material.DIAMOND);
        discoverIfReady(player, inventory, ScytheTier.NETHERITE, Material.NETHERITE_INGOT);
    }

    private void discoverIfReady(Player player, Inventory inventory, ScytheTier tier, Material bladeMaterial) {
        if (inventory.contains(bladeMaterial) && inventory.contains(Material.STICK)) {
            player.discoverRecipe(RecipeKeys.forTier(tier));
        }
    }
}
