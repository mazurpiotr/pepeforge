package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrescentBowRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;

    public CrescentBowRecipeDiscoveryListener(JavaPlugin plugin) {
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

    private void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.BOW)
                && inventory.contains(Material.BREEZE_ROD)
                && inventory.contains(Material.PHANTOM_MEMBRANE)) {
            player.discoverRecipe(CrescentBowRecipeKeys.CRESCENT_BOW);
        }
    }
}
