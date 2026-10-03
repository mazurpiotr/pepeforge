package pepin.pepeforge.weapons.emberfang;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemRarity;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.util.env.AdventureReflect;
import pepin.pepeforge.util.env.ServerEnv;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmberfangTextManager {

    private static final EmberfangTextFormatter FORMATTER = createFormatter();

    private final JavaPlugin plugin;
    private final PluginLang lang;

    public EmberfangTextManager(JavaPlugin plugin, PluginLang lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void updateText(ItemStack item) {
        double fireDamage = getConfiguredFireDamage();
        String formattedFireDamage = formatFireDamage(fireDamage);
        if (useClientSideTranslations() && !(FORMATTER instanceof EmberfangFallbackFormatter)) {
            FORMATTER.applyTranslatedText(item, formattedFireDamage);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String serverLanguage = plugin.getConfig().getString("translations.server_language", "en_us");
        List<String> configuredLore = lang.getItemLoreForLang(EmberfangDefinition.LANG_PATH, serverLanguage);
        List<String> lore = new ArrayList<>(configuredLore.size());
        for (int index = 0; index < configuredLore.size(); index++) {
            String line = configuredLore.get(index).replace("{fire_damage}", formattedFireDamage);
            lore.add(index == configuredLore.size() - 1
                    ? ItemRarity.EPIC.formatLegacy(line)
                    : ColorUtil.translate(line));
        }
        ItemMetaCompat.setLore(meta, lore);
        item.setItemMeta(meta);
    }

    public double getConfiguredFireDamage() {
        double configuredDamage = plugin.getConfig().getDouble(
                EmberfangDefinition.FIRE_DAMAGE_CONFIG_PATH,
                EmberfangDefinition.FIRE_DAMAGE);
        return Double.isFinite(configuredDamage) ? Math.max(0.0D, configuredDamage) : EmberfangDefinition.FIRE_DAMAGE;
    }

    private String formatFireDamage(double fireDamage) {
        String language = plugin.getConfig().getString("translations.server_language", "en_us");
        Locale locale = Locale.forLanguageTag(language.replace('_', '-'));
        char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
        String value = BigDecimal.valueOf(fireDamage).stripTrailingZeros().toPlainString();
        return decimalSeparator == '.' ? value : value.replace('.', decimalSeparator);
    }

    private boolean useClientSideTranslations() {
        return plugin.getConfig().getBoolean("translations.use_client_side", true)
                && ServerEnv.hasDataComponentApi()
                && AdventureReflect.isSupported();
    }

    private static EmberfangTextFormatter createFormatter() {
        if (ServerEnv.hasDataComponentApi() && AdventureReflect.isSupported()) {
            try {
                return (EmberfangTextFormatter) Class.forName(
                        "pepin.pepeforge.weapons.emberfang.EmberfangKyoriFormatter")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return new EmberfangFallbackFormatter();
    }
}
