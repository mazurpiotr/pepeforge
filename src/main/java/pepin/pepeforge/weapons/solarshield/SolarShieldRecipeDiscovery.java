package pepin.pepeforge.weapons.solarshield;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class SolarShieldRecipeDiscovery {

    private final JavaPlugin plugin;

    public SolarShieldRecipeDiscovery(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.ECHO_SHARD)) {
            player.discoverRecipe(new org.bukkit.NamespacedKey(plugin, "solar_shield"));
        }
    }
}
