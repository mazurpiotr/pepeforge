package pepin.pepeforge.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginLang {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private final YamlConfiguration messages;
    private final Map<String, YamlConfiguration> langFiles = new HashMap<>();
    private final Map<String, YamlConfiguration> bundledLangFiles = new HashMap<>();
    private final String language;

    public PluginLang(JavaPlugin plugin) {
        language = plugin.getConfig().getString("language", "en_us");
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Load all available languages
        loadAllLanguages(plugin, langDir);

        File targetFile = new File(langDir, language + ".yml");
        if (!targetFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(targetFile);

        try (InputStream fallbackStream = plugin.getResource("lang/en_us.yml")) {
            if (fallbackStream != null) {
                YamlConfiguration fallback = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(fallbackStream, StandardCharsets.UTF_8)
                );
                messages.setDefaults(fallback);
                messages.options().copyDefaults(true);
                messages.save(targetFile);
            }
        } catch (IOException ignored) {
        }
    }

    private void loadAllLanguages(JavaPlugin plugin, File langDir) {
        // Load en_us and pl_pl from resources
        loadLanguageFromResource(plugin, "en_us");
        loadLanguageFromResource(plugin, "pl_pl");

        // Load from langDir if files exist
        for (File file : langDir.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                String lang = file.getName().replace(".yml", "");
                langFiles.put(lang, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private void loadLanguageFromResource(JavaPlugin plugin, String lang) {
        try (InputStream stream = plugin.getResource("lang/" + lang + ".yml")) {
            if (stream != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                bundledLangFiles.put(lang, config);
                langFiles.put(lang, config);
            }
        } catch (IOException ignored) {
        }
    }

    public String message(String path) {
        return color(resolveText("messages.prefix", "") + resolveText(path, path));
    }

    public String text(String path) {
        return color(resolveText(path, path));
    }

    public String message(String path, Map<String, String> placeholders) {
        String value = message(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    public String itemFallbackName(String itemKey) {
        return color(resolveText("items." + itemKey + ".fallback_name", itemKey));
    }

    public List<String> itemFallbackLore(String itemKey) {
        return coloredList(resolveStringList("items." + itemKey + ".lore", language));
    }

    public String getItemNameForLang(String itemKey, String lang) {
        YamlConfiguration langConfig = langFiles.get(lang);
        String path = "items." + itemKey + ".fallback_name";
        if (langConfig != null) {
            String value = langConfig.getString(path);
            if (value != null) {
                return color(value);
            }
        }

        return color(resolveText(path, itemKey, lang));
    }

    public List<String> getItemLoreForLang(String itemKey, String lang) {
        return coloredList(resolveStringList("items." + itemKey + ".lore", lang));
    }

    private List<String> coloredList(List<String> values) {
        List<String> colored = new ArrayList<>(values.size());
        for (String value : values) {
            colored.add(color(value));
        }
        return colored;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', translateHexColors(value));
    }

    private String translateHexColors(String value) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char character : hex.toCharArray()) {
                replacement.append('&').append(character);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String resolveText(String path, String fallback) {
        String value = messages.getString(path);
        if (value != null) {
            return value;
        }

        YamlConfiguration bundledLanguage = bundledLangFiles.get(language);
        if (bundledLanguage != null) {
            value = bundledLanguage.getString(path);
            if (value != null) {
                return value;
            }
        }

        YamlConfiguration bundledEnglish = bundledLangFiles.get("en_us");
        if (bundledEnglish != null) {
            value = bundledEnglish.getString(path);
            if (value != null) {
                return value;
            }
        }

        return fallback;
    }

    private String resolveText(String path, String fallback, String preferredLanguage) {
        YamlConfiguration bundledPreferredLanguage = bundledLangFiles.get(preferredLanguage);
        if (bundledPreferredLanguage != null) {
            String value = bundledPreferredLanguage.getString(path);
            if (value != null) {
                return value;
            }
        }

        YamlConfiguration bundledEnglish = bundledLangFiles.get("en_us");
        if (bundledEnglish != null) {
            String value = bundledEnglish.getString(path);
            if (value != null) {
                return value;
            }
        }

        return resolveText(path, fallback);
    }

    private List<String> resolveStringList(String path, String preferredLanguage) {
        List<String> values = Collections.emptyList();

        YamlConfiguration langConfig = langFiles.get(preferredLanguage);
        if (langConfig != null) {
            values = langConfig.getStringList(path);
        } else {
            values = messages.getStringList(path);
        }
        if (!values.isEmpty()) {
            return values;
        }

        YamlConfiguration bundledPreferredLanguage = bundledLangFiles.get(preferredLanguage);
        if (bundledPreferredLanguage != null) {
            values = bundledPreferredLanguage.getStringList(path);
            if (!values.isEmpty()) {
                return values;
            }
        }

        YamlConfiguration bundledEnglish = bundledLangFiles.get("en_us");
        if (bundledEnglish != null) {
            values = bundledEnglish.getStringList(path);
            if (!values.isEmpty()) {
                return values;
            }
        }

        return Collections.emptyList();
    }
}
