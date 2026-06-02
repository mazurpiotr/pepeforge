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
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ItemMetaCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CrimsonSwordManager {

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
        String baseName = lang.getItemNameForLang(CrimsonSwordDefinition.LANG_PATH, serverLang);
        Component displayName = Component.text(baseName)
        .append(Component.text(
                " [Lv. " + level + "]",
                NamedTextColor.GRAY
        ));

        ItemMetaCompat.setItemName(meta, displayName);
        if (!plugin.getConfig().getBoolean("translations.use_client_side", true)
                || !ItemMetaCompat.supportsItemTextDataComponents()) {
            ItemMetaCompat.setDisplayName(meta, displayName);
        }
        ItemMetaCompat.setLore(meta, buildLore(serverLang, level, xp, requiredXp));
        item.setItemMeta(meta);
    }

    private List<Component> buildLore(String serverLang, int level, double xp, double requiredXp) {
        List<String> loreLines =
                new ArrayList<>(lang.getItemLoreForLang(CrimsonSwordDefinition.LANG_PATH, serverLang));

        List<Component> result = new ArrayList<>();

        List<Component> features = unlockedFeatures(level);

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
                        Component.text(parts[0])
                                .append(featureLine)
                                .append(Component.text(parts.length > 1 ? parts[1] : ""))
                );
            } else {
                result.add(Component.text(line));
            }
        }

        return result;
    }
    private List<Component> unlockedFeatures(int level) {
        List<Component> features = new ArrayList<>();

        if (level < 5) {
            return features;
        }

        // Heal on kill
        int heartsHealed = level >= 15 ? 2 : 1;
        features.add(
            Component.text(
                "Heal " + heartsHealed + "❤ on Kill",
                NamedTextColor.RED
            )
        );

        // Aura bonuses
        if (level >= 10) {
            double damageBonus;
            double lifesteal;
            int auraSeconds;

            if (level >= 30) {
                damageBonus = CrimsonSwordDefinition.LEVEL_30_DAMAGE_BONUS;
                lifesteal = CrimsonSwordDefinition.LEVEL_30_LIFESTEAL;
                auraSeconds = CrimsonSwordDefinition.LEVEL_30_AURA_TICKS / 20;
            } else if (level >= 20) {
                damageBonus = CrimsonSwordDefinition.LEVEL_20_DAMAGE_BONUS;
                lifesteal = CrimsonSwordDefinition.LEVEL_20_LIFESTEAL;
                auraSeconds = CrimsonSwordDefinition.LEVEL_20_AURA_TICKS / 20;
            } else {
                damageBonus = CrimsonSwordDefinition.LEVEL_10_DAMAGE_BONUS;
                lifesteal = CrimsonSwordDefinition.LEVEL_10_LIFESTEAL;
                auraSeconds = CrimsonSwordDefinition.LEVEL_10_AURA_TICKS / 20;
            }

            features.add(
                Component.text(
                    "Crimson Aura (" + auraSeconds + "s)",
                    NamedTextColor.DARK_RED
                )
            );

            features.add(
                Component.text(
                    "+" + (int) (damageBonus * 100) + "% Damage",
                    NamedTextColor.RED
                )
            );

            features.add(
                Component.text(
                    "+" + formatPercent(lifesteal * 100) + "% Lifesteal",
                    NamedTextColor.RED
                )
            );

            if (level >= 25) {
                features.add(
                    Component.text(
                        "+" + (int) (CrimsonSwordDefinition.CHAIN_DAMAGE_BONUS_PER_STACK * 100) +
                        "% Damage per Kill (" + CrimsonSwordDefinition.CHAIN_MAX_STACKS + " stacks)",
                        NamedTextColor.RED
                    )
                );
            }
        }

        return features;
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
