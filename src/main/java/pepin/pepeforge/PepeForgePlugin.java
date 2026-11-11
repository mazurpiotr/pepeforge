package pepin.pepeforge;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import pepin.pepeforge.command.PepeForgeCommand;
import pepin.pepeforge.gui.CustomItemsMenuListener;
import pepin.pepeforge.gui.ConfigMenuListener;
import org.bukkit.entity.Player;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.recipe.RecipeDiscoveryRefresher;
import pepin.pepeforge.util.combat.TwoHandedEquipmentObserver;
import pepin.pepeforge.util.aura.AuraManager;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.combat.TemporaryShieldManager;
import pepin.pepeforge.util.cooldown.CooldownManager;
import pepin.pepeforge.resourcepack.ResourcePackService;
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
import pepin.pepeforge.weapons.crescentkhopesh.CrescentKhopeshModule;
import pepin.pepeforge.weapons.obsidianglaive.ObsidianGlaiveModule;
import pepin.pepeforge.weapons.crimsonsword.CrimsonSwordModule;
import pepin.pepeforge.weapons.greatsword.GreatswordModule;
import pepin.pepeforge.weapons.rapier.RapierModule;
import pepin.pepeforge.weapons.katana.KatanaModule;
import pepin.pepeforge.weapons.solarshield.SolarShieldModule;
import pepin.pepeforge.weapons.windblade.WindBladeModule;
import pepin.pepeforge.weapons.anchor.AnchorModule;
import pepin.pepeforge.weapons.throwingknife.ThrowingKnifeModule;
import pepin.pepeforge.weapons.stormcleaver.StormcleaverModule;
import pepin.pepeforge.weapons.stormcleaver.StormcleaverAuraEffect;
import pepin.pepeforge.weapons.emberfang.EmberfangModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PepeForgePlugin extends JavaPlugin {

    private PluginLang lang;
    private ItemFactory itemFactory;
    private CooldownManager cooldownManager;
    private AuraManager auraManager;
    private CrimsonSwordManager crimsonSwordManager;
    private StatisticsManager statsManager;
    private pepin.pepeforge.util.ui.BossBarManager bossBarManager;
    private ResourcePackService resourcePackService;
    private TemporaryShieldManager temporaryShieldManager;
    
    private volatile @NonNull List<@NonNull ItemModule> modules = Objects.requireNonNull(List.of());
    private RecipeDiscoveryRefresher recipeDiscoveryRefresher;
    private TwoHandedEquipmentObserver equipmentObserver;
    private Metrics metrics;
    private volatile ConfigMenuListener configMenuListener;
    private final AtomicBoolean reloading = new AtomicBoolean();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateAnchorMechanicConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        
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
        crimsonSwordManager = new CrimsonSwordManager(this, lang);
        itemFactory = new ItemFactory(this, lang, crimsonSwordManager);
        equipmentObserver = new TwoHandedEquipmentObserver(this);
        getServer().getPluginManager().registerEvents(equipmentObserver, this);
        statsManager = new StatisticsManager(this);
        getServer().getPluginManager().registerEvents(new DamageFlow(), this);
        getServer().getPluginManager().registerEvents(new StatisticsListener(statsManager, itemFactory), this);

        boolean migrationEnabled = getConfig().getBoolean("migration.enabled", true);
        pepin.pepeforge.item.ItemMigrator itemMigrator = new pepin.pepeforge.item.ItemMigrator(this, itemFactory, migrationEnabled);
        getServer().getPluginManager().registerEvents(new pepin.pepeforge.item.ItemMigrationListener(itemMigrator), this);
        
        cooldownManager = new CooldownManager(this);
        bossBarManager = new pepin.pepeforge.util.ui.BossBarManager(this);
        temporaryShieldManager = new TemporaryShieldManager(this, lang, bossBarManager);
        resourcePackService = new ResourcePackService(this);
        getServer().getPluginManager().registerEvents(resourcePackService, this);
        resourcePackService.start();
        auraManager = new AuraManager(this);
        registerPassiveAuras();

        auraManager.startTask();
        
        registerModules();
        equipmentObserver.start();
        // Shield consumption must follow item damage adjustments at the same event priority.
        getServer().getPluginManager().registerEvents(temporaryShieldManager, this);

        PepeForgeCommand commandExecutor = new PepeForgeCommand(this, lang, itemFactory, crimsonSwordManager, statsManager, itemMigrator, equipmentObserver);
        PluginCommand command = getCommand("pepeforge");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new CustomItemsMenuListener(lang, itemFactory, equipmentObserver), this);
        registerConfigMenuListener();

        registerRecipeDiscoveryRefresher();
        recipeDiscoveryRefresher.refreshAllOnlinePlayers();

        // Smithing upgrade listener restores custom model data for smithing recipes
        getServer().getPluginManager().registerEvents(new SmithingUpgradeListener(itemFactory), this);

        updateMetrics();
    }

    public synchronized void setMetricsEnabled(boolean enabled) {
        getConfig().set("metrics.enabled", enabled);
        saveConfig();
        updateMetrics();
    }

    private synchronized void updateMetrics() {
        if (!getConfig().getBoolean("metrics.enabled", true)) {
            stopMetrics();
        } else if (metrics == null) {
            int pluginId = 31861;
            metrics = new Metrics(this, pluginId);
            
            metrics.addCustomChart(new AdvancedPie("most_crafted_weapons", () -> statsManager.getCraftedCounts()));
            metrics.addCustomChart(new AdvancedPie("most_given_weapons", () -> statsManager.getGivenCounts()));
            metrics.addCustomChart(new AdvancedPie("disabled_weapons", () -> {
                java.util.Map<String, Integer> map = new java.util.HashMap<>();
                for (String itemId : itemFactory.getAllCanonicalIds()) {
                    if (!itemFactory.isItemEnabled(itemId)) {
                        map.put(itemId, 1);
                    }
                }
                return map;
            }));
            metrics.addCustomChart(new SimplePie("plugin_language", () -> getConfig().getString("translations.server_language", "en_us")));
            metrics.addCustomChart(new SimplePie("use_client_side_translations", () -> String.valueOf(getConfig().getBoolean("translations.use_client_side", true))));
        }
    }

    private synchronized void stopMetrics() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }
    
    private void registerModules() {
        @NonNull List<@NonNull ItemModule> newModules = Objects.requireNonNull(List.of(
                new ChiselModule(this, itemFactory),
                new ScytheModule(this, itemFactory),
                new WindBladeModule(this, itemFactory, lang, cooldownManager, auraManager),
                new CrescentBowModule(this, itemFactory),
                new CrescentSpearModule(this, itemFactory, lang),
                new CrescentKhopeshModule(this, itemFactory, temporaryShieldManager, auraManager),
                new ObsidianGlaiveModule(this, itemFactory, equipmentObserver),
                new KatanaModule(this, itemFactory, lang, cooldownManager, equipmentObserver),
                new GreatswordModule(this, itemFactory, lang, equipmentObserver),
                new RapierModule(this, itemFactory, lang),
                new CrimsonSwordModule(this, itemFactory, crimsonSwordManager, auraManager),
                new SolarShieldModule(this, itemFactory, lang, bossBarManager),
                new AnchorModule(this, itemFactory, cooldownManager, lang),
                new ThrowingKnifeModule(this, itemFactory, cooldownManager),
                new StormcleaverModule(this, itemFactory, lang),
                new EmberfangModule(this, itemFactory)));
        modules = newModules;

        for (ItemModule module : newModules) {
            module.onEnable();
        }
    }

    private void registerRecipeDiscoveryRefresher() {
        recipeDiscoveryRefresher = new RecipeDiscoveryRefresher(this, player -> {
            @NonNull List<@NonNull ItemModule> moduleSnapshot = modules ;
            for (ItemModule module : moduleSnapshot) {
                module.discoverRecipesFor(player);
            }
        });
        getServer().getPluginManager().registerEvents(recipeDiscoveryRefresher, this);
    }

    @Override
    public void onDisable() {
        stopConfigMenus();
        stopMetrics();
        if (resourcePackService != null) {
            resourcePackService.stop();
        }
        stopRecipeDiscoveryRefresher();
        if (equipmentObserver != null) {
            equipmentObserver.stop();
        }
        @NonNull List<@NonNull ItemModule> modulesToDisable = modules;
        modules = Objects.requireNonNull(List.of());
        for (ItemModule module : modulesToDisable) {
            module.onDisable();
        }
        if (auraManager != null) {
            auraManager.stop();
        }
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        if (temporaryShieldManager != null) {
            temporaryShieldManager.stop();
        }
        if (bossBarManager != null) {
            bossBarManager.clearAll();
        }
        if (statsManager != null) {
            statsManager.forceSave();
        }
        HandlerList.unregisterAll(this);
    }

    public boolean reloadPlugin() {
        if (!reloading.compareAndSet(false, true)) {
            return false;
        }
        try {
            reloadRuntime();
            return true;
        } finally {
            reloading.set(false);
        }
    }

    private void reloadRuntime() {
        stopConfigMenus();
        if (statsManager != null) {
            statsManager.forceSave();
        }
        if (resourcePackService != null) {
            resourcePackService.stop();
        }
        stopRecipeDiscoveryRefresher();
        if (equipmentObserver != null) {
            equipmentObserver.stop();
        }
        HandlerList.unregisterAll(this);
        if (auraManager != null) {
            auraManager.stop();
        }
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        @NonNull List<@NonNull ItemModule> modulesToDisable = modules;
        modules = Objects.requireNonNull(List.of());
        for (ItemModule module : modulesToDisable) {
            module.onDisable();
        }
        if (temporaryShieldManager != null) {
            temporaryShieldManager.stop();
        }
        if (bossBarManager != null) {
            bossBarManager.clearAll();
        }

        reloadConfig();
        migrateAnchorMechanicConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(new DamageFlow(), this);
        lang = new PluginLang(this);
        crimsonSwordManager = new CrimsonSwordManager(this, lang);
        itemFactory = new ItemFactory(this, lang, crimsonSwordManager);
        equipmentObserver = new TwoHandedEquipmentObserver(this);
        getServer().getPluginManager().registerEvents(equipmentObserver, this);
        cooldownManager = new CooldownManager(this);
        bossBarManager = new pepin.pepeforge.util.ui.BossBarManager(this);
        temporaryShieldManager = new TemporaryShieldManager(this, lang, bossBarManager);
        resourcePackService = new ResourcePackService(this);
        getServer().getPluginManager().registerEvents(resourcePackService, this);
        resourcePackService.start();

        boolean migrationEnabled = getConfig().getBoolean("migration.enabled", true);
        pepin.pepeforge.item.ItemMigrator itemMigrator = new pepin.pepeforge.item.ItemMigrator(this, itemFactory, migrationEnabled);
        
        getServer().getPluginManager().registerEvents(new StatisticsListener(statsManager, itemFactory), this);
        getServer().getPluginManager().registerEvents(new pepin.pepeforge.item.ItemMigrationListener(itemMigrator), this);
        getServer().getPluginManager().registerEvents(new CustomItemsMenuListener(lang, itemFactory, equipmentObserver), this);
        registerConfigMenuListener();

        getServer().getPluginManager().registerEvents(new SmithingUpgradeListener(itemFactory), this);

        registerPassiveAuras();
        auraManager.startTask();

        registerModules();
        equipmentObserver.start();
        // Shield consumption must follow item damage adjustments at the same event priority.
        getServer().getPluginManager().registerEvents(temporaryShieldManager, this);
        registerRecipeDiscoveryRefresher();

        PepeForgeCommand commandExecutor = new PepeForgeCommand(this, lang, itemFactory, crimsonSwordManager, statsManager, itemMigrator, equipmentObserver);
        PluginCommand command = getCommand("pepeforge");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        recipeDiscoveryRefresher.refreshAllOnlinePlayers();
        updateMetrics();
    }

    public void openConfigMenu(Player player) {
        ConfigMenuListener listener = configMenuListener;
        if (!reloading.get() && listener != null) {
            listener.open(player);
        }
    }

    private void registerConfigMenuListener() {
        configMenuListener = new ConfigMenuListener(this, itemFactory, lang);
        getServer().getPluginManager().registerEvents(configMenuListener, this);
    }

    private void stopConfigMenus() {
        ConfigMenuListener listener = configMenuListener;
        configMenuListener = null;
        if (listener != null) {
            listener.stop();
        }
    }

    private void stopRecipeDiscoveryRefresher() {
        if (recipeDiscoveryRefresher != null) {
            recipeDiscoveryRefresher.stop();
            recipeDiscoveryRefresher = null;
        }
    }

    private void registerPassiveAuras() {
        auraManager.registerPassiveAura(new CrescentAuraEffect(itemFactory));
        auraManager.registerPassiveAura(new StormcleaverAuraEffect(this, itemFactory));
    }

    public PluginLang getPluginLang() {
        return lang;
    }

    private void migrateAnchorMechanicConfig() {
        String[] keys = {
                "ability_cooldown",
                "snare_duration",
                "snare_cooldown",
                "ability_range",
                "snare_enabled",
                "hook_enabled"
        };

        boolean changed = false;
        for (String key : keys) {
            String oldPath = "items.anchor." + key;
            String newPath = "mechanics.anchor." + key;
            if (!getConfig().isSet(oldPath)) {
                continue;
            }

            if (!getConfig().isSet(newPath)) {
                getConfig().set(newPath, getConfig().get(oldPath));
            }
            getConfig().set(oldPath, null);
            changed = true;
        }

        if (changed) {
            saveConfig();
        }
    }
}
