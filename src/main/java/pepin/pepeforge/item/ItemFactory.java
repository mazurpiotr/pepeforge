package pepin.pepeforge.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.tools.chisel.ChiselDefinition;
import pepin.pepeforge.tools.scythe.ScytheTier;
import pepin.pepeforge.util.itemmeta.ItemMetaManager;
import pepin.pepeforge.weapons.crescentbow.CrescentBowDefinition;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearDefinition;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordDefinition;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordManager;
import pepin.pepeforge.weapons.greatsword.GreatswordTier;
import pepin.pepeforge.weapons.katana.KatanaDefinition;
import pepin.pepeforge.weapons.solarshield.SolarShieldDefinition;
import pepin.pepeforge.weapons.windblade.WindBladeTier;
import pepin.pepeforge.weapons.anchor.AnchorDefinition;
import pepin.pepeforge.weapons.throwingknife.ThrowingKnifeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemFactory {

    private static final List<String> CANONICAL_ITEM_IDS = List.of(
            ItemIds.CRESCENT_BOW,
            ItemIds.CRESCENT_SPEAR,
            ItemIds.CHISEL,
            ItemIds.KATANA,
            ItemIds.IRON_WIND_BLADE,
            ItemIds.DIAMOND_WIND_BLADE,
            ItemIds.NETHERITE_WIND_BLADE,
            ItemIds.IRON_GREATSWORD,
            ItemIds.DIAMOND_GREATSWORD,
            ItemIds.NETHERITE_GREATSWORD,
            ItemIds.IRON_SCYTHE,
            ItemIds.DIAMOND_SCYTHE,
            ItemIds.NETHERITE_SCYTHE,
            ItemIds.CRIMSON_SWORD,
            ItemIds.SOLAR_SHIELD,
            ItemIds.ANCHOR,
            ItemIds.THROWING_KNIFE);

    private final NamespacedKey itemIdKey;
    private final JavaPlugin plugin;
    private final PluginLang lang;
    private final CrimsonSwordManager crimsonSwordManager;

    public ItemFactory(JavaPlugin plugin, PluginLang lang, CrimsonSwordManager crimsonSwordManager) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.plugin = plugin;
        this.lang = lang;
        this.crimsonSwordManager = crimsonSwordManager;
    }

    public ItemStack createWindBlade(WindBladeTier tier) {
        return createItem(new ItemSpec(
                tier.itemId(),
                tier.baseMaterial(),
                tier.langPath(),
                tier.translationKeyBase(),
                tier.loreLineCount(),
                tier.rarity(),
                tier.nameColor(),
                tier.customModelData(),
                tier.modelKey(),
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", tier.attackDamage()),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", tier.attackSpeed()))));
    }

    public ItemStack createGreatsword(GreatswordTier tier) {
        return createItem(new ItemSpec(
                tier.itemId(),
                tier.baseMaterial(),
                tier.langPath(),
                tier.translationKeyBase(),
                tier.loreLineCount(),
                tier.rarity(),
                tier.nameColor(),
                tier.customModelData(),
                tier.modelKey(),
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", tier.attackDamage()),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", tier.attackSpeed()))));
    }

    public ItemStack createCrescentBow() {
        return createItem(new ItemSpec(
                CrescentBowDefinition.ITEM_ID,
                CrescentBowDefinition.BASE_MATERIAL,
                CrescentBowDefinition.LANG_PATH,
                CrescentBowDefinition.TRANSLATION_KEY_BASE,
                CrescentBowDefinition.LORE_LINE_COUNT,
                CrescentBowDefinition.RARITY,
                CrescentBowDefinition.NAME_COLOR,
                CrescentBowDefinition.CUSTOM_MODEL_DATA,
                CrescentBowDefinition.MODEL_KEY,
                List.of()));
    }

    public ItemStack createCrescentSpear() {
        return createItem(new ItemSpec(
                CrescentSpearDefinition.ITEM_ID,
                CrescentSpearDefinition.BASE_MATERIAL,
                CrescentSpearDefinition.LANG_PATH,
                CrescentSpearDefinition.TRANSLATION_KEY_BASE,
                CrescentSpearDefinition.LORE_LINE_COUNT,
                CrescentSpearDefinition.RARITY,
                CrescentSpearDefinition.NAME_COLOR,
                CrescentSpearDefinition.CUSTOM_MODEL_DATA,
                CrescentSpearDefinition.MODEL_KEY,
                List.of()));
    }

    public ItemStack createChisel() {
        return createItem(new ItemSpec(
                ChiselDefinition.ITEM_ID,
                ChiselDefinition.BASE_MATERIAL,
                ChiselDefinition.LANG_PATH,
                ChiselDefinition.TRANSLATION_KEY_BASE,
                ChiselDefinition.LORE_LINE_COUNT,
                ChiselDefinition.RARITY,
                ChiselDefinition.NAME_COLOR,
                ChiselDefinition.CUSTOM_MODEL_DATA,
                ChiselDefinition.MODEL_KEY,
                List.of()));
    }

    public ItemStack createKatana() {
        return createItem(new ItemSpec(
                KatanaDefinition.ITEM_ID,
                KatanaDefinition.BASE_MATERIAL,
                KatanaDefinition.LANG_PATH,
                KatanaDefinition.TRANSLATION_KEY_BASE,
                KatanaDefinition.LORE_LINE_COUNT,
                KatanaDefinition.RARITY,
                KatanaDefinition.NAME_COLOR,
                KatanaDefinition.CUSTOM_MODEL_DATA,
                KatanaDefinition.MODEL_KEY,
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", KatanaDefinition.ATTACK_DAMAGE),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", KatanaDefinition.ATTACK_SPEED),
                        new ItemAttributeSpec(Attribute.ENTITY_INTERACTION_RANGE, "attack_range",
                                KatanaDefinition.ATTACK_RANGE_BONUS))));
    }

    public ItemStack createScythe(ScytheTier tier) {
        return createItem(new ItemSpec(
                tier.itemId(),
                tier.baseMaterial(),
                tier.langPath(),
                tier.translationKeyBase(),
                tier.loreLineCount(),
                tier.rarity(),
                tier.nameColor(),
                tier.customModelData(),
                tier.modelKey(),
                List.of()));
    }

    public ItemStack createCrimsonSword() {
        ItemStack item = new ItemStack(CrimsonSwordDefinition.BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
        String fallbackName = lang.getItemNameForLang(CrimsonSwordDefinition.LANG_PATH, serverLang);

        ItemMetaManager.setItemName(meta, fallbackName);
        if (!useClientSideTranslations()) {
            ItemMetaManager.setDisplayName(meta, fallbackName);
        }
        ItemMetaManager.setCustomModelData(meta, CrimsonSwordDefinition.CUSTOM_MODEL_DATA);
        ItemMetaManager.setItemModelIfSupported(meta, CrimsonSwordDefinition.MODEL_KEY);
        ItemMetaManager.addMainHandAttribute(
                meta,
                Attribute.ATTACK_DAMAGE,
                CrimsonSwordDefinition.ITEM_ID + "_attack_damage",
                CrimsonSwordDefinition.ATTACK_DAMAGE);
        ItemMetaManager.addMainHandAttribute(
                meta,
                Attribute.ATTACK_SPEED,
                CrimsonSwordDefinition.ITEM_ID + "_attack_speed",
                CrimsonSwordDefinition.ATTACK_SPEED);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, CrimsonSwordDefinition.ITEM_ID);
        item.setItemMeta(meta);
        crimsonSwordManager.initialize(item);
        return item;
    }

    public ItemStack createSolarShield() {
        ItemStack item = createItem(new ItemSpec(
                SolarShieldDefinition.ITEM_ID,
                SolarShieldDefinition.BASE_MATERIAL,
                SolarShieldDefinition.LANG_PATH,
                SolarShieldDefinition.TRANSLATION_KEY_BASE,
                SolarShieldDefinition.LORE_LINE_COUNT,
                SolarShieldDefinition.RARITY,
                SolarShieldDefinition.NAME_COLOR,
                SolarShieldDefinition.CUSTOM_MODEL_DATA_0,
                SolarShieldDefinition.MODEL_KEY_0,
                List.of()));
        ItemMeta meta = item.getItemMeta();
        NamespacedKey chargesKey = new NamespacedKey(plugin, SolarShieldDefinition.CHARGES_KEY_STRING);
        meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createAnchor() {
        return createItem(new ItemSpec(
                AnchorDefinition.ITEM_ID,
                AnchorDefinition.BASE_MATERIAL,
                AnchorDefinition.LANG_PATH,
                AnchorDefinition.TRANSLATION_KEY_BASE,
                AnchorDefinition.LORE_LINE_COUNT,
                AnchorDefinition.RARITY,
                AnchorDefinition.NAME_COLOR,
                0,
                AnchorDefinition.MODEL_KEY,
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", AnchorDefinition.ATTACK_DAMAGE),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", AnchorDefinition.ATTACK_SPEED))));
    }

    public ItemStack createThrowingKnife() {
        return createItem(new ItemSpec(
                ThrowingKnifeDefinition.ITEM_ID,
                ThrowingKnifeDefinition.BASE_MATERIAL,
                ThrowingKnifeDefinition.LANG_PATH,
                ThrowingKnifeDefinition.TRANSLATION_KEY_BASE,
                ThrowingKnifeDefinition.LORE_LINE_COUNT,
                ThrowingKnifeDefinition.RARITY,
                ThrowingKnifeDefinition.NAME_COLOR,
                ThrowingKnifeDefinition.CUSTOM_MODEL_DATA,
                ThrowingKnifeDefinition.MODEL_KEY,
                List.of()));
    }

    public boolean isThrowingKnife(ItemStack item) {
        return ItemIds.THROWING_KNIFE.equals(getItemId(item)) && isItemEnabled(ItemIds.THROWING_KNIFE);
    }

    public void updateSolarShieldVisuals(ItemStack item, int charges) {
        if (item == null || !isSolarShield(item) || !item.hasItemMeta()) {
            return;
        }
        int clamped = Math.max(0, Math.min(SolarShieldDefinition.MAX_CHARGES, charges));
        ItemMeta meta = item.getItemMeta();
        NamespacedKey chargesKey = new NamespacedKey(plugin, SolarShieldDefinition.CHARGES_KEY_STRING);
        meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, clamped);

        int customModelData = switch (clamped) {
            case 1 -> SolarShieldDefinition.CUSTOM_MODEL_DATA_1;
            case 2 -> SolarShieldDefinition.CUSTOM_MODEL_DATA_2;
            case 3 -> SolarShieldDefinition.CUSTOM_MODEL_DATA_3;
            default -> SolarShieldDefinition.CUSTOM_MODEL_DATA_0;
        };

        NamespacedKey modelKey = switch (clamped) {
            case 1 -> SolarShieldDefinition.MODEL_KEY_1;
            case 2 -> SolarShieldDefinition.MODEL_KEY_2;
            case 3 -> SolarShieldDefinition.MODEL_KEY_3;
            default -> SolarShieldDefinition.MODEL_KEY_0;
        };

        ItemMetaManager.setCustomModelData(meta, customModelData);
        ItemMetaManager.setItemModelIfSupported(meta, modelKey);
        item.setItemMeta(meta);
    }

    private ItemStack createItem(ItemSpec spec) {
        ItemStack item = new ItemStack(spec.baseMaterial());
        ItemMeta meta = item.getItemMeta();
        boolean clientSideTranslations = useClientSideTranslations();

        String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
        String fallbackName = lang.getItemNameForLang(spec.langPath(), serverLang);
        List<String> fallbackLore = trimLore(lang.getItemLoreForLang(spec.langPath(), serverLang),
                spec.loreLineCount());

        ItemMetaManager.setItemName(meta, fallbackName);
        if (!clientSideTranslations) {
            ItemMetaManager.setDisplayName(meta, fallbackName);
        }
        ItemMetaManager.setStringLore(meta, fallbackLore);
        if (spec.customModelData() > 0) {
            ItemMetaManager.setCustomModelData(meta, spec.customModelData());
        }
        if (spec.modelKey() != null) {
            ItemMetaManager.setItemModelIfSupported(meta, spec.modelKey());
        }
        for (ItemAttributeSpec attribute : spec.attributes()) {
            ItemMetaManager.addMainHandAttribute(
                    meta,
                    attribute.attribute(),
                    spec.itemId() + "_" + attribute.idSuffix(),
                    attribute.value());
        }

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, spec.itemId());
        item.setItemMeta(meta);

        if (pepin.pepeforge.util.env.ServerEnv.hasDataComponentApi()) {
            if (spec.itemId().equals(ItemIds.THROWING_KNIFE)) {
                pepin.pepeforge.util.itemmeta.PaperDataComponentAdapter.applyMaxStackSize(item, 16);
            }
        }

        if (clientSideTranslations) {
            pepin.pepeforge.util.itemmeta.PaperDataComponentAdapter.applyTranslatableItemTextData(
                    item,
                    spec.translationKeyBase() + ".name",
                    spec.nameColor().colorName(),
                    buildLoreKeys(spec.translationKeyBase(), spec.loreLineCount()),
                    buildLoreColors(spec.loreLineCount(), spec.rarity()));
        }
        return item;
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true)
                && pepin.pepeforge.util.env.ServerEnv.hasDataComponentApi();
    }

    public ItemStack createByName(String name) {
        String itemId = resolveRequestedItemId(name);
        if (itemId == null) {
            return null;
        }
        return switch (itemId) {
            case ItemIds.CRESCENT_BOW -> createCrescentBow();
            case ItemIds.CRESCENT_SPEAR -> createCrescentSpear();
            case ItemIds.CHISEL -> createChisel();
            case ItemIds.KATANA -> createKatana();
            case ItemIds.IRON_WIND_BLADE -> createWindBlade(WindBladeTier.IRON);
            case ItemIds.DIAMOND_WIND_BLADE -> createWindBlade(WindBladeTier.DIAMOND);
            case ItemIds.NETHERITE_WIND_BLADE -> createWindBlade(WindBladeTier.NETHERITE);
            case ItemIds.IRON_GREATSWORD -> createGreatsword(GreatswordTier.IRON);
            case ItemIds.DIAMOND_GREATSWORD -> createGreatsword(GreatswordTier.DIAMOND);
            case ItemIds.NETHERITE_GREATSWORD -> createGreatsword(GreatswordTier.NETHERITE);
            case ItemIds.IRON_SCYTHE -> createScythe(ScytheTier.IRON);
            case ItemIds.DIAMOND_SCYTHE -> createScythe(ScytheTier.DIAMOND);
            case ItemIds.NETHERITE_SCYTHE -> createScythe(ScytheTier.NETHERITE);
            case ItemIds.CRIMSON_SWORD -> createCrimsonSword();
            case ItemIds.SOLAR_SHIELD -> createSolarShield();
            case ItemIds.ANCHOR -> createAnchor();
            case ItemIds.THROWING_KNIFE -> createThrowingKnife();
            default -> null;
        };
    }

    public boolean isKnownItemName(String name) {
        return resolveRequestedItemId(name) != null;
    }

    public boolean isItemEnabledByName(String name) {
        String itemId = resolveRequestedItemId(name);
        return itemId != null && isItemEnabled(itemId);
    }

    public boolean isItemEnabled(String itemId) {
        if (itemId == null) {
            return false;
        }
        String configPath = getItemConfigPath(itemId);
        if (configPath == null) {
            return false;
        }
        return plugin.getConfig().getBoolean(configPath + ".enabled", true);
    }

    public boolean isRecipeEnabled(String itemId) {
        String configPath = getItemConfigPath(itemId);
        if (configPath == null) {
            return false;
        }
        boolean itemEnabled = plugin.getConfig().getBoolean(configPath + ".enabled", true);
        boolean recipeEnabled = plugin.getConfig().contains(configPath + ".recipe_enabled")
                ? plugin.getConfig().getBoolean(configPath + ".recipe_enabled")
                : true;
        return itemEnabled && recipeEnabled;
    }

    public WindBladeTier getWindBladeTier(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null || !isItemEnabled(itemId)) {
            return null;
        }
        return WindBladeTier.fromItemId(itemId);
    }

    public GreatswordTier getGreatswordTier(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null || !isItemEnabled(itemId)) {
            return null;
        }
        return GreatswordTier.fromItemId(itemId);
    }

    public boolean isCrescentBow(ItemStack item) {
        return ItemIds.CRESCENT_BOW.equals(getItemId(item)) && isItemEnabled(ItemIds.CRESCENT_BOW);
    }

    public boolean isCrescentSpear(ItemStack item) {
        return ItemIds.CRESCENT_SPEAR.equals(getItemId(item)) && isItemEnabled(ItemIds.CRESCENT_SPEAR);
    }

    public boolean isChisel(ItemStack item) {
        return ItemIds.CHISEL.equals(getItemId(item)) && isItemEnabled(ItemIds.CHISEL);
    }

    public boolean isKatana(ItemStack item) {
        return ItemIds.KATANA.equals(getItemId(item)) && isItemEnabled(ItemIds.KATANA);
    }

    public boolean isCrimsonSword(ItemStack item) {
        return ItemIds.CRIMSON_SWORD.equals(getItemId(item)) && isItemEnabled(ItemIds.CRIMSON_SWORD);
    }

    public boolean isSolarShield(ItemStack item) {
        return ItemIds.SOLAR_SHIELD.equals(getItemId(item)) && isItemEnabled(ItemIds.SOLAR_SHIELD);
    }

    public boolean isAnchor(ItemStack item) {
        return ItemIds.ANCHOR.equals(getItemId(item)) && isItemEnabled(ItemIds.ANCHOR);
    }

    public void setKatanaParryVisual(ItemStack item, boolean active) {
        if (item == null || !isKatana(item) || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        int targetData = active ? KatanaDefinition.PARRY_MODEL_DATA : KatanaDefinition.CUSTOM_MODEL_DATA;
        if (ItemMetaManager.hasCustomModelData(meta, targetData)) {
            return;
        }
        ItemMetaManager.setCustomModelData(meta, targetData);
        ItemMetaManager.setItemModelIfSupported(meta,
                active ? KatanaDefinition.PARRY_MODEL_KEY : KatanaDefinition.MODEL_KEY);
        item.setItemMeta(meta);
    }

    public boolean hasWindBlade(Inventory inventory, WindBladeTier wantedTier) {
        for (ItemStack item : inventory.getContents()) {
            if (getWindBladeTier(item) == wantedTier) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGreatsword(Inventory inventory, GreatswordTier wantedTier) {
        for (ItemStack item : inventory.getContents()) {
            if (getGreatswordTier(item) == wantedTier) {
                return true;
            }
        }
        return false;
    }

    public boolean hasScythe(Inventory inventory, ScytheTier wantedTier) {
        for (ItemStack item : inventory.getContents()) {
            if (getScytheTier(item) == wantedTier) {
                return true;
            }
        }
        return false;
    }

    public ScytheTier getScytheTier(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null || !isItemEnabled(itemId)) {
            return null;
        }
        return ScytheTier.fromItemId(itemId);
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    public String getBestName(ItemStack item) {
        String itemId = getItemId(item);
        String fallbackName = getFallbackNameForItemId(itemId);
        if (fallbackName != null) {
            return fallbackName;
        }

        if (item == null) {
            return "-";
        }
        return item.getType().name();
    }

    public List<String> knownGiveNames() {
        List<String> names = new ArrayList<>();
        for (String itemId : CANONICAL_ITEM_IDS) {
            if (isItemEnabled(itemId)) {
                names.add(itemId);
            }
        }
        return names;
    }

    public List<String> getAllCanonicalIds() {
        return CANONICAL_ITEM_IDS;
    }

    public List<ItemStack> createAllCustomItems() {
        List<ItemStack> items = new ArrayList<>();
        for (String itemId : CANONICAL_ITEM_IDS) {
            if (!isItemEnabled(itemId)) {
                continue;
            }
            ItemStack item = createByName(itemId);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private String getFallbackNameForItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (WindBladeTier tier : WindBladeTier.values()) {
            if (tier.itemId().equals(itemId)) {
                return lang.itemFallbackName(tier.langPath());
            }
        }
        for (ScytheTier tier : ScytheTier.values()) {
            if (tier.itemId().equals(itemId)) {
                return lang.itemFallbackName(tier.langPath());
            }
        }
        for (GreatswordTier tier : GreatswordTier.values()) {
            if (tier.itemId().equals(itemId)) {
                return lang.itemFallbackName(tier.langPath());
            }
        }
        return switch (itemId) {
            case ItemIds.CRESCENT_BOW -> lang.itemFallbackName(CrescentBowDefinition.LANG_PATH);
            case ItemIds.CRESCENT_SPEAR -> lang.itemFallbackName(CrescentSpearDefinition.LANG_PATH);
            case ItemIds.CHISEL -> lang.itemFallbackName(ChiselDefinition.LANG_PATH);
            case ItemIds.KATANA -> lang.itemFallbackName(KatanaDefinition.LANG_PATH);
            case ItemIds.CRIMSON_SWORD -> lang.itemFallbackName(CrimsonSwordDefinition.LANG_PATH);
            case ItemIds.SOLAR_SHIELD -> lang.itemFallbackName(SolarShieldDefinition.LANG_PATH);
            case ItemIds.ANCHOR -> lang.itemFallbackName(AnchorDefinition.LANG_PATH);
            case ItemIds.THROWING_KNIFE -> lang.itemFallbackName(ThrowingKnifeDefinition.LANG_PATH);
            default -> null;
        };
    }

    private String resolveRequestedItemId(String name) {
        if (name == null) {
            return null;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        return CANONICAL_ITEM_IDS.contains(lowerName) ? lowerName : null;
    }

    private String getItemConfigPath(String itemId) {
        if (itemId == null) {
            return null;
        }
        return "items." + itemId;
    }

    private List<String> buildLoreKeys(String baseKey, int count) {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            keys.add(baseKey + ".lore." + i);
        }
        return keys;
    }

    private List<String> trimLore(List<String> lore, int maxLines) {
        if (lore.size() <= maxLines) {
            return lore;
        }
        return new ArrayList<>(lore.subList(0, maxLines));
    }

    private List<String> buildLoreColors(int loreLineCount, ItemRarity rarity) {
        List<String> colors = new ArrayList<>(loreLineCount);
        int rarityLoreLineNumber = loreLineCount - 1;
        for (int i = 1; i <= loreLineCount; i++) {
            colors.add(i == rarityLoreLineNumber ? rarity.colorName() : null);
        }
        return colors;
    }

    private record ItemSpec(
            String itemId,
            Material baseMaterial,
            String langPath,
            String translationKeyBase,
            int loreLineCount,
            ItemRarity rarity,
            ItemNameColor nameColor,
            int customModelData,
            NamespacedKey modelKey,
            List<ItemAttributeSpec> attributes) {
    }

    private record ItemAttributeSpec(
            Attribute attribute,
            String idSuffix,
            double value) {
    }
}
