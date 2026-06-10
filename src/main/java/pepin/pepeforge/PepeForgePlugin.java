package pepin.pepeforge;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.command.PepeForgeCommand;
import pepin.pepeforge.gui.CustomItemsMenuListener;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.recipe.RecipeDiscoveryRefresher;
import pepin.pepeforge.util.AuraManager;
import pepin.pepeforge.util.CooldownManager;
import pepin.pepeforge.weapons.crescent.CrescentAuraEffect;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordManager;
import pepin.pepeforge.recipe.SmithingUpgradeListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.ConfigurationSection;
import pepin.pepeforge.stats.StatisticsListener;
import pepin.pepeforge.stats.StatisticsManager;

import pepin.pepeforge.module.ItemModule;
import pepin.pepeforge.tools.chisel.ChiselModule;
import pepin.pepeforge.tools.scythe.ScytheModule;
import pepin.pepeforge.weapons.crescentbow.CrescentBowModule;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearModule;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordModule;
import pepin.pepeforge.weapons.greatsword.GreatswordModule;
import pepin.pepeforge.weapons.katana.KatanaModule;
import pepin.pepeforge.weapons.solarshield.SolarShieldModule;
import pepin.pepeforge.weapons.windblade.WindBladeModule;

import java.util.ArrayList;
import java.util.List;

public final class PepeForgePlugin extends JavaPlugin {

    private PluginLang lang;
    private ItemFactory itemFactory;
    private CooldownManager cooldownManager;
    private AuraManager auraManager;
    private CrimsonSwordManager crimsonSwordManager;
    private StatisticsManager statsManager;
    
    private final List<ItemModule> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String version = getDescription().getVersion();
        
        List<String> loadedItems = new ArrayList<>();
        ConfigurationSection itemsSection = getConfig().getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (itemsSection.getBoolean(key + ".enabled", true)) {
                    String[] words = key.split("_");
                    StringBuilder formattedKey = new StringBuilder();
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            formattedKey.append(Character.toUpperCase(word.charAt(0)))
                                        .append(word.substring(1))
                                        .append(" ");
                        }
                    }
                    loadedItems.add(formattedKey.toString().trim());
                }
            }
        }
        
        List<String> formattedItemLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder(" Loaded Tools & Weapons: ");
        for (int i = 0; i < loadedItems.size(); i++) {
            String item = loadedItems.get(i);
            if (currentLine.length() + item.length() > 70) {
                formattedItemLines.add(currentLine.toString());
                currentLine = new StringBuilder("                         "); // padding
            }
            currentLine.append(item);
            if (i < loadedItems.size() - 1) {
                currentLine.append(", ");
            }
        }
        if (currentLine.length() > 0) {
            formattedItemLines.add(currentLine.toString());
        }

        List<String> bootMessage = new ArrayList<>();
        bootMessage.add(" ");
        bootMessage.add("▄▖      ▌    ▄▖        ");
        bootMessage.add("▙▌█▌▛▌█▌ ▛▘  ▙▖▛▌▛▘▛▌█▌");
        bootMessage.add("▌ ▙▖▙▌▙▖ ▄▌  ▌ ▙▌▌ ▙▌▙▖");
        bootMessage.add("    ▌              ▄▌  ");
        bootMessage.add(" ");
        bootMessage.add(" Version: " + version);
        bootMessage.addAll(formattedItemLines);
        bootMessage.add(" ");

        for (String line : bootMessage) {
            getLogger().info(line);
        }
        
        lang = new PluginLang(this);
        itemFactory = new ItemFactory(this, lang);
        statsManager = new StatisticsManager(this);
        getServer().getPluginManager().registerEvents(new StatisticsListener(statsManager, itemFactory), this);

        boolean migrationEnabled = getConfig().getBoolean("migration.enabled", true);
        pepin.pepeforge.item.ItemMigrator itemMigrator = new pepin.pepeforge.item.ItemMigrator(this, itemFactory, migrationEnabled);
        getServer().getPluginManager().registerEvents(new pepin.pepeforge.item.ItemMigrationListener(itemMigrator), this);
        
        cooldownManager = new CooldownManager();
        auraManager = new AuraManager(this);
        auraManager.registerPassiveAura(new CrescentAuraEffect(itemFactory));
        
        crimsonSwordManager = new CrimsonSwordManager(this, lang);

        auraManager.startTask();
        
        registerModules();

        PepeForgeCommand commandExecutor = new PepeForgeCommand(lang, itemFactory, crimsonSwordManager, statsManager, itemMigrator);
        PluginCommand command = getCommand("pepeforge");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new CustomItemsMenuListener(lang, itemFactory), this);

        RecipeDiscoveryRefresher recipeDiscoveryRefresher = new RecipeDiscoveryRefresher(this, player -> {
            for (ItemModule module : modules) {
                module.discoverRecipesFor(player);
            }
        });
        getServer().getPluginManager().registerEvents(recipeDiscoveryRefresher, this);
        recipeDiscoveryRefresher.refreshAllOnlinePlayers();

        // Smithing upgrade listener restores custom model data for smithing recipes
        getServer().getPluginManager().registerEvents(new SmithingUpgradeListener(itemFactory), this);

        // bStats
        if (getConfig().getBoolean("metrics.enabled", true)) {
            int pluginId = 31861;
            Metrics metrics = new Metrics(this, pluginId);
            
            metrics.addCustomChart(new AdvancedPie("most_crafted_weapons", () -> statsManager.getCraftedCounts()));
            metrics.addCustomChart(new AdvancedPie("most_given_weapons", () -> statsManager.getGivenCounts()));
            metrics.addCustomChart(new AdvancedPie("enabled_weapons", () -> {
                java.util.Map<String, Integer> map = new java.util.HashMap<>();
                for (String itemId : itemFactory.knownGiveNames()) {
                    map.put(itemId, 1);
                }
                return map;
            }));
            metrics.addCustomChart(new SimplePie("server_language", () -> getConfig().getString("translations.server_language", "en_us")));
            metrics.addCustomChart(new SimplePie("use_client_side_translations", () -> String.valueOf(getConfig().getBoolean("translations.use_client_side", true))));
        }
    }
    
    private void registerModules() {
        modules.add(new ChiselModule(this, itemFactory));
        modules.add(new ScytheModule(this, itemFactory));
        modules.add(new WindBladeModule(this, itemFactory, lang, cooldownManager, auraManager));
        modules.add(new CrescentBowModule(this, itemFactory));
        modules.add(new CrescentSpearModule(this, itemFactory, lang));
        modules.add(new KatanaModule(this, itemFactory, lang, cooldownManager));
        modules.add(new GreatswordModule(this, itemFactory, lang));
        modules.add(new CrimsonSwordModule(this, itemFactory, crimsonSwordManager, auraManager));
        modules.add(new SolarShieldModule(this, itemFactory));

        for (ItemModule module : modules) {
            module.onEnable();
        }
    }

    @Override
    public void onDisable() {
        for (ItemModule module : modules) {
            module.onDisable();
        }
        if (auraManager != null) {
            auraManager.stop();
        }
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        if (statsManager != null) {
            statsManager.forceSave();
        }
        HandlerList.unregisterAll(this);
    }
}
