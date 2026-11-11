package pepin.pepeforge.item;

import java.util.List;
import java.util.ArrayList;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.recipe.SmithingUpgradeListener;
import pepin.pepeforge.util.itemmeta.AttackAnimationCompat;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;
import pepin.pepeforge.weapons.rapier.RapierMode;
import pepin.pepeforge.weapons.rapier.RapierTier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RapierItemFactoryTest {
    private JavaPlugin plugin;
    private PluginLang lang;
    private YamlConfiguration config;
    private ItemFactory factory;

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
        plugin = mock(JavaPlugin.class);
        lang = mock(PluginLang.class);
        config = new YamlConfiguration();
        config.set("translations.use_client_side", false);
        when(plugin.getName()).thenReturn("PepeForge");
        when(plugin.namespace()).thenReturn("pepeforge");
        when(plugin.getConfig()).thenReturn(config);
        when(lang.getItemNameForLang(anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
        when(lang.getItemLoreForLang(anyString(), anyString())).thenAnswer(call -> new ArrayList<>(
                List.of("---", "Attack", "Left-click", "Sneak + right-click", "One-Handed", "Rare", "---")));
        when(lang.getTextForLang(anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
        factory = new ItemFactory(plugin, lang, null);
    }

    @ParameterizedTest
    @EnumSource(RapierTier.class)
    void tiersHaveTheirOwnIdentityModelAndFinalCombatAttributes(RapierTier tier) {
        try (MockedStatic<AttackAnimationCompat> animation = animations();
                MockedStatic<ItemMetaCompat> metadata = mockStatic(ItemMetaCompat.class, CALLS_REAL_METHODS)) {
            ItemStack item = factory.createRapier(tier);
            assertEquals(tier.baseMaterial(), item.getType());
            assertEquals(tier.itemId(), factory.getItemId(item));
            assertEquals(tier, factory.getRapierTier(item));
            // MockBukkit 4.110.0 drops item_model when cloning item metadata.
            metadata.verify(() -> ItemMetaCompat.setItemModel(any(ItemMeta.class), eq(tier.modelKey())), atLeastOnce());
            assertEquals(RapierMode.THRUST, factory.getRapierMode(item));
            assertTrue(factory.getAllCanonicalIds().contains(tier.itemId()));
            double expectedDamage = switch (tier) {
                case IRON -> 5;
                case DIAMOND -> 6;
                case NETHERITE -> 7;
                case ICE -> 8;
            };
            assertEquals(expectedDamage, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            assertEquals(2, 4 + modifierTotal(item, Attribute.ATTACK_SPEED));
            assertEquals(3.75, 3 + modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));
            if (tier == RapierTier.ICE) {
                assertEquals(32, ((Damageable) item.getItemMeta()).getMaxDamage());
            }
            animation.verify(() -> AttackAnimationCompat.apply(item, true));
        }
    }

    @ParameterizedTest
    @EnumSource(RapierTier.class)
    void refreshingLegacyModesPreservesTheirCombatBehaviorAndWritesCurrentNames(RapierTier tier) {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemFactory reloaded = new ItemFactory(plugin, lang, null);
            NamespacedKey modeKey = new NamespacedKey("pepeforge", "rapier_mode");
            for (String legacyMode : List.of("STAB", "SWEEP")) {
                ItemStack item = factory.createRapier(tier);
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, legacyMode);
                item.setItemMeta(meta);
                RapierMode expected = legacyMode.equals("SWEEP") ? RapierMode.CUT : RapierMode.THRUST;

                assertEquals(expected, reloaded.getRapierMode(item));
                assertTrue(reloaded.refreshRapier(item));
                assertEquals(expected.name(), item.getItemMeta().getPersistentDataContainer()
                        .get(modeKey, PersistentDataType.STRING));
                assertEquals(3 + expected.attackRangeBonus(),
                        3 + modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));
                assertTrue(item.getItemMeta().getLore().getLast().endsWith("mode_lore." + expected.modeKey()));
            }
        }
    }

    @Test
    void repeatedModeSwitchesPreserveItemDataAndForeignAttributesWithoutDuplicatingStats() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack item = factory.createRapier(RapierTier.DIAMOND);
            var meta = item.getItemMeta();
            ItemMetaCompat.setDisplayName(meta, "My Rapier");
            ((Damageable) meta).setDamage(21);
            NamespacedKey foreignKey = new NamespacedKey("other", "bonus");
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    foreignKey, 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            item.setItemMeta(meta);

            factory.updateRapierMode(item, RapierMode.CUT);
            factory.updateRapierMode(item, RapierMode.THRUST);
            factory.updateRapierMode(item, RapierMode.CUT);

            assertEquals(RapierMode.CUT, factory.getRapierMode(item.clone()));
            assertEquals("My Rapier", ItemMetaCompat.getDisplayName(item.getItemMeta()));
            assertEquals(21, ((Damageable) item.getItemMeta()).getDamage());
            assertEquals(2, item.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE).size());
            assertEquals(7, modifierTotal(item, Attribute.ATTACK_DAMAGE));
            assertEquals(8, item.getItemMeta().getLore().size());
            assertTrue(item.getItemMeta().getLore().getLast().endsWith("mode_lore.cut"));
            assertEquals(3, 3 + modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));
            assertEquals(1, item.getItemMeta().getAttributeModifiers(Attribute.ENTITY_INTERACTION_RANGE).size());
            animation.verify(() -> AttackAnimationCompat.apply(item, false), times(2));
        }
    }

    @Test
    void reloadRefreshesExistingItemsOnceAndPreservesMode() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack item = factory.createRapier(RapierTier.IRON);
            factory.updateRapierMode(item, RapierMode.CUT);
            assertFalse(factory.refreshRapier(item));
            config.set("mechanics.rapier.attack_speed", 2.5);
            config.set("mechanics.rapier.damage.iron", 4.0);
            ItemFactory reloaded = new ItemFactory(plugin, lang, null);

            assertTrue(reloaded.refreshRapier(item));
            assertFalse(reloaded.refreshRapier(item));
            assertEquals(RapierMode.CUT, reloaded.getRapierMode(item));
            assertEquals(0, modifierTotal(item, Attribute.ENTITY_INTERACTION_RANGE));
            assertEquals(2.5, 4 + modifierTotal(item, Attribute.ATTACK_SPEED));
            assertEquals(4, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            config.set("items.iron_rapier.enabled", false);
            assertNull(reloaded.getRapierTier(item));
            assertFalse(reloaded.knownGiveNames().contains("iron_rapier"));
        }
    }

    @Test
    void smithingPreservesModeEnchantmentsWearAndCustomName() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack diamond = factory.createRapier(RapierTier.DIAMOND);
            factory.updateRapierMode(diamond, RapierMode.CUT);
            var meta = diamond.getItemMeta();
            ItemMetaCompat.setDisplayName(meta, "Duelist");
            ((Damageable) meta).setDamage(37);
            meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            diamond.setItemMeta(meta);
            PrepareSmithingEvent event = mock(PrepareSmithingEvent.class);
            SmithingInventory inventory = smithingInventory(diamond);
            when(event.getInventory()).thenReturn(inventory);

            new SmithingUpgradeListener(factory).onPrepareSmithing(event);

            ArgumentCaptor<ItemStack> result = ArgumentCaptor.forClass(ItemStack.class);
            verify(event).setResult(result.capture());
            ItemStack netherite = result.getValue();
            assertEquals(RapierTier.NETHERITE, factory.getRapierTier(netherite));
            assertEquals(RapierMode.CUT, factory.getRapierMode(netherite));
            assertEquals(0, modifierTotal(netherite, Attribute.ENTITY_INTERACTION_RANGE));
            assertEquals(37, ((Damageable) netherite.getItemMeta()).getDamage());
            assertEquals(3, netherite.getEnchantmentLevel(Enchantment.SWEEPING_EDGE));
            assertEquals("Duelist", ItemMetaCompat.getDisplayName(netherite.getItemMeta()));
        }
    }

    @Test
    void disabledSmithingBlocksTheVanillaUpgradeAndDoesNotConsumeIngredients() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack diamond = factory.createRapier(RapierTier.DIAMOND);
            config.set("items.netherite_rapier.recipe_enabled", false);
            SmithingInventory inventory = smithingInventory(diamond);
            PrepareSmithingEvent prepare = mock(PrepareSmithingEvent.class);
            when(prepare.getInventory()).thenReturn(inventory);
            SmithItemEvent take = mock(SmithItemEvent.class);
            when(take.getInventory()).thenReturn(inventory);
            SmithingUpgradeListener listener = new SmithingUpgradeListener(factory);

            listener.onPrepareSmithing(prepare);
            listener.onSmithItem(take);

            verify(prepare).setResult(argThat(item -> item.getType().isAir()));
            verify(take).setCancelled(true);
            verify(take, never()).setCurrentItem(any());
        }
    }

    private SmithingInventory smithingInventory(ItemStack diamond) {
        SmithingInventory inventory = mock(SmithingInventory.class);
        when(inventory.getItem(0)).thenReturn(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        when(inventory.getItem(1)).thenReturn(diamond);
        when(inventory.getItem(2)).thenReturn(new ItemStack(Material.NETHERITE_INGOT));
        return inventory;
    }

    @Test
    void iceDamageTracksActualWearAndRepairWithoutChangingModeOrForeignAttributes() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack item = factory.createRapier(RapierTier.ICE);
            factory.updateRapierMode(item, RapierMode.CUT);
            var meta = item.getItemMeta();
            ItemMetaCompat.setDisplayName(meta, "Winter");
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    new NamespacedKey("other", "bonus"), 2.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            ((Damageable) meta).setDamage(16);
            item.setItemMeta(meta);

            assertTrue(factory.refreshRapier(item));
            assertFalse(factory.refreshRapier(item));
            assertEquals(8.0, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            assertEquals(2, item.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE).size());
            assertEquals("Winter", ItemMetaCompat.getDisplayName(item.getItemMeta()));
            assertEquals(RapierMode.CUT, factory.getRapierMode(item));
            meta = item.getItemMeta();
            ((Damageable) meta).setDamage(0);
            item.setItemMeta(meta);
            assertTrue(factory.refreshRapier(item));
            assertEquals(10.0, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            factory.updateRapierMode(item, RapierMode.THRUST);
            assertEquals(32, ((Damageable) item.getItemMeta()).getMaxDamage());
            assertEquals(0, ((Damageable) item.getItemMeta()).getDamage());
        }
    }

    @Test
    void changingIceDurabilityOnReloadPreservesWearFractionAndDamage() {
        try (MockedStatic<AttackAnimationCompat> animation = animations()) {
            ItemStack item = factory.createRapier(RapierTier.ICE);
            var meta = item.getItemMeta();
            ((Damageable) meta).setDamage(16);
            item.setItemMeta(meta);
            config.set("mechanics.ice_rapier.max_durability", 64);
            ItemFactory reloaded = new ItemFactory(plugin, lang, null);

            assertTrue(reloaded.refreshRapier(item));
            assertEquals(64, ((Damageable) item.getItemMeta()).getMaxDamage());
            assertEquals(32, ((Damageable) item.getItemMeta()).getDamage());
            assertEquals(6.0, 1 + modifierTotal(item, Attribute.ATTACK_DAMAGE));
            assertFalse(reloaded.refreshRapier(item));
        }
    }

    private MockedStatic<AttackAnimationCompat> animations() {
        MockedStatic<AttackAnimationCompat> animation = mockStatic(AttackAnimationCompat.class);
        animation.when(() -> AttackAnimationCompat.apply(any(ItemStack.class), anyBoolean()))
                .thenReturn(true);
        return animation;
    }

    private double modifierTotal(ItemStack item, Attribute attribute) {
        return item.getItemMeta().getAttributeModifiers(attribute).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }
}
