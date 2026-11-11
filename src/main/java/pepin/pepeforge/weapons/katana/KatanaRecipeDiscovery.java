package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class KatanaRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(KatanaRecipeKeys.KATANA);
        }
    }
}
