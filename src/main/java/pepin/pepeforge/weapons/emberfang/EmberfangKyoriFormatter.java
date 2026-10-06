package pepin.pepeforge.weapons.emberfang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.itemmeta.paper.PaperDataComponentAdapter;

import java.util.ArrayList;
import java.util.List;

public final class EmberfangKyoriFormatter implements EmberfangTextFormatter {

    @Override
    public void applyTranslatedText(ItemStack item, String fireDamage) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable(EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.1"));
        lore.add(Component.translatable(EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.2"));
        lore.add(Component.translatable(
                EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.3",
                Component.text(fireDamage, NamedTextColor.DARK_AQUA)));
        lore.add(Component.translatable(EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.4"));
        lore.add(Component.translatable(EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.5"));
        lore.add(Component.translatable(EmberfangDefinition.TRANSLATION_KEY_BASE + ".lore.6"));
        PaperDataComponentAdapter.applyRawComponents(item, null, lore);
    }
}
