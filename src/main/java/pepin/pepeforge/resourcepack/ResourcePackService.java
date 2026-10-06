package pepin.pepeforge.resourcepack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackService implements Listener {

    private static final String GITHUB_PACK_URL_TEMPLATE =
            "https://github.com/mazurpiotr/pepeforge/releases/download/v%s/PepeForge-ResourcePack.zip";
    private static final int SHA1_LENGTH = 20;
    private static final int CONNECTION_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_SHA1_RESPONSE_BYTES = 4_096;

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerResourcePackStatusEvent.Status> statuses = new ConcurrentHashMap<>();

    private volatile boolean active;
    private volatile ResourcePackMode mode = ResourcePackMode.MANAGER;
    private volatile String packUrl;
    private volatile byte[] packHash;
    private volatile UUID packId;
    private volatile ScheduledTaskCompat hashTask;

    public ResourcePackService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        active = true;
        mode = readMode();
        if (mode == ResourcePackMode.MANAGER) {
            plugin.getLogger().info("Resource pack delivery mode: MANAGER");
            return;
        }

        String version = plugin.getDescription().getVersion();
        packUrl = String.format(GITHUB_PACK_URL_TEMPLATE, version);
        packId = UUID.randomUUID();
        plugin.getLogger().info("Resource pack delivery mode: GITHUB");

        byte[] embeddedHash = loadEmbeddedHash();
        if (embeddedHash != null) {
            plugin.getLogger().info("Using the embedded GitHub Resource Pack SHA-1");
            publishHash(embeddedHash);
            return;
        }

        plugin.getLogger().info("Loading Resource Pack SHA-1 from " + packUrl + ".sha1");

        try {
            hashTask = SchedulerCompat.runLaterAsync(plugin, this::loadHash, 1L);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Could not schedule Resource Pack hash loading: " + exception.getMessage());
        }
    }

    public void stop() {
        active = false;
        ScheduledTaskCompat task = hashTask;
        hashTask = null;
        if (task != null) {
            task.cancel();
        }
        statuses.clear();
        packHash = null;
        packId = null;
        packUrl = null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isGithubMode()) {
            return;
        }

        Player player = event.getPlayer();
        if (packHash == null) {
            return;
        }

        sendToPlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        statuses.remove(playerId);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        UUID currentPackId = packId;
        if (!isGithubMode() || currentPackId == null || !currentPackId.equals(event.getID())) {
            return;
        }

        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        statuses.put(event.getPlayer().getUniqueId(), status);
        switch (status) {
            case ACCEPTED, DOWNLOADED -> plugin.getLogger().info(
                    "Resource Pack " + status.name().toLowerCase(Locale.ROOT) + " by " + event.getPlayer().getName());
            case SUCCESSFULLY_LOADED -> plugin.getLogger().info(
                    "Resource Pack successfully loaded by " + event.getPlayer().getName());
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> plugin.getLogger().warning(
                    "Resource Pack " + status.name().toLowerCase(Locale.ROOT) + " for " + event.getPlayer().getName());
        }
    }

    private ResourcePackMode readMode() {
        String configured = plugin.getConfig().getString("resource_pack.mode", ResourcePackMode.MANAGER.name());
        if (configured == null || configured.isBlank()) {
            plugin.getLogger().warning("Missing or blank resource_pack.mode; using MANAGER");
            return ResourcePackMode.MANAGER;
        }

        ResourcePackMode parsed = ResourcePackMode.parse(configured);
        if (!parsed.name().equalsIgnoreCase(configured.trim())) {
            plugin.getLogger().warning("Unknown resource_pack.mode '" + configured + "'; using MANAGER");
        }
        return parsed;
    }

    private boolean isGithubMode() {
        return active && mode == ResourcePackMode.GITHUB;
    }

    private byte[] loadEmbeddedHash() {
        try (InputStream input = plugin.getResource("resource-pack.properties")) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            String configured = properties.getProperty("sha1");
            if (configured == null || configured.isBlank()) {
                return null;
            }

            try {
                return parseSha1(configured);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "The embedded Resource Pack SHA-1 is invalid; falling back to the .sha1 sidecar");
                return null;
            }
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not read embedded Resource Pack metadata; falling back to the .sha1 sidecar: "
                            + exception.getMessage());
            return null;
        }
    }

    private void loadHash() {
        try {
            byte[] loadedHash = downloadSha1(packUrl + ".sha1");
            if (!active) {
                return;
            }
            SchedulerCompat.run(plugin, () -> publishHash(loadedHash));
        } catch (IOException | IllegalArgumentException exception) {
            if (active) {
                plugin.getLogger().severe("Could not load the GitHub Resource Pack SHA-1: " + exception.getMessage());
            }
        }
    }

    private void publishHash(byte[] loadedHash) {
        if (!isGithubMode()) {
            return;
        }

        packHash = loadedHash.clone();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendToPlayer(player);
        }
    }

    private void sendToPlayer(Player player) {
        UUID currentPackId = packId;
        byte[] currentHash = packHash;
        String currentUrl = packUrl;
        if (!isGithubMode() || currentPackId == null || currentHash == null || currentUrl == null) {
            return;
        }

        SchedulerCompat.runForPlayer(player, plugin, () -> {
            if (!isGithubMode() || !player.isOnline()) {
                return;
            }
            player.setResourcePack(currentPackId, currentUrl, currentHash.clone(), (String) null, false);
        });
    }

    static String buildPackUrl(String version) {
        return String.format(GITHUB_PACK_URL_TEMPLATE, version);
    }

    static byte[] parseSha1(String response) {
        String[] fields = response.trim().split("\\s+");
        if (fields.length == 0 || !fields[0].matches("(?i)[0-9a-f]{40}")) {
            throw new IllegalArgumentException("The SHA-1 sidecar does not contain a valid SHA-1 hash");
        }

        byte[] hash = HexFormat.of().parseHex(fields[0]);
        if (hash.length != SHA1_LENGTH) {
            throw new IllegalArgumentException("The Resource Pack SHA-1 hash has an invalid length");
        }
        return hash;
    }

    private static byte[] downloadSha1(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        if (!(connection instanceof HttpURLConnection http)) {
            throw new IOException("The Resource Pack hash URL is not an HTTP URL");
        }

        http.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        http.setReadTimeout(READ_TIMEOUT_MILLIS);
        http.setInstanceFollowRedirects(true);
        http.setRequestProperty("User-Agent", "PepeForge-ResourcePack");
        try {
            int responseCode = http.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("GitHub returned HTTP " + responseCode);
            }

            try (InputStream input = http.getInputStream()) {
                byte[] response = input.readNBytes(MAX_SHA1_RESPONSE_BYTES);
                return parseSha1(new String(response, StandardCharsets.US_ASCII));
            }
        } finally {
            http.disconnect();
        }
    }
}
