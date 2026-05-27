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
import pepin.pepeforge.util.ItemMetaCompat;
import pepin.pepeforge.weapons.crescentbow.CrescentBowDefinition;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearDefinition;
import pepin.pepeforge.weapons.greatsword.GreatswordTier;
import pepin.pepeforge.weapons.katana.KatanaDefinition;
import pepin.pepeforge.weapons.windblade.WindBladeTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            ItemIds.NETHERITE_SCYTHE
    );

    private static final Map<String, String> ITEM_ALIASES = Map.ofEntries(
            Map.entry(ItemIds.CRESCENT_BOW, ItemIds.CRESCENT_BOW),
            Map.entry(ItemIds.CRESCENT_SPEAR, ItemIds.CRESCENT_SPEAR),
            Map.entry(ItemIds.CHISEL, ItemIds.CHISEL),
            Map.entry(ItemIds.KATANA, ItemIds.KATANA),
            Map.entry(ItemIds.IRON_WIND_BLADE, ItemIds.IRON_WIND_BLADE),
            Map.entry(ItemIds.DIAMOND_WIND_BLADE, ItemIds.DIAMOND_WIND_BLADE),
            Map.entry(ItemIds.NETHERITE_WIND_BLADE, ItemIds.NETHERITE_WIND_BLADE),
            Map.entry(ItemIds.IRON_GREATSWORD, ItemIds.IRON_GREATSWORD),
            Map.entry(ItemIds.DIAMOND_GREATSWORD, ItemIds.DIAMOND_GREATSWORD),
            Map.entry(ItemIds.NETHERITE_GREATSWORD, ItemIds.NETHERITE_GREATSWORD),
            Map.entry(ItemIds.IRON_SCYTHE, ItemIds.IRON_SCYTHE),
            Map.entry(ItemIds.DIAMOND_SCYTHE, ItemIds.DIAMOND_SCYTHE),
            Map.entry(ItemIds.NETHERITE_SCYTHE, ItemIds.NETHERITE_SCYTHE)
    );

    private final NamespacedKey itemIdKey;
    private final JavaPlugin plugin;
    private final PluginLang lang;

    public ItemFactory(JavaPlugin plugin, PluginLang lang) {
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.plugin = plugin;
        this.lang = lang;
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
                null,
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", tier.attackDamage()),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", tier.attackSpeed())
                )
        ));
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
                null,
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", tier.attackDamage()),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", tier.attackSpeed())
                )
        ));
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
                null,
                List.of()
        ));
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
                null,
                List.of()
        ));
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
                null,
                List.of()
        ));
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
                null,
                List.of(
                        new ItemAttributeSpec(Attribute.ATTACK_DAMAGE, "attack_damage", KatanaDefinition.ATTACK_DAMAGE),
                        new ItemAttributeSpec(Attribute.ATTACK_SPEED, "attack_speed", KatanaDefinition.ATTACK_SPEED),
                        new ItemAttributeSpec(Attribute.ENTITY_INTERACTION_RANGE, "attack_range", KatanaDefinition.ATTACK_RANGE_BONUS)
                )
        ));
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
                List.of()
        ));
    }

    private ItemStack createItem(ItemSpec spec) {
        ItemStack item = new ItemStack(spec.baseMaterial());
        ItemMeta meta = item.getItemMeta();
        boolean clientSideTranslations = useClientSideTranslations();

        String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
        String fallbackName = lang.getItemNameForLang(spec.langPath(), serverLang);
        List<String> fallbackLore = trimLore(lang.getItemLoreForLang(spec.langPath(), serverLang), spec.loreLineCount());

        ItemMetaCompat.setItemName(meta, fallbackName);
        if (!clientSideTranslations) {
            ItemMetaCompat.setDisplayName(meta, fallbackName);
        }
        ItemMetaCompat.setLore(meta, fallbackLore);
        ItemMetaCompat.setCustomModelData(meta, spec.customModelData());
        if (spec.modelKey() != null) {
            ItemMetaCompat.setItemModelIfSupported(meta, spec.modelKey());
        }
        for (ItemAttributeSpec attribute : spec.attributes()) {
            ItemMetaCompat.addMainHandAttribute(
                    meta,
                    attribute.attribute(),
                    spec.itemId() + "_" + attribute.idSuffix(),
                    attribute.value()
            );
        }

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, spec.itemId());
        item.setItemMeta(meta);

        if (clientSideTranslations) {
            ItemMetaCompat.applyTranslatableItemTextDataIfSupported(
                    item,
                    spec.translationKeyBase() + ".name",
                    spec.nameColor().colorName(),
                    buildLoreKeys(spec.translationKeyBase(), spec.loreLineCount()),
                    buildLoreColors(spec.loreLineCount(), spec.rarity())
            );
        }
        return item;
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true)
                && ItemMetaCompat.supportsItemTextDataComponents();
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

    public boolean hasGreatsword(Inventory inventory, GreatswordTier wantedTier) {
        for (ItemStack item : inventory.getContents()) {
            if (getGreatswordTier(item) == wantedTier) {
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
            default -> null;
        };
    }

    private String resolveRequestedItemId(String name) {
        if (name == null) {
            return null;
        }
        return ITEM_ALIASES.get(name.toLowerCase(Locale.ROOT));
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
            List<ItemAttributeSpec> attributes
    ) {
    }

    private record ItemAttributeSpec(
            Attribute attribute,
            String idSuffix,
            double value
    ) {
    }
}
