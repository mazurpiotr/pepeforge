package pepin.pepeforge.gui;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RapierConfigTest {

    private static final String PATH = "mechanics.rapier.";
    private PepeForgePlugin plugin;
    private ItemFactory factory;
    private Player player;
    private YamlConfiguration config;
    private ConfigMenuListener listener;
    private Inventory inventory;

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setup() {
        plugin = mock(PepeForgePlugin.class);
        factory = mock(ItemFactory.class);
        player = mock(Player.class);
        config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("pepeforge.config")).thenReturn(true);
        when(factory.isItemEnabled(ItemIds.IRON_RAPIER)).thenReturn(true);
        when(factory.isRecipeEnabled(ItemIds.IRON_RAPIER)).thenReturn(true);
        when(factory.getAllCanonicalIds()).thenReturn(List.of(ItemIds.IRON_RAPIER));
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        when(factory.createByName(ItemIds.IRON_RAPIER)).thenReturn(item);
        when(factory.getItemId(item)).thenReturn(ItemIds.IRON_RAPIER);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenAnswer(call -> inventory);
        when(player.getOpenInventory()).thenReturn(view);
        doAnswer(call -> {
            inventory = call.getArgument(0);
            return view;
        }).when(player).openInventory(any(Inventory.class));
        PluginLang lang = mock(PluginLang.class);
        when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
        listener = new ConfigMenuListener(plugin, factory, lang);
        AtomicReference<Runnable> navigation = new AtomicReference<>();
        try (var scheduler = mockStatic(SchedulerCompat.class)) {
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(
                    eq(player), eq(plugin), any(Runnable.class), any(Runnable.class), eq(1L)))
                    .thenAnswer(call -> {
                        navigation.set(call.getArgument(2));
                        return (ScheduledTaskCompat) () -> { };
                    });
            listener.open(player);
            listener.onClick(click(0, false, true));
            assertNotNull(navigation.get());
            navigation.get().run();
        }
        clearInvocations(player, plugin);
    }

    @Test
    void menuKeepsCommonButtonsAndExposesEveryFamilySetting() {
        for (int slot : new int[] {11, 13, 15, 18, 19, 20, 21, 22, 24, 25, 26}) {
            assertNotNull(inventory.getItem(slot), "Missing control in slot " + slot);
        }
        assertTrue(inventory.getItem(19).getItemMeta().getDisplayName().contains("5.00 HP"));
        assertTrue(inventory.getItem(22).getItemMeta().getDisplayName().contains("2.00 attacks/s"));
        assertNull(inventory.getItem(23));
        for (String id : List.of(ItemIds.IRON_RAPIER, ItemIds.DIAMOND_RAPIER, ItemIds.NETHERITE_RAPIER)) {
            assertTrue(ItemConfigMenu.isRapier(id));
            assertTrue(ItemConfigMenu.hasRecipeToggle(id));
        }
    }

    @ParameterizedTest
    @MethodSource("settings")
    void clicksReachFamilyPathsAndRespectBounds(int slot, String key, double initial, double step,
            double minimum, double maximum) {
        listener.onClick(click(slot, false, true));
        assertEquals(initial + step, config.getDouble(PATH + key), 1.0E-8);
        config.set(PATH + key, minimum);
        listener.onClick(click(slot, true, false));
        assertEquals(minimum, config.getDouble(PATH + key), 1.0E-8);
        config.set(PATH + key, maximum);
        listener.onClick(click(slot, false, true));
        assertEquals(maximum, config.getDouble(PATH + key), 1.0E-8);
        verify(plugin, times(3)).saveConfig();
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void resetRestoresAllFamilyDefaultsAndPreservesAvailabilityAndOtherMechanics() {
        config.set(PATH + "damage.iron", 30D);
        config.set(PATH + "attack_speed", 4D);
        config.set(PATH + "frontal_bonus_percent", 80D);
        config.set(PATH + "frontal_cone_degrees", 30D);
        listener.onClick(click(13, false, true));
        assertTrue(config.getBoolean(PATH + "frontal_bonus_enabled"));
        config.set("items.iron_rapier.enabled", false);
        config.set("items.iron_rapier.recipe_enabled", false);
        config.set("mechanics.greatsword.micro_dash_enabled", false);
        clearInvocations(plugin);
        listener.onClick(click(18, true, false));
        assertEquals(5D, config.getDouble(PATH + "damage.iron"));
        assertEquals(6D, config.getDouble(PATH + "damage.diamond"));
        assertEquals(7D, config.getDouble(PATH + "damage.netherite"));
        assertEquals(2D, config.getDouble(PATH + "attack_speed"));
        assertFalse(config.contains(PATH + "stab_duration_ticks"));
        assertEquals(20D, config.getDouble(PATH + "frontal_bonus_percent"));
        assertEquals(120D, config.getDouble(PATH + "frontal_cone_degrees"));
        assertFalse(config.getBoolean(PATH + "frontal_bonus_enabled"));
        assertFalse(config.getBoolean("items.iron_rapier.enabled"));
        assertFalse(config.getBoolean("items.iron_rapier.recipe_enabled"));
        assertFalse(config.getBoolean("mechanics.greatsword.micro_dash_enabled"));
        verify(plugin).saveConfig();
    }

    @Test
    void nonFiniteValuesRecoverAndUnrelatedClicksDoNotSave() {
        config.set(PATH + "attack_speed", Double.NaN);
        listener.onClick(click(22, false, true));
        assertEquals(2.1D, config.getDouble(PATH + "attack_speed"), 1.0E-8);
        listener.onClick(click(22, false, false));
        listener.onClick(click(23, false, true));
        listener.onClick(click(5, false, true));
        verify(plugin).saveConfig();
    }

    private InventoryClickEvent click(int slot, boolean left, boolean right) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        when(event.getView()).thenReturn(view);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getSlot()).thenReturn(slot);
        when(event.getCurrentItem()).thenReturn(inventory.getItem(slot));
        when(event.isLeftClick()).thenReturn(left);
        when(event.isRightClick()).thenReturn(right);
        return event;
    }

    static Stream<Arguments> settings() {
        return Stream.of(
                Arguments.of(19, "damage.iron", 5D, 1D, 1D, 40D),
                Arguments.of(20, "damage.diamond", 6D, 1D, 1D, 40D),
                Arguments.of(21, "damage.netherite", 7D, 1D, 1D, 40D),
                Arguments.of(22, "attack_speed", 2D, 0.1D, 0.5D, 4D),
                Arguments.of(24, "frontal_bonus_percent", 20D, 1D, 0D, 100D),
                Arguments.of(25, "frontal_cone_degrees", 120D, 1D, 1D, 180D));
    }
}
