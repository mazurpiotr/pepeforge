package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class StormcleaverRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.CONDUIT)) {
            player.discoverRecipe(StormcleaverRecipeKeys.STORMCLEAVER);
        }
    }
}
