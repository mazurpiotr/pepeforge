package pepin.pepeforge.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.tools.chisel.ChiselRecipeDiscovery;
import pepin.pepeforge.tools.scythe.ScytheRecipeDiscovery;
import pepin.pepeforge.weapons.anchor.AnchorRecipeDiscovery;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipeDiscovery;
import pepin.pepeforge.weapons.crescentkhopesh.CrescentKhopeshRecipeDiscovery;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearRecipeDiscovery;
import pepin.pepeforge.weapons.emberfang.EmberfangRecipeDiscovery;
import pepin.pepeforge.weapons.greatsword.GreatswordRecipeDiscovery;
import pepin.pepeforge.weapons.katana.KatanaRecipeDiscovery;
import pepin.pepeforge.weapons.solarshield.SolarShieldRecipeDiscovery;
import pepin.pepeforge.weapons.stormcleaver.StormcleaverRecipeDiscovery;
import pepin.pepeforge.weapons.throwingknife.ThrowingKnifeRecipeDiscovery;
import pepin.pepeforge.weapons.windblade.WindBladeRecipeDiscovery;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RecipeDiscoveryRulesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("discoveryRules")
    void discoversOnlyTheExpectedRecipesWhenTheRequiredMaterialIsPresent(
            String name, Consumer<Player> discovery, Material material, List<String> recipeNames) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        discovery.accept(player);
        verify(player, never()).discoverRecipe(any(NamespacedKey.class));

        when(inventory.contains(material)).thenReturn(true);
        discovery.accept(player);

        for (String recipeName : recipeNames) {
            verify(player).discoverRecipe(new NamespacedKey("pepeforge", recipeName));
        }
        verify(player, atLeastOnce()).getInventory();
        verifyNoMoreInteractions(player);
    }

    @Test
    void disabledEmberfangRecipeIsNotDiscoveredEvenWithMagmaBlocks() {
        ItemFactory factory = mock(ItemFactory.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.MAGMA_BLOCK)).thenReturn(true);

        new EmberfangRecipeDiscovery(factory).discoverFor(player);

        verifyNoMoreInteractions(player);
    }

    private static Stream<Arguments> discoveryRules() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("PepeForge");
        when(plugin.namespace()).thenReturn("pepeforge");
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.isRecipeEnabled("emberfang")).thenReturn(true);

        return Stream.of(
                rule("Anchor", new AnchorRecipeDiscovery()::discoverFor, Material.IRON_BLOCK, "anchor"),
                rule("Chisel with iron", new ChiselRecipeDiscovery()::discoverFor, Material.IRON_INGOT, "chisel"),
                rule("Chisel with copper", new ChiselRecipeDiscovery()::discoverFor, Material.COPPER_INGOT, "chisel"),
                rule("Crescent Bow with amethyst", new CrescentBowRecipeDiscovery()::discoverFor, Material.AMETHYST_SHARD, "crescent_bow"),
                rule("Crescent Bow with membrane", new CrescentBowRecipeDiscovery()::discoverFor, Material.PHANTOM_MEMBRANE, "crescent_bow"),
                rule("Crescent Khopesh", new CrescentKhopeshRecipeDiscovery()::discoverFor, Material.AMETHYST_SHARD, "crescent_khopesh"),
                rule("Crescent Spear", new CrescentSpearRecipeDiscovery()::discoverFor, Material.AMETHYST_SHARD, "crescent_spear"),
                rule("Emberfang", new EmberfangRecipeDiscovery(factory)::discoverFor, Material.MAGMA_BLOCK, "emberfang"),
                rule("Iron Greatsword", new GreatswordRecipeDiscovery()::discoverFor, Material.IRON_INGOT, "iron_greatsword"),
                rule("Diamond Greatsword", new GreatswordRecipeDiscovery()::discoverFor, Material.DIAMOND, "diamond_greatsword"),
                rule("Netherite Greatsword", new GreatswordRecipeDiscovery()::discoverFor, Material.NETHERITE_INGOT, "netherite_greatsword"),
                rule("Katana", new KatanaRecipeDiscovery()::discoverFor, Material.IRON_INGOT, "katana"),
                rule("Iron Scythe", new ScytheRecipeDiscovery()::discoverFor, Material.IRON_INGOT, "iron_scythe"),
                rule("Diamond Scythe", new ScytheRecipeDiscovery()::discoverFor, Material.DIAMOND, "diamond_scythe"),
                rule("Netherite Scythe", new ScytheRecipeDiscovery()::discoverFor, Material.NETHERITE_INGOT, "netherite_scythe"),
                rule("Solar Shield", new SolarShieldRecipeDiscovery(plugin)::discoverFor, Material.ECHO_SHARD, "solar_shield"),
                rule("Stormcleaver", new StormcleaverRecipeDiscovery()::discoverFor, Material.CONDUIT, "stormcleaver"),
                rule("Throwing Knife", new ThrowingKnifeRecipeDiscovery()::discoverFor, Material.IRON_INGOT, "throwing_knife"),
                rule("Wind Blades with breeze rod", new WindBladeRecipeDiscovery()::discoverFor, Material.BREEZE_ROD, "iron_wind_blade", "diamond_wind_blade"),
                rule("Netherite Wind Blade", new WindBladeRecipeDiscovery()::discoverFor, Material.NETHERITE_INGOT, "netherite_wind_blade"));
    }

    private static Arguments rule(String name, Consumer<Player> discovery, Material material, String... recipeNames) {
        return Arguments.of(name, discovery, material, List.of(recipeNames));
    }
}
