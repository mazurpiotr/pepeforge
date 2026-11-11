package pepin.pepeforge.weapons.throwingknife;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ThrowingKnifeRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(ThrowingKnifeRecipeKeys.THROWING_KNIFE);
        }
    }
}
