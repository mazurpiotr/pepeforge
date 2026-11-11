package pepin.pepeforge.tools.scythe;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ScytheRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();

        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(ScytheRecipeKeys.IRON_SCYTHE);
        }
        if (inventory.contains(Material.DIAMOND)) {
            player.discoverRecipe(ScytheRecipeKeys.DIAMOND_SCYTHE);
        }
        if (inventory.contains(Material.NETHERITE_INGOT)) {
            player.discoverRecipe(ScytheRecipeKeys.NETHERITE_SCYTHE);
        }
    }
}
