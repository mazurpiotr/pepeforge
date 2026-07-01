package pepin.pepeforge.util.ui;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class ActionBarHelper {

    private ActionBarHelper() {
    }

    /**
     * Sends an action bar message to the player using Adventure API, supporting legacy '&' color codes.
     */
    public static void showActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) {
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            return;
        }
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    /**
     * Builds a string-based ASCII progress bar using '&a' and '&8' colors.
     * Length is fixed to 20 characters by default.
     *
     * @param progress value from 0.0 to 1.0
     * @return colored legacy string representing the progress bar
     */
    public static String buildProgressBar(double progress) {
        int filled = (int) Math.round(progress * 20);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? "&a|" : "&8|");
        }
        return bar.toString();
    }
}
