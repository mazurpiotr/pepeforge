package pepin.pepeforge.util.ui;

import org.bukkit.entity.Player;

public final class ActionBarHelper {

    private static final boolean ADVENTURE_SUPPORTED;

    static {
        boolean temp;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            temp = true;
        } catch (ClassNotFoundException e) {
            temp = false;
        }
        ADVENTURE_SUPPORTED = temp;
    }

    private ActionBarHelper() {
    }

    /**
     * Sends an action bar message to the player.
     * Uses Adventure API if supported (Paper/Folia), otherwise falls back to Spigot API (Spigot/CraftBukkit).
     */
    public static void showActionBar(Player player, String message) {
        if (ADVENTURE_SUPPORTED) {
            try {
                if (message == null || message.isEmpty()) {
                    AdventureActionBarSender.sendEmpty(player);
                } else {
                    AdventureActionBarSender.send(player, message);
                }
                return;
            } catch (Throwable ignored) {
                // Fallback to Spigot API if Adventure call unexpectedly fails
            }
        }

        // Fallback for Spigot / CraftBukkit
        sendSpigot(player, message);
    }

    private static void sendSpigot(Player player, String message) {
        try {
            if (message == null || message.isEmpty()) {
                player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent("")
                );
            } else {
                String colored = pepin.pepeforge.util.ColorUtil.translate(message);
                player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(colored)
                );
            }
        } catch (Throwable ignored) {
            // Completely ignore to prevent any server-side logs/crashes on pure vanilla Bukkit
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
