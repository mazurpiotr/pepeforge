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

class IceRapierConfigTest {

    private static final String PATH = "mechanics.ice_rapier.";
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
        when(factory.isItemEnabled(ItemIds.ICE_RAPIER)).thenReturn(true);
        when(factory.isRecipeEnabled(ItemIds.ICE_RAPIER)).thenReturn(true);
        when(factory.getAllCanonicalIds()).thenReturn(List.of(ItemIds.ICE_RAPIER));
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        when(factory.createByName(ItemIds.ICE_RAPIER)).thenReturn(item);
        when(factory.getItemId(item)).thenReturn(ItemIds.ICE_RAPIER);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenAnswer(call -> inventory);
        when(player.getOpenInventory()).thenReturn(view);
        doAnswer(call -> {
            inventory = call.getArgument(0);
            return view;
        }).when(player).openInventory(any(Inventory.class));
        PluginLang lang = mock(PluginLang.class);
        when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
        when(lang.message("messages.config.reload_reminder")).thenReturn("reload reminder");
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
    void menuKeepsCommonButtonsAndUsesOnlyIceSettings() {
        for (int slot : new int[] {11, 15, 18, 19, 20, 21, 22, 26}) {
            assertNotNull(inventory.getItem(slot), "Missing control in slot " + slot);
        }
        assertTrue(inventory.getItem(18).getItemMeta().getDisplayName().contains("Reset Ice Rapier Defaults"));
        assertTrue(inventory.getItem(19).getItemMeta().getDisplayName().contains("4.00 HP"));
        assertTrue(inventory.getItem(20).getItemMeta().getDisplayName().contains("8.00 HP"));
        assertTrue(inventory.getItem(22).getItemMeta().getLore().getFirst().contains("not a duration"));
        for (int slot : new int[] {13, 23, 24, 25}) {
            assertNull(inventory.getItem(slot));
        }
        assertTrue(ItemConfigMenu.isRapier(ItemIds.ICE_RAPIER));
        assertTrue(ItemConfigMenu.hasRecipeToggle(ItemIds.ICE_RAPIER));
    }

    @ParameterizedTest
    @MethodSource("settings")
    void clicksUseIcePathsAndRespectBounds(int slot, String key, double initial, double minimum, double maximum) {
        listener.onClick(click(slot, false, true));
        assertEquals(initial + 1, config.getDouble(PATH + key), 1.0E-8);
        config.set(PATH + "min_damage", 1D);
        config.set(PATH + "max_damage", 40D);
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
    void endpointEditsCannotInvertDamageRange() {
        config.set(PATH + "min_damage", 8D);
        config.set(PATH + "max_damage", 8D);
        listener.onClick(click(19, false, true));
        assertEquals(8D, config.getDouble(PATH + "min_damage"));
        listener.onClick(click(20, true, false));
        assertEquals(8D, config.getDouble(PATH + "max_damage"));
        config.set(PATH + "min_damage", 35D);
        config.set(PATH + "max_damage", 2D);
        listener.onClick(click(20, true, false));
        assertTrue(config.getDouble(PATH + "max_damage") >= config.getDouble(PATH + "min_damage"));
    }

    @Test
    void resetRestoresOnlyIceDefaults() {
        config.set(PATH + "min_damage", 10D);
        config.set(PATH + "max_damage", 20D);
        config.set(PATH + "max_durability", 100);
        config.set(PATH + "freeze_ticks", 400);
        config.set("items.ice_rapier.enabled", false);
        config.set("items.ice_rapier.recipe_enabled", false);
        config.set("mechanics.rapier.damage.iron", 30D);
        config.set("mechanics.rapier.attack_speed", 3D);
        config.set("mechanics.rapier.frontal_bonus_enabled", true);
        listener.onClick(click(18, true, false));
        assertEquals(4D, config.getDouble(PATH + "min_damage"));
        assertEquals(8D, config.getDouble(PATH + "max_damage"));
        assertEquals(32, config.getInt(PATH + "max_durability"));
        assertEquals(200, config.getInt(PATH + "freeze_ticks"));
        assertFalse(config.getBoolean("items.ice_rapier.enabled"));
        assertFalse(config.getBoolean("items.ice_rapier.recipe_enabled"));
        assertEquals(30D, config.getDouble("mechanics.rapier.damage.iron"));
        assertEquals(3D, config.getDouble("mechanics.rapier.attack_speed"));
        assertTrue(config.getBoolean("mechanics.rapier.frontal_bonus_enabled"));
        verify(plugin).saveConfig();
    }

    @Test
    void availabilityAndRecipeRemainIndependentAndChangesRemindOnClose() {
        listener.onClick(click(11, true, false));
        assertFalse(config.getBoolean("items.ice_rapier.enabled"));
        assertFalse(config.contains("items.ice_rapier.recipe_enabled"));
        listener.onClick(click(15, false, true));
        assertFalse(config.getBoolean("items.ice_rapier.recipe_enabled"));
        listener.onClick(click(22, false, true));
        org.bukkit.event.inventory.InventoryCloseEvent close = mock(org.bukkit.event.inventory.InventoryCloseEvent.class);
        when(close.getPlayer()).thenReturn(player);
        when(close.getInventory()).thenReturn(inventory);
        listener.onClose(close);
        verify(player).sendMessage("reload reminder");
    }

    @Test
    void unrelatedClicksAndRevokedPermissionDoNotSave() {
        listener.onClick(click(19, false, false));
        listener.onClick(click(24, false, true));
        when(player.hasPermission("pepeforge.config")).thenReturn(false);
        listener.onClick(click(19, false, true));
        verify(plugin, never()).saveConfig();
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
                Arguments.of(19, "min_damage", 4D, 1D, 40D),
                Arguments.of(20, "max_damage", 8D, 1D, 40D),
                Arguments.of(21, "max_durability", 32D, 1D, 250D),
                Arguments.of(22, "freeze_ticks", 200D, 1D, 600D));
    }
}
