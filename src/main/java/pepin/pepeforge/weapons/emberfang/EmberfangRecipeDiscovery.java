package pepin.pepeforge.weapons.emberfang;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pepin.pepeforge.item.ItemFactory;

public final class EmberfangRecipeDiscovery {

    private final ItemFactory itemFactory;

    public EmberfangRecipeDiscovery(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public void discoverFor(Player player) {
        if (itemFactory.isRecipeEnabled(EmberfangDefinition.ITEM_ID)) {
            Inventory inventory = player.getInventory();
            if (inventory.contains(Material.MAGMA_BLOCK)) {
                player.discoverRecipe(EmberfangRecipeKeys.EMBERFANG);
            }
        }
    }
}
