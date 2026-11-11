package pepin.pepeforge.weapons.rapier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pepin.pepeforge.item.ItemFactory;

public final class RapierRecipeDiscovery {
    private final ItemFactory itemFactory;

    public RapierRecipeDiscovery(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    public void discoverFor(Player player) {
        Inventory inventory = player.getInventory();
        for (RapierTier tier : RapierTier.values()) {
            if (RapierRecipes.isRecipeAvailable(itemFactory, tier) && inventory.contains(tier.bladeMaterial())) {
                player.discoverRecipe(RapierRecipeKeys.forTier(tier));
            }
        }
    }
}
