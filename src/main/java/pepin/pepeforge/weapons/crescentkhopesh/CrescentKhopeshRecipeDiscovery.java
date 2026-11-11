package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class CrescentKhopeshRecipeDiscovery {

    public void discoverFor(Player player) {
        if (player.getInventory().contains(Material.AMETHYST_SHARD)) {
            player.discoverRecipe(CrescentKhopeshRecipeKeys.CRESCENT_KHOPESH);
        }
    }
}
