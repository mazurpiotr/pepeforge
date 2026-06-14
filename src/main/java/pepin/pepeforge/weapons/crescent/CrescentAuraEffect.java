package pepin.pepeforge.weapons.crescent;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.aura.AuraEffect;

public final class CrescentAuraEffect implements AuraEffect {

    private static final double FORWARD_OFFSET = 0.45D;
    private static final double SIDE_OFFSET = 0.18D;
    private static final double VERTICAL_OFFSET = -0.35D;

    private final ItemFactory itemFactory;

    public CrescentAuraEffect(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @Override
    public int getTickInterval() {
        return 4;
    }

    @Override
    public void tick(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isHoldingCrescentWeapon(held)) {
            return;
        }

        if (!CrescentMoonPower.isMoonlit(player)) {
            return;
        }

        playCrescentMoonAura(player);
    }

    private boolean isHoldingCrescentWeapon(ItemStack item) {
        return itemFactory.isCrescentBow(item) || itemFactory.isCrescentSpear(item);
    }

    private void playCrescentMoonAura(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();
        double fx = dir.getX();
        double fz = dir.getZ();
        double length = Math.sqrt(fx * fx + fz * fz);
        if (length < 1.0E-6D) {
            fx = 0.0D;
            fz = 1.0D;
        } else {
            fx /= length;
            fz /= length;
        }

        double rx = fz;
        double rz = -fx;

        double handX = eyeLoc.getX() + (fx * FORWARD_OFFSET) + (rx * SIDE_OFFSET);
        double handY = eyeLoc.getY() + VERTICAL_OFFSET;
        double handZ = eyeLoc.getZ() + (fz * FORWARD_OFFSET) + (rz * SIDE_OFFSET);

        long time = player.getTicksLived();

        for (int point = 0; point < 2; point++) {
            double phase = (time * 0.22D) + (point * Math.PI);
            double cosPhase = Math.cos(phase);
            double sinPhase = Math.sin(phase);

            double ox = handX + (rx * cosPhase * 0.16D) + (fx * sinPhase * 0.07D);
            double oy = handY + (Math.sin(phase + (Math.PI * 0.35D)) * 0.09D);
            double oz = handZ + (rz * cosPhase * 0.16D) + (fz * sinPhase * 0.07D);

            player.getWorld().spawnParticle(
                    Particle.WAX_OFF,
                    ox, oy, oz,
                    1,
                    0.01D,
                    0.01D,
                    0.01D,
                    0.0D
            );
        }

        if (time % 8L == 0L) {
            double moteY = handY + 0.08D + (Math.sin(time * 0.08D) * 0.04D);
            player.getWorld().spawnParticle(
                    Particle.END_ROD,
                    handX, moteY, handZ,
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.002D
            );
        }
    }
}
