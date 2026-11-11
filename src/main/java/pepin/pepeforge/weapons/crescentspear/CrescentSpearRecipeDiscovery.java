package pepin.pepeforge.weapons.crescentspear;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class CrescentSpearRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.AMETHYST_SHARD)) {
            player.discoverRecipe(CrescentSpearRecipeKeys.CRESCENT_SPEAR);
        }
    }
}
