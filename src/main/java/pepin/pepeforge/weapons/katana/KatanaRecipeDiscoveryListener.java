package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class KatanaRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;

    public KatanaRecipeDiscoveryListener(JavaPlugin plugin) {
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
        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(KatanaRecipeKeys.KATANA);
        }
    }

    @EventHandler
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (event.getRecipe() == null || event.getRecipe().getKey() == null) {
            return;
        }
        if (KatanaRecipeKeys.KATANA_MIRRORED.equals(event.getRecipe().getKey())) {
            event.setCancelled(true);
        }
    }
}
