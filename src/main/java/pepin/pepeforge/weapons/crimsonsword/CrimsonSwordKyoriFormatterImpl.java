package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.inventory.ItemStack;

public final class CrimsonSwordKyoriFormatterImpl implements CrimsonSwordFormatter {

    @Override
    public void applyTranslatedText(ItemStack item, CrimsonSwordManager manager, int level, double xp, double requiredXp) {
        CrimsonSwordKyoriFormatter.applyTranslatedText(item, manager, level, xp, requiredXp);
    }
}
