package pepin.pepeforge.weapons.emberfang;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

public final class EmberfangRecipeDiscoveryListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;

    public EmberfangRecipeDiscoveryListener(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        discoverFor(event.getPlayer());
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            SchedulerCompat.runForPlayer(player, plugin, () -> discoverFor(player));
        }
    }

    public void discoverFor(Player player) {
        if (itemFactory.isRecipeEnabled(EmberfangDefinition.ITEM_ID)) {
            Inventory inventory = player.getInventory();
            if (inventory.contains(Material.MAGMA_BLOCK)) {
                player.discoverRecipe(EmberfangRecipeKeys.EMBERFANG);
            }
        }
    }
}
