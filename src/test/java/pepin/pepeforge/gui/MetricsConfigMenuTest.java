package pepin.pepeforge.gui;

import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsConfigMenuTest {
    private PepeForgePlugin plugin;
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
        player = mock(Player.class);
        config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("pepeforge.config")).thenReturn(true);
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.getAllCanonicalIds()).thenReturn(List.of());
        PluginLang lang = mock(PluginLang.class);
        when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
        when(lang.message(anyString())).thenAnswer(call -> call.getArgument(0));
        doAnswer(call -> {
            config.set("metrics.enabled", call.getArgument(0, Boolean.class));
            return null;
        }).when(plugin).setMetricsEnabled(anyBoolean());
        listener = new ConfigMenuListener(plugin, factory, lang);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenAnswer(call -> inventory);
        when(player.getOpenInventory()).thenReturn(view);
        doAnswer(call -> {
            inventory = call.getArgument(0);
            return view;
        }).when(player).openInventory(any(Inventory.class));
        listener.open(player);
        clearInvocations(player);
    }

    @Test
    void authorizedClickTogglesInPlaceWithoutReloadOrReloadReminder() {
        when(player.hasPermission("pepeforge.config")).thenReturn(true);
        assertState(true);

        InventoryClickEvent event = click(inventory);
        listener.onClick(event);
        verify(event).setCancelled(true);
        verify(plugin).setMetricsEnabled(false);
        assertState(false);

        listener.onClick(click(inventory));
        verify(plugin).setMetricsEnabled(true);
        assertState(true);

        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getPlayer()).thenReturn(player);
        when(close.getInventory()).thenReturn(inventory);
        listener.onClose(close);
        verify(plugin, never()).reloadPlugin();
        verify(player, never()).openInventory(any(Inventory.class));
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void staleMenuCannotChangeMetricsAfterPermissionIsRemoved() {
        when(player.hasPermission("pepeforge.config")).thenReturn(false);
        listener.onClick(click(inventory));

        verify(plugin, never()).setMetricsEnabled(anyBoolean());
        verify(player).sendMessage("messages.command.no_permission");
        assertState(true);
    }

    @Test
    void bottomInventoryClickCannotToggleMetrics() {
        when(player.hasPermission("pepeforge.config")).thenReturn(true);
        listener.onClick(click(mock(Inventory.class)));
        verify(plugin, never()).setMetricsEnabled(anyBoolean());
    }

    @Test
    void savedOptOutIsShownWhenOpeningTheMenu() {
        config.set("metrics.enabled", false);
        ConfigMenu.updateMetricsButton(inventory, plugin, mockLanguage());
        assertState(false);
    }

    private void assertState(boolean enabled) {
        var button = inventory.getItem(ConfigMenu.METRICS_SLOT);
        assertEquals(Material.BLACK_BANNER, button.getType());
        BannerMeta meta = (BannerMeta) button.getItemMeta();
        assertEquals(enabled ? DyeColor.GREEN : DyeColor.RED, meta.getPattern(0).getColor());
    }

    private PluginLang mockLanguage() {
        PluginLang lang = mock(PluginLang.class);
        when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
        return lang;
    }

    private InventoryClickEvent click(Inventory clickedInventory) {
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClickedInventory()).thenReturn(clickedInventory);
        when(event.getSlot()).thenReturn(ConfigMenu.METRICS_SLOT);
        when(event.getCurrentItem()).thenReturn(inventory.getItem(ConfigMenu.METRICS_SLOT));
        return event;
    }
}
