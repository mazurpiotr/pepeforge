package pepin.pepeforge.weapons.solarshield;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ui.BossBarManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolarShieldListenerTest {

    private static JavaPlugin plugin;

    @BeforeAll
    static void setUpServer() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Test
    void offHandSwapDoesNotUseTheHotbarButtonAsAnInventoryIndex() {
        ItemFactory itemFactory = mock(ItemFactory.class);
        SolarShieldListener listener = new SolarShieldListener(
                plugin,
                itemFactory,
                mock(PluginLang.class),
                mock(BossBarManager.class));

        Inventory topInventory = mock(Inventory.class);
        Inventory bottomInventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        Player player = mock(Player.class);
        PlayerInventory playerInventory = mock(PlayerInventory.class);
        InventoryClickEvent event = mock(InventoryClickEvent.class);

        when(topInventory.getType()).thenReturn(InventoryType.CHEST);
        when(event.getClickedInventory()).thenReturn(topInventory);
        when(event.getView()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(topInventory);
        when(view.getBottomInventory()).thenReturn(bottomInventory);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getAction()).thenReturn(InventoryAction.HOTBAR_SWAP);
        when(event.getClick()).thenReturn(ClickType.SWAP_OFFHAND);
        when(event.getHotbarButton()).thenReturn(-1);
        when(player.getInventory()).thenReturn(playerInventory);

        assertDoesNotThrow(() -> listener.onInventoryClick(event));

        verify(playerInventory).getItemInOffHand();
        verify(bottomInventory, never()).getItem(-1);
        verify(event, never()).getHotbarButton();
    }
}
