package pepin.pepeforge.util.ui;

import org.bukkit.entity.Player;

public final class SpigotActionBarSenderImpl implements ActionBarSender {

    @Override
    public void send(Player player, String message) {
        try {
            String colored = pepin.pepeforge.util.ColorUtil.translate(message);
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(colored)
            );
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void sendEmpty(Player player) {
        try {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("")
            );
        } catch (Throwable ignored) {
        }
    }
}
