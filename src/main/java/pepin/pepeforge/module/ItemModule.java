package pepin.pepeforge.module;

import org.bukkit.entity.Player;

public interface ItemModule {
    void onEnable();
    void onDisable();
    void discoverRecipesFor(Player player);
}
