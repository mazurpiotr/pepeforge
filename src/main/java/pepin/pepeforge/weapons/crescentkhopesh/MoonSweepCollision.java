package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Intersects complete target hitboxes with a horizontal front half ellipse. */
final class MoonSweepCollision {
    static final double HEIGHT = 0.4D;
    private static final double EPSILON = 1.0E-9D;

    private MoonSweepCollision() {
    }

    static BoundingBox bounds(Vector origin, Vector direction, double width, double range) {
        double halfWidth = width / 2;
        double sideX = direction.getZ() * halfWidth;
        double sideZ = -direction.getX() * halfWidth;
        double endX = origin.getX() + direction.getX() * range;
        double endZ = origin.getZ() + direction.getZ() * range;
        return new BoundingBox(Math.min(origin.getX(), endX) - Math.abs(sideX),
                origin.getY() - HEIGHT / 2, Math.min(origin.getZ(), endZ) - Math.abs(sideZ),
                Math.max(origin.getX(), endX) + Math.abs(sideX), origin.getY() + HEIGHT / 2,
                Math.max(origin.getZ(), endZ) + Math.abs(sideZ));
    }

    /** Returns the nearest point in the intersection, or null if the target is outside. */
    static Vector contact(Vector origin, Vector direction, double width, double range, BoundingBox target) {
        if (origin.getY() + HEIGHT / 2 < target.getMinY()
                || origin.getY() - HEIGHT / 2 > target.getMaxY()) {
            return null;
        }
        List<Point> polygon = new ArrayList<>(4);
        polygon.add(local(origin, direction, target.getMinX(), target.getMinZ()));
        polygon.add(local(origin, direction, target.getMaxX(), target.getMinZ()));
        polygon.add(local(origin, direction, target.getMaxX(), target.getMaxZ()));
        polygon.add(local(origin, direction, target.getMinX(), target.getMaxZ()));
        polygon = clipFront(polygon);
        if (polygon.isEmpty()) {
            return null;
        }
        double y = Math.max(target.getMinY(), Math.min(origin.getY(), target.getMaxY()));
        if (origin.getX() >= target.getMinX() && origin.getX() <= target.getMaxX()
                && origin.getZ() >= target.getMinZ() && origin.getZ() <= target.getMaxZ()) {
            return new Vector(origin.getX(), y, origin.getZ());
        }
        double halfWidth = width / 2;
        Point nearest = null;
        for (int i = 0; i < polygon.size(); i++) {
            Point start = polygon.get(i);
            Point end = polygon.get((i + 1) % polygon.size());
            double dx = end.x - start.x;
            double dz = end.z - start.z;
            double lengthSquared = dx * dx + dz * dz;
            double t = lengthSquared == 0 ? 0
                    : Math.max(0, Math.min(1, -(start.x * dx + start.z * dz) / lengthSquared));
            Point closest = new Point(start.x + t * dx, start.z + t * dz);
            if (inside(closest, halfWidth, range)) {
                nearest = nearer(nearest, closest);
            }
            double a = dx * dx / (halfWidth * halfWidth) + dz * dz / (range * range);
            if (a < EPSILON) {
                continue;
            }
            double b = 2 * (start.x * dx / (halfWidth * halfWidth) + start.z * dz / (range * range));
            double c = start.x * start.x / (halfWidth * halfWidth) + start.z * start.z / (range * range) - 1;
            double discriminant = b * b - 4 * a * c;
            if (discriminant < -EPSILON) {
                continue;
            }
            double root = Math.sqrt(Math.max(0, discriminant));
            for (double intersection : new double[] {(-b - root) / (2 * a), (-b + root) / (2 * a)}) {
                if (intersection >= -EPSILON && intersection <= 1 + EPSILON) {
                    double clamped = Math.max(0, Math.min(1, intersection));
                    nearest = nearer(nearest, new Point(start.x + clamped * dx, start.z + clamped * dz));
                }
            }
        }
        return nearest == null ? null : new Vector(origin.getX() + direction.getZ() * nearest.x
                + direction.getX() * nearest.z, y, origin.getZ() - direction.getX() * nearest.x
                + direction.getZ() * nearest.z);
    }

    private static Point local(Vector origin, Vector direction, double x, double z) {
        double dx = x - origin.getX();
        double dz = z - origin.getZ();
        return new Point(dx * direction.getZ() - dz * direction.getX(),
                dx * direction.getX() + dz * direction.getZ());
    }

    private static List<Point> clipFront(List<Point> polygon) {
        List<Point> clipped = new ArrayList<>(5);
        Point previous = polygon.getLast();
        for (Point current : polygon) {
            if ((previous.z >= 0) != (current.z >= 0)) {
                double t = -previous.z / (current.z - previous.z);
                clipped.add(new Point(previous.x + (current.x - previous.x) * t, 0));
            }
            if (current.z >= 0) {
                clipped.add(current);
            }
            previous = current;
        }
        return clipped;
    }

    private static boolean inside(Point point, double halfWidth, double range) {
        return point.x * point.x / (halfWidth * halfWidth) + point.z * point.z / (range * range) <= 1 + EPSILON;
    }

    private static Point nearer(Point previous, Point candidate) {
        return previous == null || candidate.x * candidate.x + candidate.z * candidate.z
                < previous.x * previous.x + previous.z * previous.z ? candidate : previous;
    }

    private record Point(double x, double z) {
    }
}
