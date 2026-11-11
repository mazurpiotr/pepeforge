package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoonWaveCollisionTest {
    private final Vector center = new Vector(0, 1, 0);
    private final Vector direction = new Vector(0, 0, 1);

    @Test
    void configuredWidthChangesSweptBoundsAndContact() {
        BoundingBox target = new BoundingBox(1.8, 0, 2, 2.4, 2, 2.6);
        assertFalse(MoonWaveCollision.bounds(center, direction, 4.5, 2.8).overlaps(target));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, direction, 4.5, 2.8, target));
        assertTrue(MoonWaveCollision.bounds(center, direction, 4.5, 4).overlaps(target));
        assertEquals(1.9, MoonWaveCollision.contact(center, direction, 4.5, 4, target), 1.0E-9);
    }

    @Test
    void fastMovementFindsTargetBetweenTickEndpoints() {
        BoundingBox target = new BoundingBox(-0.3, 0, 2, 0.3, 2, 2.6);
        assertEquals(1.9, MoonWaveCollision.contact(center, direction, 4.5, 2.8, target), 1.0E-9);
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, direction, 0.65, 2.8, target));
    }

    @Test
    void targetHitboxCanReachWaveEvenWhenItsCenterIsOutside() {
        assertEquals(0.1, MoonWaveCollision.contact(center, direction, 1, 2.8,
                new BoundingBox(1.3, 0, 0.2, 1.9, 2, 0.8)), 1.0E-9);
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, direction, 1, 2.8,
                new BoundingBox(1.5, 0, 0.2, 2.1, 2, 0.8)));
    }

    @Test
    void verticalHitboxesAndStartingOverlapAreAccountedFor() {
        assertEquals(0, MoonWaveCollision.contact(center, direction, 1, 2.8,
                new BoundingBox(-0.3, 1.1, -0.3, 0.3, 2.2, 0.3)));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, direction, 1, 2.8,
                new BoundingBox(-0.3, 1.3, -0.3, 0.3, 2.4, 0.3)));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, direction, 1, 2.8,
                new BoundingBox(-0.3, 0, -1, 0.3, 2, -0.5)));
    }

    @Test
    void diagonalWaveDoesNotHitEmptyCornersOfBroadphaseBounds() {
        Vector diagonal = new Vector(1, 0, 1).normalize();
        BoundingBox corner = new BoundingBox(2.4, 0, -0.8, 2.6, 2, -0.6);
        assertTrue(MoonWaveCollision.bounds(center, diagonal, 3, 2.8).overlaps(corner));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(center, diagonal, 3, 2.8, corner));
        assertTrue(Double.isFinite(MoonWaveCollision.contact(center, diagonal, 3, 2.8,
                new BoundingBox(1.5, 0, 1.5, 2.1, 2, 2.1))));
    }

    @Test
    void thinWaveClearsBothSlabsAndSingleFullBlockButContactsWallsAndFences() {
        Vector raisedCenter = new Vector(0, 1.3, 0);
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.5, 0, 1, 0.5, 0.5, 2)));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.5, 0.5, 1, 0.5, 1, 2)));
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.5, 0, 1, 0.5, 1, 2)));
        assertEquals(0.9, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.5, 0, 1, 0.5, 2, 2)), 1.0E-9);
        assertEquals(0.9, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(1.2, 0, 1, 1.8, 1.5, 2)), 1.0E-9);
    }

    @Test
    void thinWaveAccountsForTargetHeightAndSweptVerticalBounds() {
        Vector raisedCenter = new Vector(0, 1.3, 0);
        BoundingBox bounds = MoonWaveCollision.bounds(raisedCenter, direction, 4, 2.8);
        assertEquals(1.1, bounds.getMinY(), 1.0E-9);
        assertEquals(1.5, bounds.getMaxY(), 1.0E-9);
        assertEquals(Double.POSITIVE_INFINITY, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.3, 0, 2, 0.3, 1, 2.6)));
        assertEquals(1.9, MoonWaveCollision.contact(raisedCenter, direction, 4, 2.8,
                new BoundingBox(-0.3, 0, 2, 0.3, 1.2, 2.6)), 1.0E-9);
    }

    @Test
    void edgeObstacleIsDetectedBeforeTargetsBeyondIt() {
        double wall = MoonWaveCollision.contact(center, direction, 4, 2.8,
                new BoundingBox(1.2, 0, 1, 2, 2, 2));
        double target = MoonWaveCollision.contact(center, direction, 4, 2.8,
                new BoundingBox(-0.3, 0, 2.5, 0.3, 2, 3.1));
        assertEquals(0.9, wall, 1.0E-9);
        assertTrue(wall < target);
    }
}
