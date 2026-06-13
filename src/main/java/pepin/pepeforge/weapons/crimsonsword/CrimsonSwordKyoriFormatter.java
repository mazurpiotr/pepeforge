package pepin.pepeforge.weapons.crimsonsword;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CrimsonSwordKyoriFormatter {

    private CrimsonSwordKyoriFormatter() {}

    public static void applyTranslatedText(ItemStack item, CrimsonSwordManager manager, int level, double xp, double requiredXp) {
        Component name = Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".name", NamedTextColor.DARK_RED)
                .append(Component.text(" [Lv. " + level + "]", NamedTextColor.GRAY));

        List<Component> features = new ArrayList<>();
        features.add(Component.translatable(
            CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.edge",
            Component.text(manager.damageBonusPercent(level), NamedTextColor.RED)
        ).color(NamedTextColor.RED));

        double lifesteal = manager.lifesteal(level);
        if (lifesteal <= 0.0D) {
            features.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.lifesteal_locked").color(NamedTextColor.DARK_GRAY));
        } else {
            int nextLevel = manager.nextLifestealUnlockLevel(level);
            double nextPercent = manager.nextLifestealPercent(nextLevel);
            if (nextLevel > level && nextPercent > lifesteal) {
                features.add(Component.translatable(
                        CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.lifesteal_next",
                        Component.text(manager.formatPercent(lifesteal * 100), NamedTextColor.RED),
                        Component.text(String.valueOf(nextLevel), NamedTextColor.RED),
                        Component.text(manager.formatPercent(nextPercent * 100), NamedTextColor.RED)
                ).color(NamedTextColor.RED));
            } else {
                features.add(Component.translatable(
                        CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.lifesteal",
                        Component.text(manager.formatPercent(lifesteal * 100), NamedTextColor.RED)
                ).color(NamedTextColor.RED));
            }
        }

        if (level < 10) {
            features.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.aura_locked").color(NamedTextColor.DARK_GRAY));
        } else {
            String seconds = String.valueOf(manager.auraDurationTicks(level) / 20);
            String drain = manager.formatPercent(manager.auraDrainAmount(level));
            String radius = manager.formatPercent(CrimsonSwordDefinition.AURA_RADIUS);
            int nextLevel = manager.nextAuraUnlockLevel(level);
            String nextSeconds = String.valueOf(manager.auraDurationTicks(nextLevel) / 20);
            String nextDrain = manager.formatPercent(manager.auraDrainAmount(nextLevel));

            if (nextLevel > level) {
                features.add(Component.translatable(
                        CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.aura_next",
                        Component.text(seconds, NamedTextColor.DARK_RED),
                        Component.text(drain, NamedTextColor.DARK_RED),
                        Component.text(radius, NamedTextColor.DARK_RED),
                        Component.text(String.valueOf(nextLevel), NamedTextColor.DARK_RED),
                        Component.text(nextSeconds, NamedTextColor.DARK_RED),
                        Component.text(nextDrain, NamedTextColor.DARK_RED)
                ).color(NamedTextColor.DARK_RED));
            } else {
                features.add(Component.translatable(
                        CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".feature.aura",
                        Component.text(seconds, NamedTextColor.DARK_RED),
                        Component.text(drain, NamedTextColor.DARK_RED),
                        Component.text(radius, NamedTextColor.DARK_RED)
                ).color(NamedTextColor.DARK_RED));
            }
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.1"));
        lore.add(Component.translatable(
            CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.level",
            Component.text(String.valueOf(level), NamedTextColor.RED),
            Component.text(manager.formatXp(xp), NamedTextColor.GRAY),
            level >= CrimsonSwordDefinition.MAX_LEVEL
                    ? Component.text("MAX", NamedTextColor.GOLD)
                    : Component.text(manager.formatXp(requiredXp), NamedTextColor.GRAY)
        ));
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.2"));
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.3"));
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.unlocked"));
        if (features.isEmpty()) {
            lore.add(Component.text("-", NamedTextColor.DARK_GRAY));
        } else {
            lore.addAll(features);
        }
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.rarity").color(NamedTextColor.GOLD));
        lore.add(Component.translatable(CrimsonSwordDefinition.TRANSLATION_KEY_BASE + ".lore.1"));

        pepin.pepeforge.util.PaperDataComponentAdapter.applyRawComponents(item, name, lore);
    }
}
