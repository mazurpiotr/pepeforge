package pepin.pepeforge.tools.chisel;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ChiselRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory.contains(org.bukkit.Material.IRON_INGOT) || inventory.contains(org.bukkit.Material.COPPER_INGOT)) {
            player.discoverRecipe(ChiselRecipeKeys.CHISEL);
        }
    }
}
