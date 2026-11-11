package pepin.pepeforge.weapons.crescentbow;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class CrescentBowRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.AMETHYST_SHARD) || inventory.contains(Material.PHANTOM_MEMBRANE)) {
            player.discoverRecipe(CrescentBowRecipeKeys.CRESCENT_BOW);
        }
    }
}
