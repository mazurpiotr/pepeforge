package pepin.pepeforge.weapons.rapier;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pepin.pepeforge.item.ItemFactory;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RapierRecipesTest {
    private ServerMock server;
    private World world;
    private ItemFactory factory;
    private RapierRecipes recipes;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("rapier_recipes");
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        factory = mock(ItemFactory.class);
        for (RapierTier tier : RapierTier.values()) {
            when(factory.isItemEnabled(tier.itemId())).thenReturn(true);
            when(factory.isRecipeEnabled(tier.itemId())).thenReturn(true);
            ItemStack item = new ItemStack(tier.baseMaterial());
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey("pepeforge", "item_id"),
                    PersistentDataType.STRING, tier.itemId());
            item.setItemMeta(meta);
            when(factory.createRapier(tier)).thenReturn(item);
        }
        recipes = new RapierRecipes(plugin, factory);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void craftableRapiersRequireOneStickAndTwoBladeMaterialsInOneRow() {
        when(factory.isRecipeEnabled(RapierTier.NETHERITE.itemId())).thenReturn(false);
        recipes.registerAll();
        for (RapierTier tier : new RapierTier[]{RapierTier.IRON, RapierTier.DIAMOND, RapierTier.ICE}) {
            ShapedRecipe recipe = assertInstanceOf(ShapedRecipe.class, server.getRecipe(RapierRecipeKeys.forTier(tier)));
            assertArrayEquals(new String[]{"SMM"}, recipe.getShape());
            for (int row = 0; row < 3; row++) {
                for (boolean mirrored : new boolean[]{false, true}) {
                    ItemStack[] grid = new ItemStack[9];
                    Arrays.fill(grid, new ItemStack(Material.AIR));
                    for (int column = 0; column < 3; column++) {
                        grid[row * 3 + column] = new ItemStack(column == (mirrored ? 2 : 0)
                                ? Material.STICK : tier.bladeMaterial());
                    }
                    Recipe match = server.getCraftingRecipe(grid, world);
                    assertNotNull(match);
                    assertEquals(tier.itemId(), match.getResult().getItemMeta().getPersistentDataContainer()
                            .get(new NamespacedKey("pepeforge", "item_id"), PersistentDataType.STRING));
                    grid[row * 3 + 1] = new ItemStack(Material.IRON_NUGGET);
                    assertNull(server.getCraftingRecipe(grid, world));
                }
            }
        }
    }

    @Test
    void netheriteRequiresTheCustomDiamondRapierAndNativeUpgradeIngredients() {
        recipes.registerAll();
        SmithingTransformRecipe recipe = assertInstanceOf(SmithingTransformRecipe.class,
                server.getRecipe(RapierRecipeKeys.NETHERITE_RAPIER));
        assertTrue(recipe.getBase().test(factory.createRapier(RapierTier.DIAMOND)));
        assertFalse(recipe.getBase().test(new ItemStack(Material.DIAMOND_SWORD)));
        assertTrue(recipe.getTemplate().test(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        assertFalse(recipe.getTemplate().test(new ItemStack(Material.DIAMOND)));
        assertTrue(recipe.getAddition().test(new ItemStack(Material.NETHERITE_INGOT)));
    }

    @Test
    void registrationAndDiscoveryRespectRecipeAndItemAvailabilityAndRemoveOldRecipes() {
        recipes.registerAll();
        when(factory.isRecipeEnabled(RapierTier.IRON.itemId())).thenReturn(false);
        when(factory.isItemEnabled(RapierTier.DIAMOND.itemId())).thenReturn(false);
        when(factory.isItemEnabled(RapierTier.ICE.itemId())).thenReturn(false);
        recipes.registerAll();
        for (RapierTier tier : RapierTier.values()) {
            assertNull(server.getRecipe(RapierRecipeKeys.forTier(tier)));
        }
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(any(Material.class))).thenReturn(true);
        new RapierRecipeDiscovery(factory).discoverFor(player);
        verify(player, never()).discoverRecipe(any(NamespacedKey.class));
    }

    @Test
    void discoveryUsesOneDesignatedMaterialPerTierAndHonorsNetheriteRecipeToggle() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.DIAMOND)).thenReturn(true);
        when(inventory.contains(Material.NETHERITE_INGOT)).thenReturn(true);
        when(factory.isRecipeEnabled(RapierTier.NETHERITE.itemId())).thenReturn(false);
        new RapierRecipeDiscovery(factory).discoverFor(player);
        verify(player).discoverRecipe(RapierRecipeKeys.DIAMOND_RAPIER);
        verify(player, never()).discoverRecipe(RapierRecipeKeys.IRON_RAPIER);
        verify(player, never()).discoverRecipe(RapierRecipeKeys.NETHERITE_RAPIER);
    }
}
