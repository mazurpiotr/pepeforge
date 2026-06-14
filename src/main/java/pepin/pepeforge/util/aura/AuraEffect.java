package pepin.pepeforge.util.aura;

import org.bukkit.entity.Player;

public interface AuraEffect {

    void tick(Player player);
    int getTickInterval();
}
