package pepin.pepeforge.tools.chisel;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChiselRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;

    public ChiselRecipeDiscoveryListener(JavaPlugin plugin) {
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
        if (inventory.contains(ChiselDefinition.TOP_MATERIAL)
                && inventory.contains(ChiselDefinition.CORE_MATERIAL)
                && inventory.contains(ChiselDefinition.HANDLE_MATERIAL)) {
            player.discoverRecipe(ChiselRecipeKeys.CHISEL);
        }
    }
}
