package pepin.pepeforge.util.ui;

import org.bukkit.entity.Player;
import pepin.pepeforge.util.env.AdventureReflect;

public final class ActionBarHelper {

    private static final ActionBarSender SENDER;

    static {
        ActionBarSender temp = null;
        if (AdventureReflect.isSupported()) {
            try {
                temp = (ActionBarSender) Class.forName("pepin.pepeforge.util.ui.AdventureActionBarSenderImpl")
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        SENDER = temp != null ? temp : new SpigotActionBarSenderImpl();
    }

    private ActionBarHelper() {
    }

    /**
     * Sends an action bar message to the player.
     * Uses Adventure API if supported (Paper/Folia), otherwise falls back to Spigot API (Spigot/CraftBukkit).
     */
    public static void showActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) {
            SENDER.sendEmpty(player);
        } else {
            SENDER.send(player, message);
        }
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
