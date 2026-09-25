package pepin.pepeforge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class OnlinePlayerNames implements Listener {

    private final Set<String> names = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        names.add(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        names.remove(event.getPlayer().getName());
    }

    public List<String> matching(String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                result.add(name);
            }
        }

        return result;
    }
}