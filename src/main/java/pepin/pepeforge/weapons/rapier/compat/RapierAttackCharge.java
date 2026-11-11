package pepin.pepeforge.weapons.rapier.compat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public interface RapierAttackCharge extends Listener {
    float consume(Player attacker, LivingEntity target);

    default void clear(Player player) {
    }

    default void stop() {
    }

    static RapierAttackCharge bukkit() {
        return new RapierAttackCharge() {
            @Override
            public float consume(Player attacker, LivingEntity target) {
                return attacker.getAttackCooldown();
            }
        };
    }

    static RapierAttackCharge create() {
        try {
            Class.forName("io.papermc.paper.event.player.PrePlayerAttackEntityEvent");
        } catch (ClassNotFoundException ignored) {
            return bukkit();
        }
        try {
            return (RapierAttackCharge) Class.forName(
                    "pepin.pepeforge.weapons.rapier.compat.PaperRapierAttackCharge")
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not initialize rapier attack charge tracking", exception);
        }
    }
}
