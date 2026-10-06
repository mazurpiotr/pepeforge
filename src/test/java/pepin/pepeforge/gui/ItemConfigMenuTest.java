package pepin.pepeforge.gui;

import org.junit.jupiter.api.Test;
import pepin.pepeforge.item.ItemIds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemConfigMenuTest {

    @Test
    void recipeToggleIsHiddenForItemsWithoutCraftingTableRecipes() {
        assertFalse(ItemConfigMenu.hasRecipeToggle(ItemIds.CRIMSON_SWORD));
        assertFalse(ItemConfigMenu.hasRecipeToggle(ItemIds.NETHERITE_WIND_BLADE));
        assertFalse(ItemConfigMenu.hasRecipeToggle(ItemIds.NETHERITE_GREATSWORD));
        assertFalse(ItemConfigMenu.hasRecipeToggle(ItemIds.NETHERITE_SCYTHE));
    }

    @Test
    void recipeToggleRemainsAvailableForItemsWithCraftingTableRecipes() {
        assertTrue(ItemConfigMenu.hasRecipeToggle(ItemIds.CRESCENT_BOW));
        assertTrue(ItemConfigMenu.hasRecipeToggle(ItemIds.IRON_GREATSWORD));
        assertTrue(ItemConfigMenu.hasRecipeToggle(ItemIds.DIAMOND_SCYTHE));
        assertTrue(ItemConfigMenu.hasRecipeToggle(ItemIds.EMBERFANG));
    }
}
