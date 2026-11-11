package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoonSweepCollisionTest {
    private final Vector origin = new Vector(0, 1.3, 0);
    private final Vector forward = new Vector(0, 0, 1);

    @Test
    void frontHalfEllipseIncludesSidesAndExactBoundaryButExcludesRearAndOuterCorners() {
        assertNotNull(contact(new BoundingBox(-0.3, 0, 1, 0.3, 2, 1.6)));
        assertEquals(new Vector(1.5, 1.3, 0), contact(new BoundingBox(1.5, 0, -0.3, 2, 2, 0)));
        assertEquals(new Vector(0, 1.3, 2), contact(new BoundingBox(0, 0, 2, 0, 2, 2.6)));
        assertNull(contact(new BoundingBox(-0.3, 0, -1, 0.3, 2, -0.1)));
        assertNull(contact(new BoundingBox(1.2, 0, 1.6, 1.8, 2, 2.2)));
        assertNull(contact(new BoundingBox(1.51, 0, 0, 2, 2, 0.2)));
    }

    @Test
    void completeHitboxIntersectsEvenWhenItsCenterIsOutsideEllipseOrBehindPlayer() {
        assertNotNull(contact(new BoundingBox(1.3, 0, 0.3, 1.9, 2, 0.9)));
        assertEquals(new Vector(0, 1.3, 0), contact(new BoundingBox(-0.3, 0, -0.7, 0.3, 2, 0.1)));
    }

    @Test
    void heightAndNearestContactRespectTheThinVerticalBand() {
        assertNull(contact(new BoundingBox(-0.3, 0, 1, 0.3, 1, 1.5)));
        assertNull(contact(new BoundingBox(-0.3, 1.6, 1, 0.3, 2, 1.5)));
        assertEquals(new Vector(0.4, 1.2, 0.8), contact(new BoundingBox(0.4, 0, 0.8, 1, 1.2, 1.4)));
        BoundingBox bounds = MoonSweepCollision.bounds(origin, forward, 3, 2);
        assertEquals(1.1, bounds.getMinY(), 1.0E-9);
        assertEquals(1.5, bounds.getMaxY(), 1.0E-9);
    }

    @Test
    void rotationUsesFullRectangleInsteadOfItsCenterOrBroadphaseCorners() {
        Vector diagonal = new Vector(1, 0, 1).normalize();
        BoundingBox front = new BoundingBox(0.8, 0, 0.8, 1.4, 2, 1.4);
        assertNotNull(MoonSweepCollision.contact(origin, diagonal, 3, 2, front));
        BoundingBox emptyCorner = new BoundingBox(2.3, 0, -0.9, 2.4, 2, -0.8);
        assertTrue(MoonSweepCollision.bounds(origin, diagonal, 3, 2).overlaps(emptyCorner));
        assertNull(MoonSweepCollision.contact(origin, diagonal, 3, 2, emptyCorner));
        Vector right = new Vector(1, 0, 0);
        assertNotNull(MoonSweepCollision.contact(origin, right, 3, 2,
                new BoundingBox(1, 0, -0.3, 1.6, 2, 0.3)));
        assertNull(MoonSweepCollision.contact(origin, right, 3, 2,
                new BoundingBox(-1, 0, -0.3, -0.1, 2, 0.3)));
    }

    @Test
    void nearestPointUsesPhysicalDistanceRatherThanEllipseNormalizedDistance() {
        Vector diagonal = new Vector(1, 0, 1).normalize();
        BoundingBox target = new BoundingBox(0.2, 0, 0.8, 0.8, 2, 1.4);
        Vector point = MoonSweepCollision.contact(origin, diagonal, 3, 2, target);
        assertNotNull(point);
        assertEquals(0.2, point.getX(), 1.0E-9);
        assertEquals(0.8, point.getZ(), 1.0E-9);
    }

    @Test
    void ellipseBoundaryConstrainsContactWhenNearestRectanglePointIsOutside() {
        Vector diagonal = new Vector(1, 0, 1).normalize();
        BoundingBox target = new BoundingBox(1.75, 0, 0.1, 2, 2, 0.8);
        Vector point = MoonSweepCollision.contact(origin, diagonal, 3, 2, target);
        assertNotNull(point);
        assertEquals(1.75, point.getX(), 1.0E-9);
        assertTrue(point.getZ() > 0.1);
        double lateral = (point.getX() - point.getZ()) / Math.sqrt(2);
        double longitudinal = (point.getX() + point.getZ()) / Math.sqrt(2);
        assertEquals(1, lateral * lateral / 2.25 + longitudinal * longitudinal / 4, 1.0E-9);
    }

    private Vector contact(BoundingBox target) {
        return MoonSweepCollision.contact(origin, forward, 3, 2, target);
    }
}
