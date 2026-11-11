package pepin.pepeforge.recipe;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipes;
import pepin.pepeforge.weapons.katana.KatanaRecipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MirroredRecipesTest {

    private static final NamespacedKey ITEM_ID = new NamespacedKey("pepeforge", "item_id");

    private ServerMock server;
    private World world;
    private JavaPlugin plugin;
    private ItemFactory factory;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("recipes");
        plugin = MockBukkit.createMockPlugin();
        factory = mock(ItemFactory.class);
        when(factory.isRecipeEnabled("katana")).thenReturn(true);
        when(factory.isRecipeEnabled("crescent_bow")).thenReturn(true);
        when(factory.createKatana()).thenReturn(result(Material.IRON_SWORD, "katana"));
        when(factory.createCrescentBow()).thenReturn(result(Material.BOW, "crescent_bow"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @EnumSource(Weapon.class)
    void canonicalRecipeMatchesBothOrientations(Weapon weapon) {
        register(weapon);

        assertNotNull(server.getRecipe(weapon.key()));
        assertNull(server.getRecipe(weapon.legacyKey()));
        for (boolean mirrored : new boolean[]{false, true}) {
            Recipe recipe = server.getCraftingRecipe(weapon.matrix(mirrored), world);
            assertNotNull(recipe);
            assertEquals(weapon.key(), assertInstanceOf(Keyed.class, recipe).getKey());
            assertEquals(weapon.itemId, recipe.getResult().getItemMeta()
                    .getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING));
        }
    }

    @ParameterizedTest
    @EnumSource(Weapon.class)
    void rejectsWrongIngredientsAndExtraItems(Weapon weapon) {
        register(weapon);

        for (boolean mirrored : new boolean[]{false, true}) {
            ItemStack[] wrongIngredient = weapon.matrix(mirrored);
            int occupiedSlot = weapon == Weapon.KATANA ? (mirrored ? 0 : 2) : 0;
            wrongIngredient[occupiedSlot] = new ItemStack(Material.COAL);
            assertNull(server.getCraftingRecipe(wrongIngredient, world));

            ItemStack[] extraItem = weapon.matrix(mirrored);
            int emptySlot = weapon == Weapon.KATANA ? 1 : 4;
            extraItem[emptySlot] = new ItemStack(Material.DIRT);
            assertNull(server.getCraftingRecipe(extraItem, world));
        }
    }

    @ParameterizedTest
    @EnumSource(Weapon.class)
    void registrationRemovesStaleMirroredRecipe(Weapon weapon) {
        ShapedRecipe staleRecipe = new ShapedRecipe(weapon.legacyKey(), new ItemStack(Material.DIRT));
        staleRecipe.shape("D").setIngredient('D', Material.DIRT);
        assertTrue(server.addRecipe(staleRecipe));

        register(weapon);

        assertNull(server.getRecipe(weapon.legacyKey()));
        assertNotNull(server.getRecipe(weapon.key()));
    }

    @ParameterizedTest
    @EnumSource(Weapon.class)
    void disablingRecipeRemovesBothKeysAndBothOrientations(Weapon weapon) {
        register(weapon);
        when(factory.isRecipeEnabled(weapon.itemId)).thenReturn(false);

        register(weapon);

        assertNull(server.getRecipe(weapon.key()));
        assertNull(server.getRecipe(weapon.legacyKey()));
        assertNull(server.getCraftingRecipe(weapon.matrix(false), world));
        assertNull(server.getCraftingRecipe(weapon.matrix(true), world));
    }

    private void register(Weapon weapon) {
        if (weapon == Weapon.KATANA) {
            new KatanaRecipes(plugin, factory).registerAll();
        } else {
            new CrescentBowRecipes(plugin, factory).registerAll();
        }
    }

    private static ItemStack result(Material material, String itemId) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_ID, PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);
        return item;
    }

    private enum Weapon {
        KATANA("katana", new String[]{"  I", " I ", "S  "}),
        CRESCENT_BOW("crescent_bow", new String[]{"PAS", "P A", "PAS"});

        private final String itemId;
        private final String[] shape;

        Weapon(String itemId, String[] shape) {
            this.itemId = itemId;
            this.shape = shape;
        }

        private NamespacedKey key() {
            return new NamespacedKey("pepeforge", itemId);
        }

        private NamespacedKey legacyKey() {
            return new NamespacedKey("pepeforge", itemId + "_mirrored");
        }

        private ItemStack[] matrix(boolean mirrored) {
            ItemStack[] matrix = new ItemStack[9];
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    char ingredient = shape[row].charAt(mirrored ? 2 - column : column);
                    Material material = switch (ingredient) {
                        case 'I' -> Material.IRON_INGOT;
                        case 'S' -> Material.STICK;
                        case 'P' -> Material.PHANTOM_MEMBRANE;
                        case 'A' -> Material.AMETHYST_SHARD;
                        default -> Material.AIR;
                    };
                    matrix[row * 3 + column] = new ItemStack(material);
                }
            }
            return matrix;
        }
    }
}
