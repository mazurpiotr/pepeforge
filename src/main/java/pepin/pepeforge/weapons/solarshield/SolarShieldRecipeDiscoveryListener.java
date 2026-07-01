package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class SolarShieldRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;

    public SolarShieldRecipeDiscoveryListener(JavaPlugin plugin) {
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
        if (inventory.contains(Material.ECHO_SHARD)) {
            player.discoverRecipe(new org.bukkit.NamespacedKey(plugin, "solar_shield"));
        }
    }
}
