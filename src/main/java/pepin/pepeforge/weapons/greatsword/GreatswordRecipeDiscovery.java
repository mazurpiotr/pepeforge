package pepin.pepeforge.weapons.greatsword;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
public final class GreatswordRecipeDiscovery {

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();

        if (inventory.contains(Material.IRON_INGOT)) {
            player.discoverRecipe(GreatswordRecipeKeys.IRON_GREATSWORD);
        }
        if (inventory.contains(Material.DIAMOND)) {
            player.discoverRecipe(GreatswordRecipeKeys.DIAMOND_GREATSWORD);
        }
        if (inventory.contains(Material.NETHERITE_INGOT)) {
            player.discoverRecipe(GreatswordRecipeKeys.NETHERITE_GREATSWORD);
        }
    }
}
