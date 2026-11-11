package pepin.pepeforge.weapons.anchor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class AnchorRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.IRON_BLOCK)) {
            player.discoverRecipe(AnchorRecipeKeys.ANCHOR);
        }
    }
}
