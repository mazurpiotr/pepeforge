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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourcePackService implements Listener {

    private static final String GITHUB_PACK_URL_TEMPLATE =
            "https://github.com/mazurpiotr/pepeforge/releases/download/v%s/PepeForge-ResourcePack.zip";
    private static final String PACK_PROPERTIES_RESOURCE = "resource-pack.properties";
    private static final String PACK_PROPERTIES_FILE = "resource-pack.properties";
    private static final int SHA1_LENGTH = 20;
    private static final int CONNECTION_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_SHA1_RESPONSE_BYTES = 4_096;

    private final JavaPlugin plugin;
    private final Path metadataPath;
    private final Map<UUID, PlayerResourcePackStatusEvent.Status> statuses = new ConcurrentHashMap<>();

    private volatile boolean active;
    private volatile ResourcePackMode mode = ResourcePackMode.MANAGER;
    private volatile String packUrl;
    private volatile byte[] packHash;
    private volatile boolean packHashVerified;
    private volatile UUID packId;
    private volatile ScheduledTaskCompat hashTask;

    public ResourcePackService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.metadataPath = plugin.getDataFolder().toPath().resolve(PACK_PROPERTIES_FILE);
    }

    public void start() {
        active = true;
        mode = readMode();
        if (mode == ResourcePackMode.MANAGER) {
            plugin.getLogger().info("Resource pack delivery mode: MANAGER");
            return;
        }

        packId = UUID.randomUUID();
        plugin.getLogger().info("Resource pack delivery mode: GITHUB");
        String pluginVersion = plugin.getDescription().getVersion();
        try {
            hashTask = SchedulerCompat.runLaterAsync(
                    plugin,
                    () -> loadMetadataAndVerify(pluginVersion),
                    1L);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Could not schedule Resource Pack metadata loading: " + exception.getMessage());
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
        packHashVerified = false;
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
            case FAILED_DOWNLOAD -> plugin.getLogger().warning(
                    "Resource Pack failed_download for " + event.getPlayer().getName()
                            + (packHashVerified
                            ? "; the server downloaded the pack and verified its SHA-1, so check the client's "
                                    + "network access, proxy, TLS, and local download state"
                            : "; the server-side SHA-1 check did not complete"));
            case DECLINED, INVALID_URL, FAILED_RELOAD, DISCARDED -> plugin.getLogger().warning(
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

    private void loadMetadataAndVerify(String pluginVersion) {
        if (!active) {
            return;
        }

        createMetadataFileIfMissing();
        if (!active) {
            return;
        }
        ResourcePackMetadata metadata = loadExternalMetadata(pluginVersion);
        if (!active) {
            return;
        }
        if (metadata != null) {
            packUrl = metadata.url();
            plugin.getLogger().info("Using Resource Pack URL and SHA-1 from " + metadata.source());
            verifyAndPublish(metadata.hash());
            return;
        }

        metadata = loadEmbeddedMetadata(pluginVersion);
        if (metadata != null) {
            packUrl = metadata.url();
            plugin.getLogger().warning("Could not load external Resource Pack metadata; checking embedded values");
            verifyAndPublish(metadata.hash());
        } else {
            packUrl = buildPackUrl(pluginVersion);
            plugin.getLogger().warning(
                    "No valid Resource Pack metadata found in the external file or plugin JAR; "
                            + "checking the versioned release online");
            verifyAndPublish(null);
        }
    }

    private ResourcePackMetadata loadExternalMetadata(String pluginVersion) {
        if (!Files.isRegularFile(metadataPath)) {
            plugin.getLogger().info(
                    "External Resource Pack metadata not found at " + metadataPath + "; trying the plugin JAR");
            return null;
        }

        try (InputStream input = Files.newInputStream(metadataPath)) {
            Properties properties = new Properties();
            properties.load(input);
            return parseMetadata(properties, metadataPath.toString(), pluginVersion);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().warning(
                    "External Resource Pack metadata is invalid at " + metadataPath + ": "
                            + exception.getMessage() + "; falling back to the plugin JAR");
            return null;
        }
    }

    private void createMetadataFileIfMissing() {
        if (Files.exists(metadataPath)) {
            return;
        }

        try (InputStream input = plugin.getResource(PACK_PROPERTIES_RESOURCE)) {
            if (input == null) {
                plugin.getLogger().warning(
                        "Resource Pack metadata is missing from the plugin JAR; could not create " + metadataPath);
                return;
            }

            Files.createDirectories(metadataPath.getParent());
            Files.copy(input, metadataPath);
            plugin.getLogger().info("Created Resource Pack metadata from build defaults at " + metadataPath);
        } catch (FileAlreadyExistsException ignored) {
            // Keep metadata that was created or edited after startup began.
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not create Resource Pack metadata at " + metadataPath + ": " + exception.getMessage());
        }
    }

    private ResourcePackMetadata loadEmbeddedMetadata(String pluginVersion) {
        try (InputStream input = plugin.getResource(PACK_PROPERTIES_RESOURCE)) {
            if (input == null) {
                plugin.getLogger().info(
                        "Embedded Resource Pack metadata not found in the plugin JAR; using the SHA-1 sidecar");
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return parseMetadata(properties, "the plugin JAR", pluginVersion);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().warning(
                    "Embedded Resource Pack metadata is invalid: " + exception.getMessage()
                            + "; falling back to the SHA-1 sidecar");
            return null;
        }
    }

    private ResourcePackMetadata parseMetadata(Properties properties, String source, String pluginVersion) {
        String version = properties.getProperty("version", pluginVersion).trim();
        if (version.isBlank()) {
            throw new IllegalArgumentException("The Resource Pack metadata does not contain a version");
        }

        String configuredUrl = properties.getProperty("url");
        String url = configuredUrl == null || configuredUrl.isBlank()
                ? buildPackUrl(version)
                : configuredUrl.trim();
        URI parsedUrl;
        try {
            parsedUrl = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("The Resource Pack URL is invalid", exception);
        }
        String scheme = parsedUrl.getScheme();
        if (parsedUrl.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("The Resource Pack URL is invalid; expected an HTTP(S) URL");
        }

        String configuredHash = properties.getProperty("sha1");
        if (configuredHash == null || configuredHash.isBlank()) {
            return new ResourcePackMetadata(url, null, source);
        }
        try {
            return new ResourcePackMetadata(url, parseSha1(configuredHash), source);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning(
                    "The local Resource Pack SHA-1 in " + source + " is invalid: " + exception.getMessage()
                            + "; checking the online sidecar");
            return new ResourcePackMetadata(url, null, source);
        }
    }

    private void verifyAndPublish(byte[] configuredHash) {
        try {
            if (!active) {
                return;
            }

            byte[] sidecarHash = null;
            String sidecarError = null;
            try {
                sidecarHash = downloadSha1(buildSidecarUrl(packUrl));
            } catch (IOException | IllegalArgumentException exception) {
                sidecarError = exception.getMessage();
                plugin.getLogger().warning(
                        "Could not check the online Resource Pack sidecar: " + sidecarError
                                + "; trying the local SHA-1");
            }

            if (sidecarHash == null && configuredHash == null) {
                plugin.getLogger().severe(
                        "Resource Pack cannot be verified: no valid local SHA-1 and no usable online sidecar");
                return;
            }

            byte[] actualHash = downloadPackHash(packUrl);
            if (!active) {
                return;
            }

            if (sidecarHash != null && MessageDigest.isEqual(actualHash, sidecarHash)) {
                if (configuredHash == null || !MessageDigest.isEqual(configuredHash, sidecarHash)) {
                    plugin.getLogger().info(
                            "Local Resource Pack SHA-1 is stale; the verified GitHub sidecar SHA-1 will be saved");
                    updateMetadataSha1(sidecarHash);
                }
                plugin.getLogger().info("Online Resource Pack sidecar and downloaded pack match");
                byte[] verifiedHash = sidecarHash;
                SchedulerCompat.run(plugin, () -> publishHash(verifiedHash));
                return;
            }

            if (configuredHash != null && MessageDigest.isEqual(actualHash, configuredHash)) {
                plugin.getLogger().warning(
                        sidecarHash == null
                                ? "Online sidecar is unavailable; local Resource Pack SHA-1 matches the downloaded pack"
                                : "GitHub sidecar SHA-1 does not match the downloaded pack; local SHA-1 does, "
                                        + "so the local value is being used");
                SchedulerCompat.run(plugin, () -> publishHash(configuredHash));
                return;
            }

            String actualValue = HexFormat.of().formatHex(actualHash);
            if (sidecarHash != null) {
                throw new IOException(
                        "SHA-1 mismatch: GitHub sidecar " + HexFormat.of().formatHex(sidecarHash)
                                + ", local " + (configuredHash == null ? "missing" : HexFormat.of().formatHex(configuredHash))
                                + ", downloaded pack " + actualValue);
            }
            throw new IOException(
                    "local SHA-1 mismatch: configured " + HexFormat.of().formatHex(configuredHash)
                            + ", downloaded pack " + actualValue + "; sidecar check failed: " + sidecarError);
        } catch (IOException | IllegalArgumentException exception) {
            if (active) {
                plugin.getLogger().severe("Resource Pack verification failed: " + exception.getMessage());
            }
        }
    }

    private void updateMetadataSha1(byte[] hash) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(metadataPath)) {
            properties.load(input);
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not read Resource Pack metadata at " + metadataPath + ": " + exception.getMessage());
            return;
        }

        String configuredUrl = properties.getProperty("url");
        if (configuredUrl != null && !packUrl.equals(configuredUrl.trim())) {
            plugin.getLogger().warning(
                    "Online SHA-1 was verified, but the metadata URL changed; leaving the file untouched");
            return;
        }
        properties.setProperty("url", packUrl);
        properties.setProperty("sha1", HexFormat.of().formatHex(hash));

        Path temporaryPath = null;
        try {
            temporaryPath = Files.createTempFile(metadataPath.getParent(), "resource-pack-", ".tmp");
            try (OutputStream output = Files.newOutputStream(
                    temporaryPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                properties.store(output, "PepeForge Resource Pack delivery settings");
            }
            Files.move(
                    temporaryPath,
                    metadataPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            temporaryPath = null;
        } catch (AtomicMoveNotSupportedException exception) {
            plugin.getLogger().warning(
                    "Atomic replacement is not supported for Resource Pack metadata at " + metadataPath
                            + "; keeping the existing file");
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not atomically update Resource Pack SHA-1 at " + metadataPath + ": "
                            + exception.getMessage());
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException exception) {
                    plugin.getLogger().warning(
                            "Could not remove temporary Resource Pack metadata file " + temporaryPath + ": "
                                    + exception.getMessage());
                }
            }
        }
    }

    private void publishHash(byte[] loadedHash) {
        if (!isGithubMode()) {
            return;
        }

        packHash = loadedHash.clone();
        packHashVerified = true;
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

    static String buildSidecarUrl(String packUrl) {
        URI parsedUrl = URI.create(packUrl);
        if (parsedUrl.getRawPath() == null || parsedUrl.getRawPath().isBlank()) {
            throw new IllegalArgumentException("The Resource Pack URL does not contain a path");
        }

        int queryIndex = packUrl.indexOf('?');
        int fragmentIndex = packUrl.indexOf('#');
        int suffixIndex = packUrl.length();
        if (queryIndex >= 0) {
            suffixIndex = Math.min(suffixIndex, queryIndex);
        }
        if (fragmentIndex >= 0) {
            suffixIndex = Math.min(suffixIndex, fragmentIndex);
        }
        return packUrl.substring(0, suffixIndex) + ".sha1" + packUrl.substring(suffixIndex);
    }

    private record ResourcePackMetadata(String url, byte[] hash, String source) {
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

    private static byte[] downloadPackHash(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        if (!(connection instanceof HttpURLConnection http)) {
            throw new IOException("The Resource Pack URL is not an HTTP URL");
        }

        http.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        http.setReadTimeout(READ_TIMEOUT_MILLIS);
        http.setInstanceFollowRedirects(true);
        http.setRequestProperty("User-Agent", "PepeForge-ResourcePack");
        try {
            int responseCode = http.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("The Resource Pack server returned HTTP " + responseCode);
            }

            MessageDigest digest = newSha1Digest();
            try (InputStream input = http.getInputStream()) {
                byte[] buffer = new byte[8_192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return digest.digest();
        } finally {
            http.disconnect();
        }
    }

    private static MessageDigest newSha1Digest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-1 is unavailable", exception);
        }
    }
}
