package pepin.pepeforge.weapons.stormcleaver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.item.ItemFactory;
import pepin.pepeforge.util.aura.AuraEffect;

public final class StormcleaverAuraEffect implements AuraEffect {

    private final ItemFactory itemFactory;
    private final org.bukkit.NamespacedKey chargesKey;
    private final JavaPlugin plugin;

    public StormcleaverAuraEffect(JavaPlugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.chargesKey = new org.bukkit.NamespacedKey(plugin, StormcleaverDefinition.CHARGES_KEY_STRING);
    }

    @Override
    public int getTickInterval() {
        return 4;
    }

    @Override
    public void tick(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        int requiredCharges = Math.max(1, Math.min(10, plugin.getConfig().getInt(
                "mechanics.stormcleaver.charges_required", StormcleaverDefinition.DEFAULT_CHARGES_REQUIRED)));
        int charges = player.getPersistentDataContainer().getOrDefault(
                chargesKey, PersistentDataType.INTEGER, 0);

        if (!itemFactory.isStormcleaver(held)
                || (offHand != null && !offHand.getType().isAir())
                || charges < requiredCharges) {
            return;
        }

        Location center = player.getLocation().add(0.0D, 1.0D, 0.0D);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 10,
                0.35D, 0.65D, 0.35D, 0.04D);
    }
}
