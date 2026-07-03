package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
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
        pepin.pepeforge.util.scheduler.SchedulerCompat.runForPlayer(player, plugin, () -> discoverFor(player));
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.AMETHYST_SHARD) || inventory.contains(Material.PHANTOM_MEMBRANE)) {
            player.discoverRecipe(CrescentBowRecipeKeys.CRESCENT_BOW);
        }
    }

    @EventHandler
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (CrescentBowRecipeKeys.CRESCENT_BOW_MIRRORED.equals(event.getRecipe())) {
            event.setCancelled(true);
        }
    }
}
