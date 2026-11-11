package pepin.pepeforge.gui.itemconfig.obsidianglaive;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pepin.pepeforge.PepeForgePlugin;
import pepin.pepeforge.gui.ConfigIconography;
import pepin.pepeforge.lang.PluginLang;
import pepin.pepeforge.weapons.obsidianglaive.ObsidianGlaiveSettings;

public final class ObsidianGlaiveConfig {

    private final PepeForgePlugin plugin;
    private final PluginLang lang;

    public ObsidianGlaiveConfig(PepeForgePlugin plugin, PluginLang lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void build(Inventory inventory) {
        ItemStack resetButton = new ItemStack(ConfigIconography.RESET);
        ItemMeta resetMeta = resetButton.getItemMeta();
        if (resetMeta != null) {
            resetMeta.setDisplayName(lang.text("messages.config.obsidian_glaive.reset.title"));
            resetMeta.setLore(List.of(lang.text("messages.config.obsidian_glaive.reset.lore")));
            resetButton.setItemMeta(resetMeta);
        }
        inventory.setItem(18, resetButton);

        ObsidianGlaiveSettings settings = ObsidianGlaiveSettings.fromConfig(plugin.getConfig());
        ItemStack damageButton = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta damageMeta = damageButton.getItemMeta();
        if (damageMeta != null) {
            damageMeta.setDisplayName(format("messages.config.obsidian_glaive.damage.title",
                    "damage", String.format(Locale.ROOT, "%.1f", settings.damage())));
            damageMeta.setLore(List.of(
                    format("messages.config.obsidian_glaive.damage.range",
                            "min", formatNumber(ObsidianGlaiveSettings.MIN_DAMAGE),
                            "max", formatNumber(ObsidianGlaiveSettings.MAX_DAMAGE)),
                    lang.text("messages.config.obsidian_glaive.damage.decrease"),
                    lang.text("messages.config.obsidian_glaive.damage.increase")));
            damageButton.setItemMeta(damageMeta);
        }
        inventory.setItem(19, damageButton);

        inventory.setItem(20, createToggle("stab", settings.stabEnabled()));
        inventory.setItem(21, createToggle("stab_lift", settings.stabLiftEnabled()));
    }

    private ItemStack createToggle(String setting, boolean enabled) {
        ItemStack button = ConfigIconography.createStateButton(enabled);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            String base = "messages.config.obsidian_glaive." + setting;
            meta.setDisplayName(format(base + ".title", "state",
                    lang.text(base + (enabled ? ".enabled" : ".disabled"))));
            meta.setLore(List.of(lang.text(base + ".description"),
                    lang.text("messages.config.obsidian_glaive.toggle")));
            button.setItemMeta(meta);
        }
        return button;
    }

    private String format(String path, String... pairs) {
        String value = lang.text(path);
        for (int index = 0; index < pairs.length; index += 2) {
            value = value.replace("{" + pairs[index] + "}", pairs[index + 1]);
        }
        return value;
    }

    private static String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
