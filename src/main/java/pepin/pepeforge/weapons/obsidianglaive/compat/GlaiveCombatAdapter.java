package pepin.pepeforge.weapons.obsidianglaive.compat;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;

public interface GlaiveCombatAdapter extends Listener {
    Attack capture(Player player, LivingEntity primary);

    void clear(Player player);

    void stop();

    interface Attack {
        boolean sweepAllowed();

        double sweepDamage(LivingEntity secondary);

        default DamageSource sweepDamageSource() {
            return null;
        }

        default void afterSweepHit(LivingEntity secondary) {
        }

        default double stabDamage(LivingEntity secondary) {
            return 0.0D;
        }

        default DamageSource stabDamageSource() {
            return null;
        }

        default void afterStabHit(LivingEntity secondary) {
        }
    }

    static GlaiveCombatAdapter create(JavaPlugin plugin, ItemFactory factory) {
        NativeGlaiveCombat nativeCombat = NativeGlaiveCombat.discover(plugin);
        try {
            Class.forName("io.papermc.paper.event.player.PrePlayerAttackEntityEvent");
        } catch (ClassNotFoundException exception) {
            return new BukkitGlaiveCombatAdapter(plugin, factory, nativeCombat);
        }
        try {
            return (GlaiveCombatAdapter) Class.forName(
                    "pepin.pepeforge.weapons.obsidianglaive.compat.PaperGlaiveCombatAdapter")
                    .getConstructor(JavaPlugin.class, ItemFactory.class, NativeGlaiveCombat.class)
                    .newInstance(plugin, factory, nativeCombat);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not initialize Obsidian Glaive combat", exception);
        }
    }
}
