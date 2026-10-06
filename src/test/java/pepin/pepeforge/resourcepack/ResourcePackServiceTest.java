package pepin.pepeforge.resourcepack;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePackServiceTest {

    @Test
    void buildsTheVersionedGitHubReleaseUrl() {
        assertEquals(
                "https://github.com/mazurpiotr/pepeforge/releases/download/v1.4.0/PepeForge-ResourcePack.zip",
                ResourcePackService.buildPackUrl("1.4.0"));
    }

    @Test
    void parsesSha1SidecarWithOptionalFilename() {
        byte[] expected = HexFormat.of().parseHex("b66705d9a4cac6f38bdbacc511bed059da120705");

        assertArrayEquals(expected, ResourcePackService.parseSha1(
                "b66705d9a4cac6f38bdbacc511bed059da120705  PepeForge-ResourcePack.zip\n"));
    }

    @Test
    void rejectsMalformedSha1Sidecar() {
        assertThrows(IllegalArgumentException.class, () -> ResourcePackService.parseSha1("not-a-sha1"));
        assertThrows(IllegalArgumentException.class, () -> ResourcePackService.parseSha1(
                "b66705d9a4cac6f38bdbacc511bed059da12070"));
    }
}
