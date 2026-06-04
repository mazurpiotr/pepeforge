package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ItemMetaCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CrimsonSwordManager {

    private static final String TRANSLATION_KEY_BASE = CrimsonSwordDefinition.TRANSLATION_KEY_BASE;
    private static final LegacyComponentSerializer LEGACY_SECTION_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final PluginLang lang;
    private final NamespacedKey xpKey;
    private final NamespacedKey levelKey;

    public CrimsonSwordManager(JavaPlugin plugin, PluginLang lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.xpKey = new NamespacedKey(plugin, CrimsonSwordDefinition.XP_KEY);
        this.levelKey = new NamespacedKey(plugin, CrimsonSwordDefinition.LEVEL_KEY);
    }

    public void initialize(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(levelKey, PersistentDataType.INTEGER, CrimsonSwordDefinition.MIN_LEVEL);
        data.set(xpKey, PersistentDataType.DOUBLE, 0.0D);
        item.setItemMeta(meta);
        updateText(item);
    }

    public int getLevel(ItemStack item) {
        ItemMeta meta = getMeta(item);
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
    }

    public boolean addXp(Player player, ItemStack item, double amount) {
        if (amount <= 0.0D) {
            return false;
        }

        ItemMeta meta = getMeta(item);
        if (meta == null) {
            return false;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        int level = data.getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
        if (level <= 0 || level >= CrimsonSwordDefinition.MAX_LEVEL) {
            return false;
        }

        double xp = data.getOrDefault(xpKey, PersistentDataType.DOUBLE, 0.0D) + amount;
        boolean leveledUp = false;
        while (level < CrimsonSwordDefinition.MAX_LEVEL) {
            double requiredXp = requiredXpForLevel(level);
            if (xp < requiredXp) {
                break;
            }
            xp -= requiredXp;
            level++;
            leveledUp = true;
        }

        if (level >= CrimsonSwordDefinition.MAX_LEVEL) {
            xp = 0.0D;
        }

        data.set(levelKey, PersistentDataType.INTEGER, level);
        data.set(xpKey, PersistentDataType.DOUBLE, xp);
        item.setItemMeta(meta);
        updateText(item);

        if (leveledUp) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.55f);
        }
        return leveledUp;
    }

    public double requiredXpForLevel(int level) {
        int clampedLevel = Math.max(CrimsonSwordDefinition.MIN_LEVEL, Math.min(level, CrimsonSwordDefinition.MAX_LEVEL));
        return Math.floor(CrimsonSwordDefinition.BASE_XP
                * Math.pow(CrimsonSwordDefinition.XP_CURVE_MULTIPLIER, clampedLevel - 1));
    }

    public void updateText(ItemStack item) {
        ItemMeta meta = getMeta(item);
        if (meta == null) {
            return;
        }

        int level = meta.getPersistentDataContainer()
                .getOrDefault(levelKey, PersistentDataType.INTEGER, CrimsonSwordDefinition.MIN_LEVEL);
        double xp = meta.getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.DOUBLE, 0.0D);
        double requiredXp = level >= CrimsonSwordDefinition.MAX_LEVEL ? 0.0D : requiredXpForLevel(level);
        String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
        boolean clientSideTranslations = useClientSideTranslations();
        Component displayName = clientSideTranslations
                ? translatedName(level)
                : fallbackName(serverLang, level);

        ItemMetaCompat.setItemName(meta, displayName);
        if (!clientSideTranslations) {
            ItemMetaCompat.setDisplayName(meta, displayName);
        }
        ItemMetaCompat.setLore(meta, clientSideTranslations
                ? buildTranslatedLore(level, xp, requiredXp)
                : buildFallbackLore(serverLang, level, xp, requiredXp));
        item.setItemMeta(meta);
    }

    private Component translatedName(int level) {
        return Component.translatable(TRANSLATION_KEY_BASE + ".name", NamedTextColor.DARK_RED)
                .append(levelSuffix(level));
    }

    private Component fallbackName(String serverLang, int level) {
        String baseName = lang.getItemNameForLang(CrimsonSwordDefinition.LANG_PATH, serverLang);
        return LEGACY_SECTION_SERIALIZER.deserialize(baseName).append(levelSuffix(level));
    }

    private Component levelSuffix(int level) {
        return Component.text(" [Lv. " + level + "]", NamedTextColor.GRAY);
    }

    private List<Component> buildTranslatedLore(int level, double xp, double requiredXp) {
        List<Component> features = translatedFeatures(level);
        Component featureLine = features.isEmpty()
                ? Component.text("-", NamedTextColor.DARK_GRAY)
                : Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), features);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.1"));
        lore.add(Component.translatable(
                TRANSLATION_KEY_BASE + ".lore.level",
                Component.text(level, NamedTextColor.RED),
                Component.text(formatXp(xp), NamedTextColor.GRAY),
                level >= CrimsonSwordDefinition.MAX_LEVEL
                        ? Component.text("MAX", NamedTextColor.GOLD)
                        : Component.text(formatXp(requiredXp), NamedTextColor.GRAY)
        ));
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.2"));
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.3"));
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.4"));
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.unlocked"));
        lore.add(featureLine);
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.rarity", NamedTextColor.GOLD));
        lore.add(Component.translatable(TRANSLATION_KEY_BASE + ".lore.1"));
        return lore;
    }

    private List<Component> buildFallbackLore(String serverLang, int level, double xp, double requiredXp) {
        List<String> loreLines =
                new ArrayList<>(lang.getItemLoreForLang(CrimsonSwordDefinition.LANG_PATH, serverLang));

        List<Component> result = new ArrayList<>();

        List<Component> features = fallbackFeatures(level);

        Component featureLine = features.isEmpty()
                ? Component.text("-", NamedTextColor.DARK_GRAY)
                : Component.join(
                        JoinConfiguration.separator(
                                Component.text(", ", NamedTextColor.GRAY)
                        ),
                        features
                );

        for (String line : loreLines) {
            line = line
                    .replace("{level}", String.valueOf(level))
                    .replace("{xp}", formatXp(xp))
                    .replace("{max_xp}", level >= CrimsonSwordDefinition.MAX_LEVEL
                            ? "MAX"
                            : formatXp(requiredXp));

            if (line.contains("{features}")) {
                String[] parts = line.split("\\{features\\}", 2);

                result.add(
                        LEGACY_SECTION_SERIALIZER.deserialize(parts[0])
                                .append(featureLine)
                                .append(LEGACY_SECTION_SERIALIZER.deserialize(parts.length > 1 ? parts[1] : ""))
                );
            } else {
                result.add(LEGACY_SECTION_SERIALIZER.deserialize(line));
            }
        }

        return result;
    }

    private List<Component> translatedFeatures(int level) {
        List<Component> features = new ArrayList<>();

        features.add(Component.translatable(
                TRANSLATION_KEY_BASE + ".feature.edge",
                NamedTextColor.RED,
                Component.text(damageBonusPercent(level), NamedTextColor.RED)
        ));

        addLifestealFeature(level, features, true);
        addAuraFeature(level, features, true);
        return features;
    }

    private List<Component> fallbackFeatures(int level) {
        List<Component> features = new ArrayList<>();

        features.add(LEGACY_SECTION_SERIALIZER.deserialize(
                lang.text("items.crimson_sword.features.edge")
                        .replace("{percent}", String.valueOf(damageBonusPercent(level)))
        ));

        addLifestealFeature(level, features, false);
        addAuraFeature(level, features, false);
        return features;
    }

    private void addLifestealFeature(int level, List<Component> features, boolean translated) {
        double lifesteal = lifesteal(level);
        if (lifesteal <= 0.0D) {
            return;
        }
        if (translated) {
            features.add(Component.translatable(
                    TRANSLATION_KEY_BASE + ".feature.lifesteal",
                    NamedTextColor.RED,
                    Component.text(formatPercent(lifesteal * 100), NamedTextColor.RED)
            ));
            return;
        }

        features.add(LEGACY_SECTION_SERIALIZER.deserialize(
                lang.text("items.crimson_sword.features.lifesteal")
                        .replace("{percent}", formatPercent(lifesteal * 100))
        ));
    }

    private void addAuraFeature(int level, List<Component> features, boolean translated) {
        if (level < 10) {
            return;
        }

        String seconds = String.valueOf(auraDurationTicks(level) / 20);
        String drain = formatPercent(auraDrainAmount(level));
        String radius = formatPercent(CrimsonSwordDefinition.AURA_RADIUS);
        if (translated) {
            features.add(Component.translatable(
                    TRANSLATION_KEY_BASE + ".feature.aura",
                    NamedTextColor.DARK_RED,
                    Component.text(seconds, NamedTextColor.DARK_RED),
                    Component.text(drain, NamedTextColor.DARK_RED),
                    Component.text(radius, NamedTextColor.DARK_RED)
            ));
            return;
        }

        features.add(LEGACY_SECTION_SERIALIZER.deserialize(
                lang.text("items.crimson_sword.features.aura")
                        .replace("{seconds}", seconds)
                        .replace("{drain}", drain)
                        .replace("{radius}", radius)
        ));
    }

    private int damageBonusPercent(int level) {
        return (int) (Math.min(level, CrimsonSwordDefinition.MAX_LEVEL)
                * CrimsonSwordDefinition.DAMAGE_BONUS_PER_LEVEL
                * 100);
    }

    private double lifesteal(int level) {
        if (level >= 25) {
            return CrimsonSwordDefinition.LEVEL_25_LIFESTEAL;
        }
        if (level >= 15) {
            return CrimsonSwordDefinition.LEVEL_15_LIFESTEAL;
        }
        if (level >= 5) {
            return CrimsonSwordDefinition.LEVEL_5_LIFESTEAL;
        }
        return 0.0D;
    }

    private int auraDurationTicks(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_AURA_TICKS;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_AURA_TICKS;
        }
        return CrimsonSwordDefinition.LEVEL_10_AURA_TICKS;
    }

    private double auraDrainAmount(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_AURA_DRAIN;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_AURA_DRAIN;
        }
        return CrimsonSwordDefinition.LEVEL_10_AURA_DRAIN;
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true)
                && ItemMetaCompat.supportsItemTextDataComponents();
    }

    private String formatPercent(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private String formatXp(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private ItemMeta getMeta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta();
    }
}
