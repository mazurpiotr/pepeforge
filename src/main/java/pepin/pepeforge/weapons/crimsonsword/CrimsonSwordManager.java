package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.util.itemmeta.ItemMetaManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CrimsonSwordManager {

    private static final CrimsonSwordFormatter FORMATTER;

    static {
        CrimsonSwordFormatter temp = null;
        if (pepin.pepeforge.util.env.ServerEnv.hasDataComponentApi() && pepin.pepeforge.util.env.AdventureReflect.isSupported()) {
            try {
                temp = (CrimsonSwordFormatter) Class.forName("pepin.pepeforge.weapons.crimsonsword.CrimsonSwordKyoriFormatterImpl")
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        FORMATTER = temp != null ? temp : new FallbackCrimsonSwordFormatterImpl();
    }

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

    public void setLevel(ItemStack item, int level) {
        ItemMeta meta = getMeta(item);
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        int clampedLevel = Math.max(CrimsonSwordDefinition.MIN_LEVEL,
                Math.min(level, CrimsonSwordDefinition.MAX_LEVEL));
        data.set(levelKey, PersistentDataType.INTEGER, clampedLevel);
        data.set(xpKey, PersistentDataType.DOUBLE, 0.0D);
        item.setItemMeta(meta);
        updateText(item);
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
        int clampedLevel = Math.max(CrimsonSwordDefinition.MIN_LEVEL,
                Math.min(level, CrimsonSwordDefinition.MAX_LEVEL));
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

        if (useClientSideTranslations()) {
            FORMATTER.applyTranslatedText(item, this, level, xp, requiredXp);
        } else {
            String serverLang = plugin.getConfig().getString("translations.server_language", "en_us");
            String displayName = fallbackName(serverLang, level);
            List<String> lore = buildFallbackLore(serverLang, level, xp, requiredXp);

            ItemMetaManager.setItemName(meta, displayName);
            ItemMetaManager.setDisplayName(meta, displayName);
            ItemMetaManager.setStringLore(meta, lore);
            item.setItemMeta(meta);
        }
    }

    private String fallbackName(String serverLang, int level) {
        String baseName = lang.getItemNameForLang(CrimsonSwordDefinition.LANG_PATH, serverLang);
        return ColorUtil.translate(baseName) + ColorUtil.GRAY + " [Lv. " + level + "]";
    }

    private List<String> fallbackFeatures(int level) {
        List<String> features = new ArrayList<>();

        features.add(ColorUtil.translate(
                lang.text("items.crimson_sword.features.edge")
                        .replace("{percent}", String.valueOf(damageBonusPercent(level)))));

        addLifestealFeature(level, features);
        addAuraFeature(level, features);
        return features;
    }

    private void addLifestealFeature(int level, List<String> features) {
        double lifesteal = lifesteal(level);
        if (lifesteal <= 0.0D) {
            features.add(ColorUtil.translate(lang.text("items.crimson_sword.features.lifesteal_locked")));
            return;
        }

        int nextLevel = nextLifestealUnlockLevel(level);
        double nextPercent = nextLifestealPercent(nextLevel);

        String line = nextLevel > level && nextPercent > lifesteal
                ? lang.text("items.crimson_sword.features.lifesteal_next")
                : lang.text("items.crimson_sword.features.lifesteal");

        features.add(ColorUtil.translate(line
                .replace("{percent}", formatPercent(lifesteal * 100))
                .replace("{next_level}", String.valueOf(nextLevel))
                .replace("{next_percent}", formatPercent(nextPercent * 100))));
    }

    private void addAuraFeature(int level, List<String> features) {
        if (level < 10) {
            features.add(ColorUtil.translate(lang.text("items.crimson_sword.features.aura_locked")));
            return;
        }

        String seconds = String.valueOf(auraDurationTicks(level) / 20);
        String drain = formatPercent(auraDrainAmount(level));
        String radius = formatPercent(CrimsonSwordDefinition.AURA_RADIUS);
        int nextLevel = nextAuraUnlockLevel(level);
        String nextSeconds = String.valueOf(auraDurationTicks(nextLevel) / 20);
        String nextDrain = formatPercent(auraDrainAmount(nextLevel));

        String line = nextLevel > level
                ? lang.text("items.crimson_sword.features.aura_next")
                : lang.text("items.crimson_sword.features.aura");

        features.add(ColorUtil.translate(line
                .replace("{seconds}", seconds)
                .replace("{drain}", drain)
                .replace("{radius}", radius)
                .replace("{next_level}", String.valueOf(nextLevel))
                .replace("{next_seconds}", nextSeconds)
                .replace("{next_drain}", nextDrain)));
    }

    private List<String> buildFallbackLore(String serverLang, int level, double xp, double requiredXp) {
        List<String> loreLines = new ArrayList<>(lang.getItemLoreForLang(CrimsonSwordDefinition.LANG_PATH, serverLang));

        List<String> result = new ArrayList<>();
        List<String> features = fallbackFeatures(level);

        for (String line : loreLines) {
            line = line
                    .replace("{level}", String.valueOf(level))
                    .replace("{xp}", formatXp(xp))
                    .replace("{max_xp}", level >= CrimsonSwordDefinition.MAX_LEVEL
                            ? "MAX"
                            : formatXp(requiredXp));

            if (line.contains("{features}")) {
                if (features.isEmpty()) {
                    result.add(ColorUtil.translate(line.replace("{features}", "-")));
                } else {
                    for (String f : features) {
                        result.add(ColorUtil.translate(line.replace("{features}", "") + f));
                    }
                }
            } else {
                result.add(ColorUtil.translate(line));
            }
        }

        return result;
    }

    public int damageBonusPercent(int level) {
        return (int) (Math.min(level, CrimsonSwordDefinition.MAX_LEVEL)
                * CrimsonSwordDefinition.DAMAGE_BONUS_PER_LEVEL
                * 100);
    }

    public double lifesteal(int level) {
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

    public int nextLifestealUnlockLevel(int level) {
        if (level < 5) {
            return 5;
        }
        if (level < 15) {
            return 15;
        }
        return 25;
    }

    public double nextLifestealPercent(int level) {
        if (level >= 25) {
            return CrimsonSwordDefinition.LEVEL_25_LIFESTEAL;
        }
        if (level >= 15) {
            return CrimsonSwordDefinition.LEVEL_15_LIFESTEAL;
        }
        return CrimsonSwordDefinition.LEVEL_5_LIFESTEAL;
    }

    public int auraDurationTicks(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_AURA_TICKS;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_AURA_TICKS;
        }
        return CrimsonSwordDefinition.LEVEL_10_AURA_TICKS;
    }

    public double auraDrainAmount(int level) {
        if (level >= 30) {
            return CrimsonSwordDefinition.LEVEL_30_AURA_DRAIN;
        }
        if (level >= 20) {
            return CrimsonSwordDefinition.LEVEL_20_AURA_DRAIN;
        }
        return CrimsonSwordDefinition.LEVEL_10_AURA_DRAIN;
    }

    public int nextAuraUnlockLevel(int level) {
        if (level < 20) {
            return 20;
        }
        return 30;
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true)
                && pepin.pepeforge.util.env.ServerEnv.hasDataComponentApi();
    }

    public String formatPercent(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    public String formatXp(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    public void heal(Player player, double amount) {
        if (amount <= 0.0D || player.isDead()) {
            return;
        }

        org.bukkit.attribute.AttributeInstance maxHealthAttribute = player
                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();
        player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
    }

    private ItemMeta getMeta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta();
    }
}
