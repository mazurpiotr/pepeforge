package pepin.pepeforge.weapons.crescentkhopesh;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Sweeps a horizontal oriented box against an entity or block collision box. */
final class MoonWaveCollision {
    static final double HEIGHT = 0.4D;
    private static final double HALF_DEPTH = 0.1D;

    private MoonWaveCollision() {
    }

    static BoundingBox bounds(Vector center, Vector direction, double distance, double width) {
        double extentX = Math.abs(direction.getZ()) * width / 2 + Math.abs(direction.getX()) * HALF_DEPTH;
        double extentZ = Math.abs(direction.getX()) * width / 2 + Math.abs(direction.getZ()) * HALF_DEPTH;
        double endX = center.getX() + direction.getX() * distance;
        double endZ = center.getZ() + direction.getZ() * distance;
        return new BoundingBox(Math.min(center.getX(), endX) - extentX, center.getY() - HEIGHT / 2,
                Math.min(center.getZ(), endZ) - extentZ, Math.max(center.getX(), endX) + extentX,
                center.getY() + HEIGHT / 2, Math.max(center.getZ(), endZ) + extentZ);
    }

    /** Returns the earliest travel distance at contact, or infinity when there is no contact. */
    static double contact(Vector center, Vector direction, double distance, double width, BoundingBox target) {
        if (center.getY() + HEIGHT / 2 < target.getMinY()
                || center.getY() - HEIGHT / 2 > target.getMaxY()) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = target.getCenterX() - center.getX();
        double dz = target.getCenterZ() - center.getZ();
        double halfX = target.getWidthX() / 2;
        double halfZ = target.getWidthZ() / 2;
        double sideX = direction.getZ();
        double sideZ = -direction.getX();
        double entry = 0;
        double exit = distance;
        // The four separating axes of an oriented rectangle and an axis-aligned rectangle.
        double[][] axes = {{1, 0}, {0, 1}, {direction.getX(), direction.getZ()}, {sideX, sideZ}};
        for (double[] axis : axes) {
            double projection = dx * axis[0] + dz * axis[1];
            double velocity = direction.getX() * axis[0] + direction.getZ() * axis[1];
            double radius = Math.abs(axis[0]) * halfX + Math.abs(axis[1]) * halfZ
                    + Math.abs(sideX * axis[0] + sideZ * axis[1]) * width / 2
                    + Math.abs(velocity) * HALF_DEPTH;
            if (Math.abs(velocity) < 1.0E-10D) {
                if (Math.abs(projection) > radius) {
                    return Double.POSITIVE_INFINITY;
                }
            } else {
                double first = (projection - radius) / velocity;
                double last = (projection + radius) / velocity;
                entry = Math.max(entry, Math.min(first, last));
                exit = Math.min(exit, Math.max(first, last));
                if (entry > exit) {
                    return Double.POSITIVE_INFINITY;
                }
            }
        }
        return entry;
    }
}
