package pepin.pepeforge.util.ui;

import org.bukkit.entity.Player;

public interface ActionBarSender {
    void send(Player player, String message);
    void sendEmpty(Player player);
}
