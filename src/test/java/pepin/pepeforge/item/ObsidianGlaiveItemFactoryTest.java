package pepin.pepeforge.item;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.itemmeta.AttackAnimationCompat;
import pepin.pepeforge.util.itemmeta.AttackRangeCompat;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;
import pepin.pepeforge.weapons.obsidianglaive.ObsidianGlaiveDefinition;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ObsidianGlaiveItemFactoryTest {
    private JavaPlugin plugin;
    private PluginLang lang;
    private YamlConfiguration config;
    private ItemFactory factory;
    private MockedStatic<AttackAnimationCompat> animation;
    private MockedStatic<AttackRangeCompat> range;

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        lang = mock(PluginLang.class);
        config = new YamlConfiguration();
        config.set("translations.use_client_side", false);
        when(plugin.getName()).thenReturn("PepeForge");
        when(plugin.namespace()).thenReturn("pepeforge");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(lang.getItemNameForLang(anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
        when(lang.getItemLoreForLang(anyString(), anyString())).thenAnswer(call -> new ArrayList<>(
                List.of("---", "Attack", "Left-click", "One-Handed", "Rare", "---")));
        when(lang.getTextForLang(anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
        animation = mockStatic(AttackAnimationCompat.class);
        animation.when(() -> AttackAnimationCompat.apply(any(ItemStack.class), anyBoolean())).thenReturn(true);
        range = mockStatic(AttackRangeCompat.class);
        range.when(() -> AttackRangeCompat.apply(any(ItemStack.class), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), anyDouble())).thenReturn(true);
        factory = new ItemFactory(plugin, lang, null);
    }

    @AfterEach
    void closeCompatibilityMocks() {
        range.close();
        animation.close();
    }

    @Test
    void canonicalItemHasIndependentIdentityModelAndDiamondSwordStats() {
        try (MockedStatic<ItemMetaCompat> metadata = mockStatic(ItemMetaCompat.class, CALLS_REAL_METHODS)) {
            ItemStack item = factory.createByName("obsidian_glaive");

            assertNotNull(item);
            assertEquals(Material.DIAMOND_SWORD, item.getType());
            assertEquals("obsidian_glaive", factory.getItemId(item));
            assertTrue(factory.isObsidianGlaive(item));
            assertTrue(factory.getAllCanonicalIds().contains("obsidian_glaive"));
            assertTrue(factory.knownGiveNames().contains("obsidian_glaive"));
            assertEquals(7.0, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            assertEquals(1.6, 4 + modifierTotal(item, Attribute.ATTACK_SPEED), 0.000001);
            assertFalse(((Damageable) item.getItemMeta()).hasMaxDamage());
            assertEquals(0, factory.getObsidianGlaiveStage(item));
            assertTrue(factory.hasCurrentGlaiveRevision(item));
            assertNotNull(factory.getObsidianGlaiveIdentity(item));
            assertNotEquals(factory.getObsidianGlaiveIdentity(item),
                    factory.getObsidianGlaiveIdentity(factory.createObsidianGlaive()));
            metadata.verify(() -> ItemMetaCompat.setItemModel(any(ItemMeta.class),
                    eq(ObsidianGlaiveDefinition.MODEL_KEY)), atLeastOnce());
            animation.verify(() -> AttackAnimationCompat.apply(item, false));
            range.verify(() -> AttackRangeCompat.apply(item, 0.0, 4.0, 0.0, 4.0, 0.3));
        }
    }

    @Test
    void comboPreparesTwoWhacksThenNativeSpearAnimationAndReach() {
        ItemStack item = factory.createObsidianGlaive();

        factory.prepareObsidianGlaive(item, 1);
        assertEquals(1, factory.getObsidianGlaiveStage(item));
        factory.prepareObsidianGlaive(item, 2);
        assertEquals(2, factory.getObsidianGlaiveStage(item.clone()));
        animation.verify(() -> AttackAnimationCompat.apply(item, false), times(2));
        animation.verify(() -> AttackAnimationCompat.apply(item, true));
        range.verify(() -> AttackRangeCompat.apply(item, 0.0, 4.0, 0.0, 4.0, 0.3), times(2));
        range.verify(() -> AttackRangeCompat.apply(item, 0.0, 4.5, 0.0, 6.5, 0.125));
    }

    @Test
    void equippingCopiedRecipeResultsCreatesIndependentComboTokens() {
        ItemStack recipeResult = factory.createObsidianGlaive();
        ItemStack first = recipeResult.clone();
        ItemStack second = recipeResult.clone();
        assertEquals(factory.getObsidianGlaiveIdentity(first), factory.getObsidianGlaiveIdentity(second));

        factory.beginObsidianGlaiveCombo(first);
        factory.prepareObsidianGlaive(first, 2);
        factory.beginObsidianGlaiveCombo(second);

        assertNotEquals(factory.getObsidianGlaiveIdentity(first), factory.getObsidianGlaiveIdentity(second));
        assertEquals(2, factory.getObsidianGlaiveStage(first));
        assertEquals(0, factory.getObsidianGlaiveStage(second));
        factory.beginObsidianGlaiveCombo(first);
        assertEquals(0, factory.getObsidianGlaiveStage(first));
    }

    @Test
    void stageChangesPreserveForeignAttributesEnchantmentsWearNameAndIdentity() {
        ItemStack item = factory.createObsidianGlaive();
        String identity = factory.getObsidianGlaiveIdentity(item);
        ItemMeta meta = item.getItemMeta();
        ItemMetaCompat.setDisplayName(meta, "Nightfall");
        ((Damageable) meta).setDamage(37);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                new NamespacedKey("other", "bonus"), 2,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        item.setItemMeta(meta);

        for (int stage : new int[]{1, 2, 0, 1, 2}) {
            factory.prepareObsidianGlaive(item, stage);
        }

        assertEquals(identity, factory.getObsidianGlaiveIdentity(item));
        assertEquals("Nightfall", ItemMetaCompat.getDisplayName(item.getItemMeta()));
        assertEquals(37, ((Damageable) item.getItemMeta()).getDamage());
        assertEquals(3, item.getEnchantmentLevel(Enchantment.SWEEPING_EDGE));
        assertEquals(2, item.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE).size());
        assertEquals(9, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
        assertEquals(1, item.getItemMeta().getAttributeModifiers(Attribute.ATTACK_SPEED).size());
    }

    @Test
    void reloadRefreshesOnceResetsComboAndPreservesWearAndIdentity() {
        ItemStack item = factory.createObsidianGlaive();
        factory.prepareObsidianGlaive(item, 2);
        String identity = factory.getObsidianGlaiveIdentity(item);
        ItemMeta meta = item.getItemMeta();
        ((Damageable) meta).setDamage(53);
        item.setItemMeta(meta);
        assertFalse(factory.refreshObsidianGlaive(item));
        config.set("mechanics.obsidian_glaive.damage", 10.0);
        ItemFactory reloaded = new ItemFactory(plugin, lang, null);

        assertFalse(reloaded.hasCurrentGlaiveRevision(item));
        assertTrue(reloaded.refreshObsidianGlaive(item));
        assertFalse(reloaded.refreshObsidianGlaive(item));
        assertTrue(reloaded.hasCurrentGlaiveRevision(item));
        assertEquals(0, reloaded.getObsidianGlaiveStage(item));
        assertEquals(10, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
        assertEquals(53, ((Damageable) item.getItemMeta()).getDamage());
        assertEquals(identity, reloaded.getObsidianGlaiveIdentity(item));
    }

    @Test
    void disabledStabKeepsWhackAndDisabledItemIsNotRecognizedAsActive() {
        config.set("mechanics.obsidian_glaive.stab_enabled", false);
        factory = new ItemFactory(plugin, lang, null);
        ItemStack item = factory.createObsidianGlaive();

        factory.prepareObsidianGlaive(item, 2);

        assertEquals(0, factory.getObsidianGlaiveStage(item));
        animation.verify(() -> AttackAnimationCompat.apply(item, true), never());
        range.verify(() -> AttackRangeCompat.apply(item, 0.0, 4.0, 0.0, 4.0, 0.3), times(2));
        config.set("items.obsidian_glaive.enabled", false);
        assertFalse(factory.isObsidianGlaive(item));
        assertFalse(factory.isRecipeEnabled("obsidian_glaive"));
        assertFalse(factory.knownGiveNames().contains("obsidian_glaive"));
    }

    @Test
    void missingRangeComponentUsesOwnedFallbackWithoutStackingModifiers() {
        range.when(() -> AttackRangeCompat.apply(any(ItemStack.class), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), anyDouble())).thenReturn(false);
        ItemStack item = factory.createObsidianGlaive();
        assertEquals(4.0, 3 + modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));

        factory.prepareObsidianGlaive(item, 2);
        factory.prepareObsidianGlaive(item, 2);

        assertEquals(4.5, 3 + modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));
        assertEquals(1, item.getItemMeta().getAttributeModifiers(Attribute.ENTITY_INTERACTION_RANGE).size());
        verify(plugin.getLogger(), times(1)).warning(contains("attack range component API"));
    }

    private double modifierTotal(ItemStack item, Attribute attribute) {
        var modifiers = item.getItemMeta().getAttributeModifiers(attribute);
        return modifiers == null ? 0.0 : modifiers.stream().mapToDouble(AttributeModifier::getAmount).sum();
    }
}
