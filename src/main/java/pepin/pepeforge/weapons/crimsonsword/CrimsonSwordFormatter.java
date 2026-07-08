package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.inventory.ItemStack;

public interface CrimsonSwordFormatter {
    void applyTranslatedText(ItemStack item, CrimsonSwordManager manager, int level, double xp, double requiredXp);
}
