package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlaiveGeometryTest {
    @Test
    void rangeUsesNearestHitboxPointInsteadOfEntityCenter() {
        BoundingBox target = new BoundingBox(-0.3, 0, 4, 0.3, 2, 4.6);
        double distance = GlaiveGeometry.distanceSquared(new Vector(0, 1.62, 0), target);
        assertEquals(16.0D, distance);
        assertTrue(GlaiveGeometry.inRange(distance, false, false));
        assertFalse(GlaiveGeometry.inRange(Math.nextUp(distance), false, false));
    }

    @Test
    void closePenaltyHasAnExclusiveOneAndHalfBlockBoundary() {
        assertEquals(0.30D, GlaiveGeometry.damageMultiplier(Math.nextDown(2.25D)));
        assertEquals(1.0D, GlaiveGeometry.damageMultiplier(2.25D));
        assertEquals(1.0D, GlaiveGeometry.damageMultiplier(Math.nextUp(2.25D)));
    }

    @Test
    void stabHasInclusiveMaximumRangesAndCanHitAtPointBlankDistance() {
        assertTrue(GlaiveGeometry.inRange(0.0D, true, false));
        assertTrue(GlaiveGeometry.inRange(0.25D, true, false));
        assertTrue(GlaiveGeometry.inRange(Math.nextDown(4.0D), true, false));
        assertTrue(GlaiveGeometry.inRange(4.0D, true, false));
        assertTrue(GlaiveGeometry.inRange(4.5D * 4.5D, true, false));
        assertFalse(GlaiveGeometry.inRange(Math.nextUp(4.5D * 4.5D), true, false));
        assertTrue(GlaiveGeometry.inRange(6.5D * 6.5D, true, true));
        assertFalse(GlaiveGeometry.inRange(Math.nextUp(6.5D * 6.5D), true, true));
        assertFalse(GlaiveGeometry.inRange(Double.NaN, false, false));
        assertFalse(GlaiveGeometry.inRange(Double.POSITIVE_INFINITY, false, false));
        assertFalse(GlaiveGeometry.inRange(4.1D * 4.1D, false, true));
    }

    @Test
    void piercingLineIntersectsOnlyForwardHitboxesWithinRange() {
        Vector origin = new Vector(0, 1.62, 0);
        Vector direction = new Vector(0, 0, 1);
        assertEquals(0.0D, GlaiveGeometry.rayDistance(origin, direction,
                new BoundingBox(-0.3, 0, -0.2, 0.3, 2, 0.4), 4.5D));
        assertEquals(4.5D, GlaiveGeometry.rayDistance(origin, direction,
                new BoundingBox(-0.3, 0, 4.5, 0.3, 2, 5.1), 4.5D));
        assertEquals(Double.POSITIVE_INFINITY, GlaiveGeometry.rayDistance(origin, direction,
                new BoundingBox(-0.3, 0, 4.6, 0.3, 2, 5.2), 4.5D));
        assertEquals(Double.POSITIVE_INFINITY, GlaiveGeometry.rayDistance(origin, direction,
                new BoundingBox(0.5, 0, 2, 1.1, 2, 2.6), 4.5D));
        assertEquals(Double.POSITIVE_INFINITY, GlaiveGeometry.rayDistance(origin, direction,
                new BoundingBox(-0.3, 0, -1, 0.3, 2, -0.4), 4.5D));
    }
}
