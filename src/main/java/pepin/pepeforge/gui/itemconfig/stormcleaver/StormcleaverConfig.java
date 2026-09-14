package pepin.pepeforge.gui.itemconfig.stormcleaver;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.util.ColorUtil;
import pepin.pepeforge.weapons.stormcleaver.StormcleaverDefinition;

import java.util.List;

public class StormcleaverConfig {

    private final PepeForgePlugin plugin;

    public StormcleaverConfig(PepeForgePlugin plugin) {
        this.plugin = plugin;
    }

    public void build(Inventory inventory) {
        ItemStack resetBtn = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetBtn.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(ColorUtil.RED + "Reset Stormcleaver Defaults");
            resetMeta.setLore(List.of(ColorUtil.GRAY + "Reset shared dive settings"));
            resetBtn.setItemMeta(resetMeta);
        }
        inventory.setItem(18, resetBtn);

        ItemStack chargesBtn = new ItemStack(ConfigIconography.CHARGES);
        ItemMeta chargesMeta = chargesBtn.getItemMeta();
        if (chargesMeta != null) {
                int charges = plugin.getConfig().getInt("mechanics.stormcleaver.charges_required",
                    StormcleaverDefinition.DEFAULT_CHARGES_REQUIRED);
            chargesMeta.setDisplayName(ColorUtil.GOLD + "Required Charges: " + ColorUtil.GREEN + charges);
            chargesMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -1",
                    ColorUtil.GRAY + "Right-Click: +1"));
            chargesBtn.setItemMeta(chargesMeta);
        }
        inventory.setItem(19, chargesBtn);

        ItemStack multiplierBtn = new ItemStack(ConfigIconography.STRENGTH);
        ItemMeta multiplierMeta = multiplierBtn.getItemMeta();
        if (multiplierMeta != null) {
                double multiplier = plugin.getConfig().getDouble("mechanics.stormcleaver.jump_velocity_multiplier",
                    StormcleaverDefinition.DEFAULT_JUMP_VELOCITY_MULTIPLIER);
            multiplierMeta.setDisplayName(ColorUtil.GOLD + "Jump Multiplier: " + ColorUtil.GREEN + multiplier);
            multiplierMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.1",
                    ColorUtil.GRAY + "Right-Click: +0.1"));
            multiplierBtn.setItemMeta(multiplierMeta);
        }
        inventory.setItem(20, multiplierBtn);

        ItemStack decayBtn = new ItemStack(ConfigIconography.DECAY);
        ItemMeta decayMeta = decayBtn.getItemMeta();
        if (decayMeta != null) {
                int decay = plugin.getConfig().getInt("mechanics.stormcleaver.charge_decay_interval",
                    StormcleaverDefinition.DEFAULT_CHARGE_DECAY_INTERVAL);
            decayMeta.setDisplayName(ColorUtil.GOLD + "Charge Decay: " + ColorUtil.GREEN
                    + (decay / 20.0D) + "s (" + decay + " ticks)");
            decayMeta.setLore(List.of(
                    ColorUtil.GRAY + "Left-Click: -0.5s (-10 ticks)",
                    ColorUtil.GRAY + "Right-Click: +0.5s (+10 ticks)"));
            decayBtn.setItemMeta(decayMeta);
        }
        inventory.setItem(21, decayBtn);
    }
}
