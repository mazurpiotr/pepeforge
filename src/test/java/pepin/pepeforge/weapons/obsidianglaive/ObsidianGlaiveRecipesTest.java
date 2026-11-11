package pepin.pepeforge.weapons.obsidianglaive;

import java.util.Arrays;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pepin.pepeforge.item.ItemFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObsidianGlaiveRecipesTest {
    private static final NamespacedKey ITEM_ID = new NamespacedKey("pepeforge", "item_id");
    private ServerMock server;
    private World world;
    private ItemFactory factory;
    private ObsidianGlaiveRecipes recipes;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("obsidian_glaive_recipes");
        factory = mock(ItemFactory.class);
        when(factory.isRecipeEnabled("obsidian_glaive")).thenReturn(true);
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);
        var meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_ID, PersistentDataType.STRING, "obsidian_glaive");
        result.setItemMeta(meta);
        when(factory.createObsidianGlaive()).thenReturn(result);
        recipes = new ObsidianGlaiveRecipes(MockBukkit.createMockPlugin(), factory);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void canonicalDiagonalRecipeAcceptsBothOrientationsAndProducesCustomItem() {
        recipes.registerAll();

        ShapedRecipe registered = assertInstanceOf(ShapedRecipe.class, server.getRecipe(ObsidianGlaiveRecipes.KEY));
        assertArrayEquals(new String[]{"  O", " S ", "S  "}, registered.getShape());
        assertEquals(CraftingBookCategory.EQUIPMENT, registered.getCategory());
        assertNull(server.getRecipe(new NamespacedKey("pepeforge", "obsidian_glaive_mirrored")));
        for (boolean mirrored : new boolean[]{false, true}) {
            Recipe match = server.getCraftingRecipe(matrix(mirrored), world);
            assertNotNull(match);
            assertEquals(ObsidianGlaiveRecipes.KEY, assertInstanceOf(Keyed.class, match).getKey());
            assertEquals("obsidian_glaive", match.getResult().getItemMeta()
                    .getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING));
        }
    }

    @Test
    void wrongHeadOrAdditionalIngredientsCannotCraftGlaive() {
        recipes.registerAll();
        for (boolean mirrored : new boolean[]{false, true}) {
            ItemStack[] wrongHead = matrix(mirrored);
            wrongHead[mirrored ? 0 : 2] = new ItemStack(Material.CRYING_OBSIDIAN);
            assertNull(server.getCraftingRecipe(wrongHead, world));
            ItemStack[] extraIngredient = matrix(mirrored);
            extraIngredient[1] = new ItemStack(Material.STICK);
            assertNull(server.getCraftingRecipe(extraIngredient, world));
        }
    }

    @Test
    void disablingRecipeRemovesRegistrationAndBothCraftingOrientations() {
        recipes.registerAll();
        when(factory.isRecipeEnabled("obsidian_glaive")).thenReturn(false);

        recipes.registerAll();

        assertNull(server.getRecipe(ObsidianGlaiveRecipes.KEY));
        assertNull(server.getCraftingRecipe(matrix(false), world));
        assertNull(server.getCraftingRecipe(matrix(true), world));
    }

    @Test
    void discoveryUsesObsidianWithoutRequiringSticksAndHonorsAvailability() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.STICK)).thenReturn(true);

        recipes.discoverFor(player);

        verify(player, never()).discoverRecipe(any(NamespacedKey.class));
        when(inventory.contains(Material.OBSIDIAN)).thenReturn(true);
        when(inventory.contains(Material.STICK)).thenReturn(false);
        recipes.discoverFor(player);
        verify(player).discoverRecipe(ObsidianGlaiveRecipes.KEY);
        clearInvocations(player);
        when(factory.isRecipeEnabled("obsidian_glaive")).thenReturn(false);
        recipes.discoverFor(player);
        verify(player, never()).discoverRecipe(any(NamespacedKey.class));
    }

    @Test
    void stoppingModuleRemovesRecipe() {
        recipes.registerAll();

        recipes.unregisterAll();

        assertNull(server.getRecipe(ObsidianGlaiveRecipes.KEY));
    }

    private ItemStack[] matrix(boolean mirrored) {
        ItemStack[] matrix = new ItemStack[9];
        Arrays.fill(matrix, new ItemStack(Material.AIR));
        matrix[mirrored ? 0 : 2] = new ItemStack(Material.OBSIDIAN);
        matrix[4] = new ItemStack(Material.STICK);
        matrix[mirrored ? 8 : 6] = new ItemStack(Material.STICK);
        return matrix;
    }
}
