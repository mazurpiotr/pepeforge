package pepin.pepeforge;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.command.PepeForgeCommand;
import pepin.pepeforge.gui.CustomItemsMenuListener;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.recipe.RecipeDiscoveryRefresher;
import pepin.pepeforge.tools.chisel.ChiselListener;
import pepin.pepeforge.tools.chisel.ChiselRecipeDiscoveryListener;
import pepin.pepeforge.tools.chisel.ChiselRecipes;
import pepin.pepeforge.tools.scythe.ScytheListener;
import pepin.pepeforge.tools.scythe.ScytheRecipeDiscoveryListener;
import pepin.pepeforge.tools.scythe.ScytheRecipes;
import pepin.pepeforge.weapons.katana.KatanaListener;
import pepin.pepeforge.weapons.katana.KatanaRecipeDiscoveryListener;
import pepin.pepeforge.weapons.katana.KatanaRecipes;
import pepin.pepeforge.weapons.crescent.CrescentMoonGlow;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearListener;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearRecipeDiscoveryListener;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearRecipes;
import pepin.pepeforge.weapons.crescentbow.CrescentBowListener;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipeDiscoveryListener;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipes;
import pepin.pepeforge.weapons.greatsword.GreatswordListener;
import pepin.pepeforge.weapons.greatsword.GreatswordRecipeDiscoveryListener;
import pepin.pepeforge.weapons.greatsword.GreatswordRecipes;
import pepin.pepeforge.weapons.windblade.WindBladeListener;
import pepin.pepeforge.weapons.windblade.WindBladeRecipeDiscoveryListener;
import pepin.pepeforge.weapons.windblade.WindBladeRecipes;

public final class PepeForgePlugin extends JavaPlugin {

    private PluginLang lang;
    private ItemFactory itemFactory;
    private ChiselRecipes chiselRecipes;
    private ScytheRecipes scytheRecipes;
    private WindBladeRecipes windBladeRecipes;
    private CrescentBowRecipes crescentBowRecipes;
    private CrescentSpearRecipes crescentSpearRecipes;
    private KatanaRecipes katanaRecipes;
    private GreatswordRecipes greatswordRecipes;
    private GreatswordListener greatswordListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new PluginLang(this);
        itemFactory = new ItemFactory(this, lang);
        chiselRecipes = new ChiselRecipes(this, itemFactory);
        scytheRecipes = new ScytheRecipes(this, itemFactory);
        windBladeRecipes = new WindBladeRecipes(this, itemFactory);
        crescentBowRecipes = new CrescentBowRecipes(this, itemFactory);
        crescentSpearRecipes = new CrescentSpearRecipes(this, itemFactory);
        katanaRecipes = new KatanaRecipes(this, itemFactory);
        greatswordRecipes = new GreatswordRecipes(this, itemFactory);

        chiselRecipes.registerAll();
        scytheRecipes.registerAll();
        windBladeRecipes.registerAll();
        crescentBowRecipes.registerAll();
        crescentSpearRecipes.registerAll();
        katanaRecipes.registerAll();
        greatswordRecipes.registerAll();

        PepeForgeCommand commandExecutor = new PepeForgeCommand(lang, itemFactory);
        PluginCommand command = getCommand("pepeforge");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new CustomItemsMenuListener(lang, itemFactory), this);
        getServer().getPluginManager().registerEvents(new ChiselListener(itemFactory), this);
        ChiselRecipeDiscoveryListener chiselRecipeDiscoveryListener = new ChiselRecipeDiscoveryListener(this);
        getServer().getPluginManager().registerEvents(chiselRecipeDiscoveryListener, this);
        getServer().getPluginManager().registerEvents(new ScytheListener(itemFactory), this);
        ScytheRecipeDiscoveryListener scytheRecipeDiscoveryListener = new ScytheRecipeDiscoveryListener(this);
        getServer().getPluginManager().registerEvents(scytheRecipeDiscoveryListener, this);
        WindBladeListener windBladeListener = new WindBladeListener(this, itemFactory);
        windBladeListener.startHoldingTask();
        getServer().getPluginManager().registerEvents(windBladeListener, this);
        WindBladeRecipeDiscoveryListener windBladeRecipeDiscoveryListener = new WindBladeRecipeDiscoveryListener(this, itemFactory);
        getServer().getPluginManager().registerEvents(windBladeRecipeDiscoveryListener, this);
        getServer().getPluginManager().registerEvents(new CrescentBowListener(itemFactory), this);
        CrescentBowRecipeDiscoveryListener crescentBowRecipeDiscoveryListener = new CrescentBowRecipeDiscoveryListener(this);
        getServer().getPluginManager().registerEvents(crescentBowRecipeDiscoveryListener, this);
        CrescentMoonGlow crescentMoonGlow = new CrescentMoonGlow(this, itemFactory);
        crescentMoonGlow.startTask();
        CrescentSpearListener crescentSpearListener = new CrescentSpearListener(this, itemFactory, lang);
        crescentSpearListener.startStatusTask();
        getServer().getPluginManager().registerEvents(crescentSpearListener, this);
        CrescentSpearRecipeDiscoveryListener crescentSpearRecipeDiscoveryListener = new CrescentSpearRecipeDiscoveryListener(this);
        getServer().getPluginManager().registerEvents(crescentSpearRecipeDiscoveryListener, this);
        KatanaListener katanaListener = new KatanaListener(this, itemFactory, lang);
        katanaListener.startStatusTask();
        getServer().getPluginManager().registerEvents(katanaListener, this);
        KatanaRecipeDiscoveryListener katanaRecipeDiscoveryListener = new KatanaRecipeDiscoveryListener(this);
        getServer().getPluginManager().registerEvents(katanaRecipeDiscoveryListener, this);
        greatswordListener = new GreatswordListener(this, itemFactory, lang);
        greatswordListener.startStatusTask();
        getServer().getPluginManager().registerEvents(greatswordListener, this);
        GreatswordRecipeDiscoveryListener greatswordRecipeDiscoveryListener = new GreatswordRecipeDiscoveryListener(this, itemFactory);
        getServer().getPluginManager().registerEvents(greatswordRecipeDiscoveryListener, this);

        RecipeDiscoveryRefresher recipeDiscoveryRefresher = new RecipeDiscoveryRefresher(this, player -> {
            chiselRecipeDiscoveryListener.discoverFor(player);
            scytheRecipeDiscoveryListener.discoverFor(player);
            windBladeRecipeDiscoveryListener.discoverFor(player);
            crescentBowRecipeDiscoveryListener.discoverFor(player);
            crescentSpearRecipeDiscoveryListener.discoverFor(player);
            katanaRecipeDiscoveryListener.discoverFor(player);
            greatswordRecipeDiscoveryListener.discoverFor(player);
        });
        getServer().getPluginManager().registerEvents(recipeDiscoveryRefresher, this);
        recipeDiscoveryRefresher.refreshAllOnlinePlayers();
    }

    @Override
    public void onDisable() {
        if (chiselRecipes != null) {
            chiselRecipes.unregisterAll();
        }
        if (scytheRecipes != null) {
            scytheRecipes.unregisterAll();
        }
        if (windBladeRecipes != null) {
            windBladeRecipes.unregisterAll();
        }
        if (crescentBowRecipes != null) {
            crescentBowRecipes.unregisterAll();
        }
        if (crescentSpearRecipes != null) {
            crescentSpearRecipes.unregisterAll();
        }
        if (katanaRecipes != null) {
            katanaRecipes.unregisterAll();
        }
        if (greatswordRecipes != null) {
            greatswordRecipes.unregisterAll();
        }
        if (greatswordListener != null) {
            greatswordListener.clearAllPlayerState();
        }
    }
}
