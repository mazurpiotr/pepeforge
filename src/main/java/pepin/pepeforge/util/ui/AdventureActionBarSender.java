package pepin.pepeforge.util.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

final class AdventureActionBarSender {

    private AdventureActionBarSender() {
    }

    static void send(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    static void sendEmpty(Player player) {
        player.sendActionBar(Component.empty());
    }
}
