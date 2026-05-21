package pepin.pepeforge.item;

import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.tools.chisel.ChiselDefinition;
import pepin.pepeforge.tools.scythe.ScytheTier;
import pepin.pepeforge.util.ItemMetaCompat;
import pepin.pepeforge.weapons.katana.KatanaDefinition;
import pepin.pepeforge.weapons.crescentbow.CrescentBowDefinition;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearDefinition;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

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
            ItemIds.IRON_SCYTHE,
            ItemIds.DIAMOND_SCYTHE,
            ItemIds.NETHERITE_SCYTHE
    );

    private final NamespacedKey itemIdKey;
    private final JavaPlugin plugin;
    private final PluginLang lang;

    public ItemFactory(JavaPlugin plugin, PluginLang lang) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.plugin = plugin;
        this.lang = lang;
    }

    private void setItemNameBasedOnConfig(ItemMeta meta, String langPath, String translationKeyBase, ItemNameColor nameColor) {
        boolean useClientSide = plugin.getConfig().getBoolean("translations.use_client_side", true);
        if (useClientSide) {
            ItemMetaCompat.setTranslatableItemNameIfSupported(meta, translationKeyBase + ".name", nameColor.colorName());
            ItemMetaCompat.setTranslatableDisplayNameIfSupported(meta, translationKeyBase + ".name", nameColor.colorName());
        } else {
            String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
            String name = lang.getItemNameForLang(langPath, serverLang);
            ItemMetaCompat.setItemName(meta, name);
            ItemMetaCompat.setDisplayName(meta, name);
        }
    }

    private void setItemLoreBasedOnConfig(ItemMeta meta, String langPath, String translationKeyBase, int loreLineCount, ItemRarity rarity) {
        boolean useClientSide = plugin.getConfig().getBoolean("translations.use_client_side", true);
        if (useClientSide) {
            List<String> loreKeys = buildLoreKeys(translationKeyBase, loreLineCount);
            List<String> loreColors = buildLoreColors(loreLineCount, rarity);
            ItemMetaCompat.setTranslatableLoreIfSupported(meta, loreKeys, loreColors);
        } else {
            String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
            List<String> lore = trimLore(lang.getItemLoreForLang(langPath, serverLang), loreLineCount);
            ItemMetaCompat.setLore(meta, lore);
        }
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true);
    }

    public ItemStack createWindBlade(WindBladeTier tier) {
        ItemStack item = new ItemStack(tier.baseMaterial());
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(tier.langPath());
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(tier.langPath()), tier.loreLineCount());

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, tier.customModelData());
        ItemMetaCompat.addMainHandAttribute(meta, Attribute.ATTACK_DAMAGE, tier.itemId() + "_attack_damage", tier.attackDamage());
        ItemMetaCompat.addMainHandAttribute(meta, Attribute.ATTACK_SPEED, tier.itemId() + "_attack_speed", tier.attackSpeed());
        setItemNameBasedOnConfig(meta, tier.langPath(), tier.translationKeyBase(), tier.nameColor());
        setItemLoreBasedOnConfig(meta, tier.langPath(), tier.translationKeyBase(), tier.loreLineCount(), tier.rarity());

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, tier.itemId());
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(item, tier.translationKeyBase() + ".name", tier.nameColor().colorName(), buildLoreKeys(tier.translationKeyBase(), tier.loreLineCount()), buildLoreColors(tier.loreLineCount(), tier.rarity()));
        }
        return item;
    }

    public ItemStack createCrescentBow() {
        ItemStack item = new ItemStack(CrescentBowDefinition.BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(CrescentBowDefinition.LANG_PATH);
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(CrescentBowDefinition.LANG_PATH), CrescentBowDefinition.LORE_LINE_COUNT);

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, CrescentBowDefinition.CUSTOM_MODEL_DATA);
        setItemNameBasedOnConfig(meta, CrescentBowDefinition.LANG_PATH, CrescentBowDefinition.TRANSLATION_KEY_BASE, CrescentBowDefinition.NAME_COLOR);
        setItemLoreBasedOnConfig(meta, CrescentBowDefinition.LANG_PATH, CrescentBowDefinition.TRANSLATION_KEY_BASE, CrescentBowDefinition.LORE_LINE_COUNT, CrescentBowDefinition.RARITY);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, CrescentBowDefinition.ITEM_ID);
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(
                    item,
                    CrescentBowDefinition.TRANSLATION_KEY_BASE + ".name",
                    CrescentBowDefinition.NAME_COLOR.colorName(),
                    buildLoreKeys(CrescentBowDefinition.TRANSLATION_KEY_BASE, CrescentBowDefinition.LORE_LINE_COUNT),
                    buildLoreColors(CrescentBowDefinition.LORE_LINE_COUNT, CrescentBowDefinition.RARITY)
            );
        }
        return item;
    }

    public ItemStack createCrescentSpear() {
        ItemStack item = new ItemStack(CrescentSpearDefinition.BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(CrescentSpearDefinition.LANG_PATH);
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(CrescentSpearDefinition.LANG_PATH), CrescentSpearDefinition.LORE_LINE_COUNT);

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, CrescentSpearDefinition.CUSTOM_MODEL_DATA);
        setItemNameBasedOnConfig(meta, CrescentSpearDefinition.LANG_PATH, CrescentSpearDefinition.TRANSLATION_KEY_BASE, CrescentSpearDefinition.NAME_COLOR);
        setItemLoreBasedOnConfig(meta, CrescentSpearDefinition.LANG_PATH, CrescentSpearDefinition.TRANSLATION_KEY_BASE, CrescentSpearDefinition.LORE_LINE_COUNT, CrescentSpearDefinition.RARITY);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, CrescentSpearDefinition.ITEM_ID);
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(
                    item,
                    CrescentSpearDefinition.TRANSLATION_KEY_BASE + ".name",
                    CrescentSpearDefinition.NAME_COLOR.colorName(),
                    buildLoreKeys(CrescentSpearDefinition.TRANSLATION_KEY_BASE, CrescentSpearDefinition.LORE_LINE_COUNT),
                    buildLoreColors(CrescentSpearDefinition.LORE_LINE_COUNT, CrescentSpearDefinition.RARITY)
            );
        }
        return item;
    }

    public ItemStack createChisel() {
        ItemStack item = new ItemStack(ChiselDefinition.BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(ChiselDefinition.LANG_PATH);
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(ChiselDefinition.LANG_PATH), ChiselDefinition.LORE_LINE_COUNT);

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, ChiselDefinition.CUSTOM_MODEL_DATA);
        setItemNameBasedOnConfig(meta, ChiselDefinition.LANG_PATH, ChiselDefinition.TRANSLATION_KEY_BASE, ChiselDefinition.NAME_COLOR);
        setItemLoreBasedOnConfig(meta, ChiselDefinition.LANG_PATH, ChiselDefinition.TRANSLATION_KEY_BASE, ChiselDefinition.LORE_LINE_COUNT, ChiselDefinition.RARITY);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, ChiselDefinition.ITEM_ID);
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(
                    item,
                    ChiselDefinition.TRANSLATION_KEY_BASE + ".name",
                    ChiselDefinition.NAME_COLOR.colorName(),
                    buildLoreKeys(ChiselDefinition.TRANSLATION_KEY_BASE, ChiselDefinition.LORE_LINE_COUNT),
                    buildLoreColors(ChiselDefinition.LORE_LINE_COUNT, ChiselDefinition.RARITY)
            );
        }
        return item;
    }

    public ItemStack createKatana() {
        ItemStack item = new ItemStack(KatanaDefinition.BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(KatanaDefinition.LANG_PATH);
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(KatanaDefinition.LANG_PATH), KatanaDefinition.LORE_LINE_COUNT);

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, KatanaDefinition.CUSTOM_MODEL_DATA);
        ItemMetaCompat.addMainHandAttribute(meta, Attribute.ATTACK_DAMAGE, KatanaDefinition.ITEM_ID + "_attack_damage", KatanaDefinition.ATTACK_DAMAGE);
        ItemMetaCompat.addMainHandAttribute(meta, Attribute.ATTACK_SPEED, KatanaDefinition.ITEM_ID + "_attack_speed", KatanaDefinition.ATTACK_SPEED);
        ItemMetaCompat.addMainHandAttribute(meta, Attribute.ENTITY_INTERACTION_RANGE, KatanaDefinition.ITEM_ID + "_attack_range", KatanaDefinition.ATTACK_RANGE_BONUS);
        setItemNameBasedOnConfig(meta, KatanaDefinition.LANG_PATH, KatanaDefinition.TRANSLATION_KEY_BASE, KatanaDefinition.NAME_COLOR);
        setItemLoreBasedOnConfig(meta, KatanaDefinition.LANG_PATH, KatanaDefinition.TRANSLATION_KEY_BASE, KatanaDefinition.LORE_LINE_COUNT, KatanaDefinition.RARITY);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, KatanaDefinition.ITEM_ID);
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(
                    item,
                    KatanaDefinition.TRANSLATION_KEY_BASE + ".name",
                    KatanaDefinition.NAME_COLOR.colorName(),
                    buildLoreKeys(KatanaDefinition.TRANSLATION_KEY_BASE, KatanaDefinition.LORE_LINE_COUNT),
                    buildLoreColors(KatanaDefinition.LORE_LINE_COUNT, KatanaDefinition.RARITY)
            );
        }
        return item;
    }

    public ItemStack createScythe(ScytheTier tier) {
        ItemStack item = new ItemStack(tier.baseMaterial());
        ItemMeta meta = item.getItemMeta();

        String fallbackName = lang.itemFallbackName(tier.langPath());
        List<String> fallbackLore = trimLore(lang.itemFallbackLore(tier.langPath()), tier.loreLineCount());

        ItemMetaCompat.setItemName(meta, fallbackName);
        ItemMetaCompat.setDisplayName(meta, fallbackName);
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, tier.customModelData());
        ItemMetaCompat.setItemModelIfSupported(meta, tier.modelKey());
        setItemNameBasedOnConfig(meta, tier.langPath(), tier.translationKeyBase(), tier.nameColor());
        setItemLoreBasedOnConfig(meta, tier.langPath(), tier.translationKeyBase(), tier.loreLineCount(), tier.rarity());

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, tier.itemId());
        item.setItemMeta(meta);
        if (useClientSideTranslations()) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(item, tier.translationKeyBase() + ".name", tier.nameColor().colorName(), buildLoreKeys(tier.translationKeyBase(), tier.loreLineCount()), buildLoreColors(tier.loreLineCount(), tier.rarity()));
        }
        return item;
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
            case ItemIds.IRON_SCYTHE -> createScythe(ScytheTier.IRON);
            case ItemIds.DIAMOND_SCYTHE -> createScythe(ScytheTier.DIAMOND);
            case ItemIds.NETHERITE_SCYTHE -> createScythe(ScytheTier.NETHERITE);
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
        return itemEnabled && plugin.getConfig().getBoolean(configPath + ".recipe_enabled", false);
    }

    public WindBladeTier getWindBladeTier(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null || !isItemEnabled(itemId)) {
            return null;
        }
        return WindBladeTier.fromItemId(itemId);
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

    public void setKatanaParryVisual(ItemStack item, boolean active) {
        if (item == null || !isKatana(item) || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        ItemMetaCompat.setCustomModelData(meta, active ? KatanaDefinition.PARRY_MODEL_DATA : KatanaDefinition.CUSTOM_MODEL_DATA);
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

    public ScytheTier getScytheTier(ItemStack item) {
        String itemId = getItemId(item);
        if (itemId == null || !isItemEnabled(itemId)) {
            return null;
        }
        for (ScytheTier tier : ScytheTier.values()) {
            if (tier.itemId().equals(itemId)) {
                return tier;
            }
        }
        return null;
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
        return switch (itemId) {
            case ItemIds.CRESCENT_BOW -> lang.itemFallbackName(CrescentBowDefinition.LANG_PATH);
            case ItemIds.CRESCENT_SPEAR -> lang.itemFallbackName(CrescentSpearDefinition.LANG_PATH);
            case ItemIds.CHISEL -> lang.itemFallbackName(ChiselDefinition.LANG_PATH);
            case ItemIds.KATANA -> lang.itemFallbackName(KatanaDefinition.LANG_PATH);
            default -> null;
        };
    }

    private String resolveRequestedItemId(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case ItemIds.CRESCENT_BOW -> ItemIds.CRESCENT_BOW;
            case ItemIds.CRESCENT_SPEAR -> ItemIds.CRESCENT_SPEAR;
            case ItemIds.CHISEL -> ItemIds.CHISEL;
            case ItemIds.KATANA -> ItemIds.KATANA;
            case ItemIds.IRON_WIND_BLADE, "iron_wind_sword" -> ItemIds.IRON_WIND_BLADE;
            case ItemIds.DIAMOND_WIND_BLADE, "diamond_wind_sword" -> ItemIds.DIAMOND_WIND_BLADE;
            case ItemIds.NETHERITE_WIND_BLADE, "netherite_wind_sword" -> ItemIds.NETHERITE_WIND_BLADE;
            case ItemIds.IRON_SCYTHE -> ItemIds.IRON_SCYTHE;
            case ItemIds.DIAMOND_SCYTHE -> ItemIds.DIAMOND_SCYTHE;
            case ItemIds.NETHERITE_SCYTHE -> ItemIds.NETHERITE_SCYTHE;
            default -> null;
        };
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
}
