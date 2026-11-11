package pepin.pepeforge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigMenuSessionTest {
    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void navigationIsDeferredAndRapidClicksHaveOnlyOneEffectiveTransition() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            Inventory main = c.inventory;
            c.click(0);
            Pending first = f.latest();
            c.click(0);
            assertTrue(first.cancelled);
            assertSame(main, c.inventory);
            clearInvocations(c.player);

            first.run.run();
            verify(c.player, never()).openInventory(any(Inventory.class));
            f.latest().run.run();
            assertEquals(ItemIds.KATANA, ItemConfigMenu.getItemId(c.inventory));
            verify(c.player).openInventory(any(Inventory.class));
        }
    }

    @Test
    void dirtySessionSurvivesNavigationAndWarnsOnceWhenLeavingASubmenu() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.enterItem();
            Inventory submenu = c.inventory;
            clearInvocations(c.player);
            c.click(11);
            assertSame(submenu, c.inventory);
            verify(c.player, never()).openInventory(any(Inventory.class));
            c.click(26);
            f.latest().run.run();
            c.enterItem();
            verify(c.player, never()).sendMessage(anyString());

            Inventory closed = c.inventory;
            c.close();
            f.listener.onClose(c.closeEvent(closed));
            verify(c.player).sendMessage("messages.config.reload_reminder");

            clearInvocations(c.player);
            f.listener.open(c.player);
            c.close();
            verify(c.player, never()).sendMessage(anyString());
        }
    }

    @Test
    void escapeCancelsPendingNavigationEvenIfTheOldInventoryIsReopened() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            Inventory original = c.inventory;
            c.click(0);
            Pending pending = f.latest();
            c.close();
            c.inventory = original;
            clearInvocations(c.player);
            pending.run.run();
            assertTrue(pending.cancelled);
            verifyNoInteractions(c.player);
        }
    }

    @Test
    void staleCloseAndCallbackCannotAffectANewVisit() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            Inventory original = c.inventory;
            c.click(0);
            Pending old = f.latest();
            c.close();
            f.listener.open(c.player);
            c.enterItem();
            c.click(11);
            Inventory current = c.inventory;
            f.listener.onClose(c.closeEvent(original));
            old.run.run();
            assertSame(current, c.inventory);
            c.close();
            verify(c.player).sendMessage("messages.config.reload_reminder");
        }
    }

    @Test
    void quitAndRetirementReleaseSessionsWithoutAccessingRetiredPlayers() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.enterItem();
            c.click(11);
            c.click(26);
            Pending pending = f.latest();
            PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
            when(quit.getPlayer()).thenReturn(c.player);
            f.listener.onQuit(quit);
            clearInvocations(c.player);
            pending.run.run();
            assertTrue(pending.cancelled);
            verifyNoInteractions(c.player);

            f.listener.open(c.player);
            c.click(0);
            Pending retired = f.latest();
            clearInvocations(c.player);
            retired.retired.run();
            retired.run.run();
            verifyNoInteractions(c.player);
        }
    }

    @Test
    void permissionRemovalAndOfflineStatePreventDeferredActions() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.click(0);
            when(c.player.hasPermission("pepeforge.config")).thenReturn(false);
            clearInvocations(c.player);
            f.latest().run.run();
            verify(c.player, never()).openInventory(any(Inventory.class));
            verify(c.player).sendMessage("messages.command.no_permission");

            when(c.player.hasPermission("pepeforge.config")).thenReturn(true);
            c.click(0);
            when(c.player.isOnline()).thenReturn(false);
            clearInvocations(c.player);
            f.latest().run.run();
            verify(c.player, never()).getOpenInventory();
            verify(c.player, never()).sendMessage(anyString());
        }
    }

    @Test
    void unexpectedViewCannotBeReplacedByAPendingAction() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.click(0);
            c.inventory = Bukkit.createInventory(null, 9);
            clearInvocations(c.player);
            f.latest().run.run();
            verify(c.player, never()).openInventory(any(Inventory.class));
        }
    }

    @Test
    void cancelledOpeningEndsDirtySessionWhenTheSourceWasClosed() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.enterItem();
            c.click(11);
            c.rejectOpening = true;
            c.click(26);
            f.latest().run.run();
            verify(c.player).sendMessage("messages.config.reload_reminder");
            c.close();
            verify(c.player).sendMessage("messages.config.reload_reminder");
        }
    }

    @Test
    void guiReloadIsDeferredAndReloadPermissionIsCheckedAgain() {
        try (Fixture f = new Fixture()) {
            Client c = f.client();
            c.click(53);
            verify(f.plugin, never()).reloadPlugin();
            when(c.player.hasPermission("pepeforge.reload")).thenReturn(false);
            f.latest().run.run();
            verify(f.plugin, never()).reloadPlugin();
            verify(c.player).sendMessage("messages.command.no_permission");

            when(c.player.hasPermission("pepeforge.reload")).thenReturn(true);
            when(f.plugin.reloadPlugin()).thenAnswer(call -> {
                f.listener.stop();
                return true;
            });
            c.click(53);
            f.latest().run.run();
            verify(f.plugin).reloadPlugin();
            verify(c.player).sendMessage("messages.config.reloaded");
        }
    }

    @Test
    void runtimeStopInvalidatesAllPlayersAndForeignCleanupPreservesANewGui() {
        try (Fixture f = new Fixture()) {
            Client a = f.client();
            Client b = f.client();
            a.enterItem();
            a.click(11);
            a.click(26);
            Pending pending = f.latest();
            Inventory stale = a.inventory;
            f.owned = false;
            f.listener.stop();
            List<Pending> cleanup = new ArrayList<>(f.tasks.subList(f.tasks.size() - 2, f.tasks.size()));
            assertTrue(pending.cancelled);

            f.listener = new ConfigMenuListener(f.plugin, f.factory, f.lang);
            f.listener.open(a.player);
            Inventory replacement = a.inventory;
            clearInvocations(a.player, b.player);
            pending.run.run();
            cleanup.forEach(task -> task.run.run());
            assertSame(replacement, a.inventory);
            verify(a.player, never()).closeInventory();
            verify(b.player).closeInventory();
            verify(a.player, never()).sendMessage(anyString());

            InventoryClickEvent staleClick = a.clickEvent(stale, 11);
            f.listener.onClick(staleClick);
            verify(staleClick).setCancelled(true);
            assertFalse(f.config.getBoolean("items.katana.enabled", true));
        }
    }

    @Test
    void dirtyFlagsAreIndependentForPlayersInDifferentRegions() {
        try (Fixture f = new Fixture()) {
            Client a = f.client();
            Client b = f.client();
            a.enterItem();
            a.click(11);
            b.close();
            a.close();
            verify(a.player).sendMessage("messages.config.reload_reminder");
            verify(b.player, never()).sendMessage(anyString());
        }
    }

    private static final class Pending {
        private final Runnable run;
        private final Runnable retired;
        private boolean cancelled;

        private Pending(Runnable run, Runnable retired) {
            this.run = run;
            this.retired = retired;
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final PepeForgePlugin plugin = mock(PepeForgePlugin.class);
        private final ItemFactory factory = mock(ItemFactory.class);
        private final PluginLang lang = mock(PluginLang.class);
        private final YamlConfiguration config = new YamlConfiguration();
        private final List<Pending> tasks = new ArrayList<>();
        private final MockedStatic<SchedulerCompat> scheduler = mockStatic(SchedulerCompat.class);
        private ConfigMenuListener listener;
        private boolean owned = true;

        private Fixture() {
            when(plugin.getConfig()).thenReturn(config);
            when(plugin.isEnabled()).thenReturn(true);
            when(factory.getAllCanonicalIds()).thenReturn(List.of(ItemIds.KATANA));
            when(factory.createByName(ItemIds.KATANA)).thenAnswer(call -> new ItemStack(Material.DIAMOND_SWORD));
            when(factory.getItemId(any(ItemStack.class))).thenReturn(ItemIds.KATANA);
            when(factory.isItemEnabled(ItemIds.KATANA)).thenAnswer(call -> config.getBoolean("items.katana.enabled", true));
            when(factory.isRecipeEnabled(ItemIds.KATANA)).thenReturn(true);
            when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
            when(lang.message(anyString())).thenAnswer(call -> call.getArgument(0));
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(
                    any(Player.class), eq(plugin), any(Runnable.class), any(Runnable.class), eq(1L)))
                    .thenAnswer(call -> schedule(call.getArgument(2), call.getArgument(3)));
            scheduler.when(() -> SchedulerCompat.runLaterForPlayer(
                    any(Player.class), eq(plugin), any(Runnable.class), eq(1L)))
                    .thenAnswer(call -> schedule(call.getArgument(2), null));
            scheduler.when(() -> SchedulerCompat.isOwnedByCurrentRegion(any(Entity.class)))
                    .thenAnswer(call -> owned);
            listener = new ConfigMenuListener(plugin, factory, lang);
        }

        private ScheduledTaskCompat schedule(Runnable action, Runnable retired) {
            Pending pending = new Pending(action, retired);
            tasks.add(pending);
            return () -> pending.cancelled = true;
        }

        private Pending latest() {
            return tasks.getLast();
        }

        private Client client() {
            Client client = new Client(this);
            listener.open(client.player);
            return client;
        }

        @Override
        public void close() {
            listener.stop();
            scheduler.close();
        }
    }

    private static final class Client {
        private final Fixture fixture;
        private final Player player = mock(Player.class);
        private final InventoryView view = mock(InventoryView.class);
        private Inventory inventory = Bukkit.createInventory(null, 9);
        private boolean rejectOpening;

        private Client(Fixture fixture) {
            this.fixture = fixture;
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            when(player.hasPermission("pepeforge.config")).thenReturn(true);
            when(player.hasPermission("pepeforge.reload")).thenReturn(true);
            when(view.getTopInventory()).thenAnswer(call -> inventory);
            when(player.getOpenInventory()).thenReturn(view);
            doAnswer(call -> {
                fixture.listener.onClose(closeEvent(inventory));
                inventory = rejectOpening ? Bukkit.createInventory(null, 9) : call.getArgument(0);
                rejectOpening = false;
                return view;
            }).when(player).openInventory(any(Inventory.class));
            doAnswer(call -> {
                close();
                return null;
            }).when(player).closeInventory();
        }

        private void enterItem() {
            click(0);
            fixture.latest().run.run();
        }

        private void click(int slot) {
            fixture.listener.onClick(clickEvent(inventory, slot));
        }

        private InventoryClickEvent clickEvent(Inventory source, int slot) {
            InventoryView clickedView = mock(InventoryView.class);
            when(clickedView.getTopInventory()).thenReturn(source);
            InventoryClickEvent event = mock(InventoryClickEvent.class);
            when(event.getWhoClicked()).thenReturn(player);
            when(event.getView()).thenReturn(clickedView);
            when(event.getClickedInventory()).thenReturn(source);
            when(event.getCurrentItem()).thenReturn(source.getItem(slot));
            when(event.getSlot()).thenReturn(slot);
            when(event.isLeftClick()).thenReturn(true);
            return event;
        }

        private InventoryCloseEvent closeEvent(Inventory source) {
            InventoryCloseEvent event = mock(InventoryCloseEvent.class);
            when(event.getPlayer()).thenReturn(player);
            when(event.getInventory()).thenReturn(source);
            return event;
        }

        private void close() {
            fixture.listener.onClose(closeEvent(inventory));
            inventory = Bukkit.createInventory(null, 9);
        }
    }
}
