package pepin.pepeforge.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public final class PluginLang {

    private final YamlConfiguration messages;
    private final Map<String, YamlConfiguration> langFiles = new HashMap<>();
    private final Map<String, YamlConfiguration> bundledLangFiles = new HashMap<>();
    private final String language;

    public PluginLang(JavaPlugin plugin) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        refreshBundledLanguageFiles(plugin, langDir);
        loadAllLanguages(plugin, langDir);

        String configuredLanguage = plugin.getConfig().getString("language", "en_us");
        language = selectLanguage(configuredLanguage, langFiles.keySet());
        if (configuredLanguage == null || !language.equalsIgnoreCase(configuredLanguage.trim())) {
            plugin.getLogger().warning("Unsupported language '" + configuredLanguage + "'; using en_us instead.");
            plugin.getConfig().set("language", language);
            plugin.saveConfig();
        }

        File targetFile = new File(langDir, language + ".yml");
        if (!targetFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        YamlConfiguration bundledLanguage = bundledLangFiles.get(language);
        messages = bundledLanguage != null
                ? bundledLanguage
                : YamlConfiguration.loadConfiguration(targetFile);

        try (InputStream fallbackStream = plugin.getResource("lang/en_us.yml")) {
            if (fallbackStream != null) {
                YamlConfiguration fallback = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(fallbackStream, StandardCharsets.UTF_8)
                );
                messages.setDefaults(fallback);
                messages.options().copyDefaults(true);
                messages.save(targetFile);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not update language defaults in " + targetFile.getName() + ": "
                    + exception.getMessage());
        }
    }

    static String selectLanguage(String requestedLanguage, Set<String> availableLanguages) {
        if (requestedLanguage != null) {
            String normalizedLanguage = requestedLanguage.trim().toLowerCase(Locale.ROOT);
            if (availableLanguages.contains(normalizedLanguage)) {
                return normalizedLanguage;
            }
        }
        return "en_us";
    }

    private void loadAllLanguages(JavaPlugin plugin, File langDir) {
        // Load en_us and pl_pl from resources
        loadLanguageFromResource(plugin, "en_us");
        loadLanguageFromResource(plugin, "pl_pl");

        // Load from langDir if files exist
        File[] files = langDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().endsWith(".yml")) {
                String lang = file.getName().replace(".yml", "");
                if (!bundledLangFiles.containsKey(lang)) {
                    langFiles.put(lang, YamlConfiguration.loadConfiguration(file));
                }
            }
        }
    }

    private void refreshBundledLanguageFiles(JavaPlugin plugin, File langDir) {
        refreshBundledLanguageFile(plugin, langDir, "en_us");
        refreshBundledLanguageFile(plugin, langDir, "pl_pl");
    }

    private void refreshBundledLanguageFile(JavaPlugin plugin, File langDir, String language) {
        String resourcePath = "lang/" + language + ".yml";
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return;
            }

            byte[] bundledContent = stream.readAllBytes();
            Path target = langDir.toPath().resolve(language + ".yml");
            if (Files.exists(target)) {
                byte[] existingContent = Files.readAllBytes(target);
                if (Arrays.equals(existingContent, bundledContent)) {
                    return;
                }

                Path backup = nextLanguageBackupPath(plugin, langDir, language);
                Files.copy(target, backup);
                plugin.getLogger().info("Saved the previous " + language + " language file to "
                        + backup.getFileName() + ".");
            }

            Files.write(target, bundledContent);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not refresh bundled language file " + resourcePath + ": "
                    + exception.getMessage());
        }
    }

    private Path nextLanguageBackupPath(JavaPlugin plugin, File langDir, String language) {
        String version = plugin.getDescription().getVersion().replaceAll("[^A-Za-z0-9._-]", "_");
        Path backup = langDir.toPath().resolve(language + ".yml.backup-" + version);
        int copy = 2;
        while (Files.exists(backup)) {
            backup = langDir.toPath().resolve(language + ".yml.backup-" + version + "-" + copy);
            copy++;
        }
        return backup;
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

    public String getTextForLang(String path, String preferredLanguage) {
        YamlConfiguration langConfig = langFiles.get(preferredLanguage);
        if (langConfig != null) {
            String value = langConfig.getString(path);
            if (value != null) {
                return color(value);
            }
        }

        return color(resolveText(path, path, preferredLanguage));
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
        return pepin.pepeforge.util.ColorUtil.translate(value);
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
