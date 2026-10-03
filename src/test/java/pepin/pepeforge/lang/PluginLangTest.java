package pepin.pepeforge.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginLangTest {

    @TempDir
    Path dataFolder;

    @Test
    void unsupportedConfiguredLanguageFallsBackAndUpdatesConfig() throws IOException {
        Path langDirectory = Files.createDirectories(dataFolder.resolve("lang"));
        Files.writeString(langDirectory.resolve("en_us.yml"), "messages.test: English fallback\n");

        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "missing_locale");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PluginLangTest"));

        PluginLang pluginLang = new PluginLang(plugin);

        assertEquals("en_us", config.getString("language"));
        assertEquals("English fallback", pluginLang.text("messages.test"));
        verify(plugin).saveConfig();
    }

    @Test
    void explicitLanguageLookupDoesNotDependOnPluginLanguage() throws IOException {
        Path langDirectory = Files.createDirectories(dataFolder.resolve("lang"));
        Files.writeString(langDirectory.resolve("en_us.yml"), "items.crimson_sword.features.edge: English feature\n");
        Files.writeString(langDirectory.resolve("pl_pl.yml"), "items.crimson_sword.features.edge: Polska cecha\n");

        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en_us");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PluginLangTest"));

        PluginLang pluginLang = new PluginLang(plugin);

        assertEquals("English feature", pluginLang.text("items.crimson_sword.features.edge"));
        assertEquals("Polska cecha", pluginLang.getTextForLang("items.crimson_sword.features.edge", "pl_pl"));
    }

    @Test
    void bundledLanguageFilesAreRefreshedAndPreviousCopiesAreBackedUp() throws IOException {
        Path langDirectory = Files.createDirectories(dataFolder.resolve("lang"));
        String oldEnglish = "messages:\n  version: Old English\n";
        String oldPolish = "messages:\n  version: Stary polski\n";
        Files.writeString(langDirectory.resolve("en_us.yml"), oldEnglish);
        Files.writeString(langDirectory.resolve("pl_pl.yml"), oldPolish);

        JavaPlugin plugin = mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en_us");
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PluginLangTest"));
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        when(description.getVersion()).thenReturn("1.4.1");
        when(plugin.getDescription()).thenReturn(description);
        when(plugin.getResource("lang/en_us.yml")).thenAnswer(invocation -> new ByteArrayInputStream(
                "messages:\n  version: Current English\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(plugin.getResource("lang/pl_pl.yml")).thenAnswer(invocation -> new ByteArrayInputStream(
                "messages:\n  version: Aktualny polski\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        PluginLang pluginLang = new PluginLang(plugin);

        assertEquals("Current English", pluginLang.text("messages.version"));
        assertEquals("Current English", YamlConfiguration.loadConfiguration(langDirectory.resolve("en_us.yml").toFile())
                .getString("messages.version"));
        assertEquals(oldEnglish, Files.readString(langDirectory.resolve("en_us.yml.backup-1.4.1")));
        assertEquals(oldPolish, Files.readString(langDirectory.resolve("pl_pl.yml.backup-1.4.1")));
        assertTrue(Files.exists(langDirectory.resolve("pl_pl.yml")));
    }

    @Test
    void selectLanguageAcceptsAvailableLanguageCaseInsensitively() {
        assertEquals("pl_pl", PluginLang.selectLanguage(" PL_PL ", Set.of("en_us", "pl_pl")));
    }

    @Test
    void selectLanguageUsesEnglishWhenLanguageIsUnavailable() {
        assertEquals("en_us", PluginLang.selectLanguage("missing_locale", Set.of("en_us", "pl_pl")));
    }
}
