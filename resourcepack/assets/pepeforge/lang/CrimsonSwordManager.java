package pepin.pepeforge.items.crimson;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pepin.pepeforge.PepeForge;

public class CrimsonSwordManager {
    private final PepeForge plugin;
    private final NamespacedKey xpKey;
    private final NamespacedKey levelKey;
    private static final double BASE_XP = 500.0;

    public CrimsonSwordManager(PepeForge plugin) {
        this.plugin = plugin;
        this.xpKey = new NamespacedKey(plugin, "crimson_xp");
        this.levelKey = new NamespacedKey(plugin, "crimson_level");
    }

    public void addXp(ItemStack item, double amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        double currentXp = pdc.getOrDefault(xpKey, PersistentDataType.DOUBLE, 0.0);
        int currentLevel = pdc.getOrDefault(levelKey, PersistentDataType.INTEGER, 1);

        if (currentLevel >= 30) return;

        currentXp += amount;
        double required = getRequiredXp(currentLevel);

        while (currentXp >= required && currentLevel < 30) {
            currentXp -= required;
            currentLevel++;
            required = getRequiredXp(currentLevel);
            // TODO: Play level up sound/effect
        }

        pdc.set(xpKey, PersistentDataType.DOUBLE, currentXp);
        pdc.set(levelKey, PersistentDataType.INTEGER, currentLevel);
        item.setItemMeta(meta);
        updateLore(item, currentLevel, currentXp, required);
    }

    public double getRequiredXp(int level) {
        return Math.floor(BASE_XP * Math.pow(1.15, level - 1));
    }

    private void updateLore(ItemStack item, int level, double xp, double max) {
        // Implementation using ItemMetaCompat to handle placeholders and translations
        // This would call plugin's internal lore builder
    }

    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
    }
}