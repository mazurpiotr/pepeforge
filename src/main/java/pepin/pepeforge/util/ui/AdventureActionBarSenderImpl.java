package pepin.pepeforge.util.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class AdventureActionBarSenderImpl implements ActionBarSender {

    @Override
    public void send(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    @Override
    public void sendEmpty(Player player) {
        player.sendActionBar(Component.empty());
    }
}
