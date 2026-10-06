package pepin.pepeforge.weapons.katana;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.cooldown.CooldownManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KatanaListenerTest {

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
    void droppingKatanaRestoresTheDroppedStacksNormalVisual() {
        ItemFactory itemFactory = mock(ItemFactory.class);
        KatanaListener listener = new KatanaListener(
                plugin,
                itemFactory,
                mock(PluginLang.class),
                mock(CooldownManager.class));
        Player player = MockBukkit.getMock().addPlayer();
        Item droppedEntity = mock(Item.class);
        ItemStack droppedStack = new ItemStack(Material.DIAMOND_SWORD);
        when(droppedEntity.getItemStack()).thenReturn(droppedStack);
        when(itemFactory.isKatana(droppedStack)).thenReturn(true);
        when(itemFactory.hasKatanaParryVisual(droppedStack)).thenReturn(true);

        listener.onDrop(new PlayerDropItemEvent(player, droppedEntity));

        verify(itemFactory).setKatanaParryVisual(droppedStack, false);
        verify(droppedEntity).setItemStack(droppedStack);
    }
}
