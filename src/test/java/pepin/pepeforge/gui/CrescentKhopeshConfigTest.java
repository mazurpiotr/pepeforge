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
import pepin.pepeforge.weapons.crescentkhopesh.MoonstrikeMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrescentKhopeshConfigTest {

    private static final String PATH = "mechanics.crescent_khopesh.";
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
        config.set(MoonstrikeMode.CONFIG_PATH, MoonstrikeMode.WAVE.name());
        when(plugin.getConfig()).thenReturn(config);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("pepeforge.config")).thenReturn(true);
        when(factory.isItemEnabled(ItemIds.CRESCENT_KHOPESH)).thenReturn(true);
        when(factory.isRecipeEnabled(ItemIds.CRESCENT_KHOPESH)).thenReturn(true);
        when(factory.getAllCanonicalIds()).thenReturn(List.of(ItemIds.CRESCENT_KHOPESH));
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        when(factory.createByName(ItemIds.CRESCENT_KHOPESH)).thenReturn(item);
        when(factory.getItemId(item)).thenReturn(ItemIds.CRESCENT_KHOPESH);
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
    void shieldLimitShowsHeartsWhileSavingHp() {
        listener.onClick(click(20, false, true));
        assertEquals(5.0D, config.getDouble(PATH + "shield_absorption"));
        assertTrue(inventory.getItem(20).getItemMeta().getDisplayName().contains("2.50 hearts"));
        assertTrue(inventory.getItem(20).getItemMeta().getLore().stream()
                .anyMatch(line -> line.contains("Right-Click: +0.50 hearts")));
    }

    @Test
    void menuIncludesEveryMechanicAndKeepsAvailabilityRecipeAndNavigationButtons() {
        for (int slot = 18; slot <= 23; slot++) {
            assertNotNull(inventory.getItem(slot), "Missing control in slot " + slot);
        }
        assertNotNull(inventory.getItem(11));
        assertNotNull(inventory.getItem(15));
        assertNotNull(inventory.getItem(26));
        assertTrue(inventory.getItem(23).getItemMeta().getDisplayName().contains("0.65 blocks/tick"));
        assertTrue(inventory.getItem(20).getItemMeta().getDisplayName().contains("2.00 hearts"));
        assertTrue(inventory.getItem(22).getItemMeta().getDisplayName().contains("2.80 blocks"));
        assertTrue(inventory.getItem(24).getItemMeta().getDisplayName().contains("WAVE"));
        assertNull(inventory.getItem(25));
    }

    @ParameterizedTest
    @MethodSource("settings")
    void mechanicClicksReachTheOwningConfigPathAndRefreshTheMenu(
            int slot, String key, double defaultValue, double step, double min, double max) {
        String previousTitle = inventory.getItem(slot).getItemMeta().getDisplayName();
        InventoryClickEvent event = click(slot, false, true);
        listener.onClick(event);
        assertEquals(defaultValue + step, config.getDouble(PATH + key), 1.0E-8);
        verify(plugin).saveConfig();
        verify(player, never()).openInventory(any(Inventory.class));
        assertNotEquals(previousTitle, inventory.getItem(slot).getItemMeta().getDisplayName());
        verify(event).setCancelled(true);
    }

    @ParameterizedTest
    @MethodSource("settings")
    void mechanicControlsCannotEscapeRuntimeBounds(
            int slot, String key, double defaultValue, double step, double min, double max) {
        config.set(PATH + key, min);
        listener.onClick(click(slot, true, false));
        assertEquals(min, config.getDouble(PATH + key), 1.0E-8);
        config.set(PATH + key, max);
        listener.onClick(click(slot, false, true));
        assertEquals(max, config.getDouble(PATH + key), 1.0E-8);
        verify(plugin, times(2)).saveConfig();
    }

    @Test
    void resetRestoresBothVariantsAndDefaultModeAndPreservesOtherConfiguration() {
        config.set(PATH + "hits_required", 5);
        config.set(PATH + "wave_width", 6.0D);
        config.set(PATH + "wave_range", 9.0D);
        config.set(PATH + "wave_speed", 1.0D);
        config.set(PATH + "shield_absorption", 10.0D);
        config.set(PATH + "sweep_width", 8.0D);
        config.set(PATH + "sweep_range", 7.0D);
        config.set("items.crescent_khopesh.enabled", false);
        config.set("items.crescent_khopesh.recipe_enabled", false);
        config.set("mechanics.stormcleaver.charges_required", 8);

        listener.onClick(click(18, true, false));

        assertEquals(3, config.get(PATH + "hits_required"));
        assertEquals(2.8D, config.getDouble(PATH + "wave_width"));
        assertEquals(4.5D, config.getDouble(PATH + "wave_range"));
        assertEquals(0.65D, config.getDouble(PATH + "wave_speed"));
        assertEquals(4.0D, config.getDouble(PATH + "shield_absorption"));
        assertEquals(3.0D, config.getDouble(PATH + "sweep_width"));
        assertEquals(2.0D, config.getDouble(PATH + "sweep_range"));
        assertEquals(MoonstrikeMode.SWEEP, MoonstrikeMode.fromConfig(config));
        assertFalse(config.getBoolean("items.crescent_khopesh.enabled"));
        assertFalse(config.getBoolean("items.crescent_khopesh.recipe_enabled"));
        assertEquals(8, config.getInt("mechanics.stormcleaver.charges_required"));
        verify(plugin).saveConfig();
    }

    @Test
    void missingOrInvalidModeUsesSweepAndShowsSweepDimensionsWithoutSpeed() {
        config.set(MoonstrikeMode.CONFIG_PATH, null);
        assertEquals(MoonstrikeMode.SWEEP, MoonstrikeMode.fromConfig(config));
        ItemConfigMenu.refresh(inventory, factory, plugin);
        assertTrue(inventory.getItem(21).getItemMeta().getDisplayName().contains("2.00 blocks"));
        assertTrue(inventory.getItem(22).getItemMeta().getDisplayName().contains("3.00 blocks"));
        assertNull(inventory.getItem(23));
        assertTrue(inventory.getItem(24).getItemMeta().getDisplayName().contains("SWEEP"));

        listener.onClick(click(23, false, true));
        assertFalse(config.contains(PATH + "wave_speed"));
        verify(plugin, never()).saveConfig();
        config.set(MoonstrikeMode.CONFIG_PATH, "unknown");
        assertEquals(MoonstrikeMode.SWEEP, MoonstrikeMode.fromConfig(config));
        config.set(MoonstrikeMode.CONFIG_PATH, " wave ");
        assertEquals(MoonstrikeMode.WAVE, MoonstrikeMode.fromConfig(config));
    }

    @Test
    void switchingModesPreservesVariantDimensionsAndRefreshesTheMenu() {
        config.set(PATH + "wave_width", 5.0D);
        config.set(PATH + "sweep_width", 4.0D);
        listener.onClick(click(24, true, false));
        assertEquals(MoonstrikeMode.SWEEP, MoonstrikeMode.fromConfig(config));
        assertTrue(inventory.getItem(22).getItemMeta().getDisplayName().contains("4.00 blocks"));
        assertNull(inventory.getItem(23));

        listener.onClick(click(24, false, true));
        assertEquals(MoonstrikeMode.WAVE, MoonstrikeMode.fromConfig(config));
        assertEquals(5.0D, config.getDouble(PATH + "wave_width"));
        assertEquals(4.0D, config.getDouble(PATH + "sweep_width"));
        verify(plugin, times(2)).saveConfig();
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @ParameterizedTest
    @MethodSource("sweepSettings")
    void sweepDimensionControlsReachTheirOwnPathsAndRespectBounds(int slot, String key, double defaultValue) {
        config.set(MoonstrikeMode.CONFIG_PATH, MoonstrikeMode.SWEEP.name());
        listener.onClick(click(slot, false, true));
        assertEquals(defaultValue + 0.1D, config.getDouble(PATH + key), 1.0E-8);
        assertFalse(config.contains(PATH + (slot == 21 ? "wave_range" : "wave_width")));
        config.set(PATH + key, 0.1D);
        listener.onClick(click(slot, true, false));
        assertEquals(0.1D, config.getDouble(PATH + key), 1.0E-8);
        config.set(PATH + key, 16.0D);
        listener.onClick(click(slot, false, true));
        assertEquals(16.0D, config.getDouble(PATH + key), 1.0E-8);
        verify(plugin, times(3)).saveConfig();
    }

    static Stream<Arguments> sweepSettings() {
        return Stream.of(Arguments.of(21, "sweep_range", 2.0D), Arguments.of(22, "sweep_width", 3.0D));
    }

    @Test
    void nonFiniteValuesRecoverAndUnrelatedClickTypesDoNotChangeSettings() {
        config.set(PATH + "wave_speed", Double.NaN);
        listener.onClick(click(23, false, true));
        assertEquals(0.7D, config.getDouble(PATH + "wave_speed"), 1.0E-8);
        listener.onClick(click(23, false, false));
        listener.onClick(click(5, false, true));
        assertEquals(0.7D, config.getDouble(PATH + "wave_speed"), 1.0E-8);
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
                Arguments.of(19, "hits_required", 3.0D, 1.0D, 1.0D, 100.0D),
                Arguments.of(20, "shield_absorption", 4.0D, 1.0D, 0.0D, 40.0D),
                Arguments.of(21, "wave_range", 4.5D, 0.1D, 0.1D, 16.0D),
                Arguments.of(22, "wave_width", 2.8D, 0.1D, 0.1D, 16.0D),
                Arguments.of(23, "wave_speed", 0.65D, 0.05D, 0.05D, 16.0D));
    }
}
