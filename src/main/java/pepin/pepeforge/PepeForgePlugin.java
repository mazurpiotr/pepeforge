package pepin.pepeforge;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.command.PepeForgeCommand;
import pepin.pepeforge.gui.CustomItemsMenuListener;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.tools.chisel.ChiselListener;
import pepin.pepeforge.tools.chisel.ChiselRecipes;
import pepin.pepeforge.tools.scythe.ScytheListener;
import pepin.pepeforge.tools.scythe.ScytheRecipeDiscoveryListener;
import pepin.pepeforge.tools.scythe.ScytheRecipes;
import pepin.pepeforge.weapons.katana.KatanaListener;
import pepin.pepeforge.weapons.katana.KatanaRecipeDiscoveryListener;
import pepin.pepeforge.weapons.katana.KatanaRecipes;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearListener;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearRecipeDiscoveryListener;
import pepin.pepeforge.weapons.crescentspear.CrescentSpearRecipes;
import pepin.pepeforge.weapons.crescentbow.CrescentBowListener;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipeDiscoveryListener;
import pepin.pepeforge.weapons.crescentbow.CrescentBowRecipes;
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

        chiselRecipes.registerAll();
        scytheRecipes.registerAll();
        windBladeRecipes.registerAll();
        crescentBowRecipes.registerAll();
        crescentSpearRecipes.registerAll();
        katanaRecipes.registerAll();

        PepeForgeCommand commandExecutor = new PepeForgeCommand(lang, itemFactory);
        PluginCommand command = getCommand("pepeforge");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(new CustomItemsMenuListener(lang, itemFactory), this);
        getServer().getPluginManager().registerEvents(new ChiselListener(itemFactory), this);
        getServer().getPluginManager().registerEvents(new ScytheListener(itemFactory), this);
        getServer().getPluginManager().registerEvents(new ScytheRecipeDiscoveryListener(this), this);
        WindBladeListener windBladeListener = new WindBladeListener(this, itemFactory);
        windBladeListener.startHoldingTask();
        getServer().getPluginManager().registerEvents(windBladeListener, this);
        getServer().getPluginManager().registerEvents(new WindBladeRecipeDiscoveryListener(this, itemFactory), this);
        getServer().getPluginManager().registerEvents(new CrescentBowListener(itemFactory), this);
        getServer().getPluginManager().registerEvents(new CrescentBowRecipeDiscoveryListener(this), this);
        CrescentSpearListener crescentSpearListener = new CrescentSpearListener(this, itemFactory, lang);
        crescentSpearListener.startStatusTask();
        getServer().getPluginManager().registerEvents(crescentSpearListener, this);
        getServer().getPluginManager().registerEvents(new CrescentSpearRecipeDiscoveryListener(this), this);
        KatanaListener katanaListener = new KatanaListener(this, itemFactory, lang);
        katanaListener.startStatusTask();
        getServer().getPluginManager().registerEvents(katanaListener, this);
        getServer().getPluginManager().registerEvents(new KatanaRecipeDiscoveryListener(this), this);
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
    }
}
