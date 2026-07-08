package pepin.pepeforge.weapons.crimsonsword;

import org.bukkit.inventory.ItemStack;

public final class FallbackCrimsonSwordFormatterImpl implements CrimsonSwordFormatter {

    @Override
    public void applyTranslatedText(ItemStack item, CrimsonSwordManager manager, int level, double xp, double requiredXp) {
        // No-op on Spigot/Bukkit
    }
}
