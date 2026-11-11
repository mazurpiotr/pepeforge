package pepin.pepeforge.weapons.obsidianglaive;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

final class GlaiveGeometry {
    private GlaiveGeometry() {
    }

    static Vector closestPoint(Vector origin, BoundingBox bounds) {
        return new Vector(Math.max(bounds.getMinX(), Math.min(bounds.getMaxX(), origin.getX())),
                Math.max(bounds.getMinY(), Math.min(bounds.getMaxY(), origin.getY())),
                Math.max(bounds.getMinZ(), Math.min(bounds.getMaxZ(), origin.getZ())));
    }

    static double distanceSquared(Vector origin, BoundingBox bounds) {
        return origin.distanceSquared(closestPoint(origin, bounds));
    }

    static double rayDistance(Vector origin, Vector direction, BoundingBox bounds, double range) {
        if (distanceSquared(origin, bounds) == 0.0D) {
            return 0.0D;
        }
        RayTraceResult hit = bounds.rayTrace(origin, direction, range);
        return hit == null ? Double.POSITIVE_INFINITY : origin.distance(hit.getHitPosition());
    }

    static boolean inRange(double distanceSquared, boolean stab, boolean creative) {
        double minimum = stab ? ObsidianGlaiveDefinition.STAB_MIN_RANGE : 0.0D;
        double maximum = stab ? (creative ? ObsidianGlaiveDefinition.STAB_CREATIVE_RANGE
                : ObsidianGlaiveDefinition.STAB_RANGE) : ObsidianGlaiveDefinition.WHACK_RANGE;
        return Double.isFinite(distanceSquared) && distanceSquared >= minimum * minimum
                && distanceSquared <= maximum * maximum;
    }

    static double damageMultiplier(double distanceSquared) {
        return distanceSquared < ObsidianGlaiveDefinition.CLOSE_RANGE * ObsidianGlaiveDefinition.CLOSE_RANGE
                ? ObsidianGlaiveDefinition.CLOSE_DAMAGE_MULTIPLIER : 1.0D;
    }
}
