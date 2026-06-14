package pepin.pepeforge.weapons.windblade;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public final class WindBladeRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public WindBladeRecipeDiscoveryListener(JavaPlugin plugin, ItemFactory itemFactory) {
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
        pepin.pepeforge.util.SchedulerCompat.runForPlayer(player, plugin, () -> discoverFor(player));
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();

        if (inventory.contains(Material.BREEZE_ROD)) {
            player.discoverRecipe(WindBladeRecipeKeys.IRON_WIND_BLADE);
            player.discoverRecipe(WindBladeRecipeKeys.DIAMOND_WIND_BLADE);
        }
        if (inventory.contains(Material.NETHERITE_INGOT)) {
            player.discoverRecipe(WindBladeRecipeKeys.NETHERITE_WIND_BLADE);
        }
    }
}
