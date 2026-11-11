package pepin.pepeforge.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.itemconfig.obsidianglaive.ObsidianGlaiveConfigListener;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.item.ItemIds;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.weapons.obsidianglaive.ObsidianGlaiveSettings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObsidianGlaiveConfigTest {

    private static final String PATH = "mechanics.obsidian_glaive.";

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Test
    void itemMenuShowsAvailabilityRecipeAndAllGlaiveControls() {
        PepeForgePlugin plugin = mock(PepeForgePlugin.class);
        ItemFactory factory = mock(ItemFactory.class);
        FileConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        PluginLang lang = mock(PluginLang.class);
        when(plugin.getPluginLang()).thenReturn(lang);
        when(lang.text(anyString())).thenAnswer(call -> call.getArgument(0));
        when(lang.text("messages.config.obsidian_glaive.damage.title"))
                .thenReturn("Damage {damage} HP");
        when(factory.isItemEnabled(ItemIds.OBSIDIAN_GLAIVE)).thenReturn(true);
        when(factory.isRecipeEnabled(ItemIds.OBSIDIAN_GLAIVE)).thenReturn(true);

        Inventory inventory = ItemConfigMenu.create(ItemIds.OBSIDIAN_GLAIVE, factory, plugin);

        assertNotNull(inventory.getItem(11));
        assertNotNull(inventory.getItem(15));
        assertNotNull(inventory.getItem(18));
        assertTrue(inventory.getItem(19).getItemMeta().getDisplayName().contains("7.0 HP"));
        assertNotNull(inventory.getItem(20));
        assertNotNull(inventory.getItem(21));
        assertNotNull(inventory.getItem(26));
    }

    @Test
    void damageControlClampsAndChangesByHalfHp() {
        PepeForgePlugin plugin = configuredPlugin();
        ObsidianGlaiveConfigListener listener = new ObsidianGlaiveConfigListener(plugin);

        assertTrue(listener.handleClick(click(19, true, false)));
        assertEquals(6.5D, plugin.getConfig().getDouble(PATH + "damage"));
        plugin.getConfig().set(PATH + "damage", ObsidianGlaiveSettings.MAX_DAMAGE);
        assertFalse(listener.handleClick(click(19, false, true)));
        assertEquals(ObsidianGlaiveSettings.MAX_DAMAGE, plugin.getConfig().getDouble(PATH + "damage"));
        plugin.getConfig().set(PATH + "damage", ObsidianGlaiveSettings.MIN_DAMAGE);
        assertFalse(listener.handleClick(click(19, true, false)));
        assertEquals(ObsidianGlaiveSettings.MIN_DAMAGE, plugin.getConfig().getDouble(PATH + "damage"));
        verify(plugin, times(1)).saveConfig();
    }

    @Test
    void togglesAndResetOnlyChangeGlaiveMechanics() {
        PepeForgePlugin plugin = configuredPlugin();
        FileConfiguration config = plugin.getConfig();
        config.set("mechanics.stormcleaver.charges_required", 8);
        config.set("items.obsidian_glaive.enabled", false);
        ObsidianGlaiveConfigListener listener = new ObsidianGlaiveConfigListener(plugin);

        listener.handleClick(click(20, false, true));
        listener.handleClick(click(21, false, true));
        assertFalse(config.getBoolean(PATH + "stab_enabled"));
        assertFalse(config.getBoolean(PATH + "stab_lift_enabled"));
        listener.handleClick(click(18, true, false));

        assertEquals(ObsidianGlaiveSettings.DEFAULT_DAMAGE, config.getDouble(PATH + "damage"));
        assertTrue(config.getBoolean(PATH + "stab_enabled"));
        assertTrue(config.getBoolean(PATH + "stab_lift_enabled"));
        assertFalse(config.getBoolean("items.obsidian_glaive.enabled"));
        assertEquals(8, config.getInt("mechanics.stormcleaver.charges_required"));
        verify(plugin, times(3)).saveConfig();
        assertFalse(listener.handleClick(click(18, true, false)));
        verify(plugin, times(3)).saveConfig();
    }

    @Test
    void settingsLoadDefaultsAndClampInvalidDamage() {
        FileConfiguration config = new YamlConfiguration();
        ObsidianGlaiveSettings defaults = ObsidianGlaiveSettings.fromConfig(config);
        assertEquals(ObsidianGlaiveSettings.DEFAULT_DAMAGE, defaults.damage());
        assertTrue(defaults.stabEnabled());
        assertTrue(defaults.stabLiftEnabled());

        config.set(PATH + "damage", Double.POSITIVE_INFINITY);
        ObsidianGlaiveSettings nonFinite = ObsidianGlaiveSettings.fromConfig(config);
        assertEquals(ObsidianGlaiveSettings.DEFAULT_DAMAGE, nonFinite.damage());
        config.set(PATH + "damage", 100.0D);
        assertEquals(ObsidianGlaiveSettings.MAX_DAMAGE,
                ObsidianGlaiveSettings.fromConfig(config).damage());
        config.set(PATH + "damage", 0.0D);
        assertEquals(ObsidianGlaiveSettings.MIN_DAMAGE,
                ObsidianGlaiveSettings.fromConfig(config).damage());
    }

    private static PepeForgePlugin configuredPlugin() {
        PepeForgePlugin plugin = mock(PepeForgePlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getPluginLang()).thenReturn(mock(PluginLang.class));
        return plugin;
    }

    private static InventoryClickEvent click(int slot, boolean left, boolean right) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Glaive config");
        inventory.setItem(slot, new ItemStack(Material.STICK));
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getSlot()).thenReturn(slot);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getCurrentItem()).thenReturn(inventory.getItem(slot));
        when(event.isLeftClick()).thenReturn(left);
        when(event.isRightClick()).thenReturn(right);
        return event;
    }
}
