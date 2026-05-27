package pepin.pepeforge.recipe;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class RecipeDiscoveryRefresher implements Listener {

    private final JavaPlugin plugin;
    private final Consumer<Player> discoverer;
    private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();

    public RecipeDiscoveryRefresher(JavaPlugin plugin, Consumer<Player> discoverer) {
        this.plugin = plugin;
        this.discoverer = discoverer;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refreshLater(event.getPlayer(), 1L);
        refreshLater(event.getPlayer(), 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BukkitTask task = pendingTasks.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            refreshSoon(player);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        refreshSoon(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        refreshSoon(event.getPlayer());
    }

    public void refreshAllOnlinePlayers() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            refreshLater(player, 1L);
            refreshLater(player, 40L);
        });
    }

    private void refreshSoon(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask previousTask = pendingTasks.remove(playerId);
        if (previousTask != null) {
            previousTask.cancel();
        }
        pendingTasks.put(playerId, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTasks.remove(playerId);
            refresh(player);
        }, 1L));
    }

    private void refreshLater(Player player, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refresh(player), delayTicks);
    }

    private void refresh(Player player) {
        if (player.isOnline()) {
            discoverer.accept(player);
        }
    }
}
