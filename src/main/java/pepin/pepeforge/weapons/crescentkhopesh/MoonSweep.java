package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

final class MoonSweep {
    private static final double SPAWN_HEIGHT = 1.3D;

    private final Player owner;
    private final World world;
    private final Location origin;
    private final Vector direction;
    private final UUID excludedTarget;
    private final double damage;
    private final double width;
    private final double range;
    private final Runnable acceptedHit;
    private final BooleanSupplier active;
    private boolean struck;

    MoonSweep(Player owner, Location activationLocation, UUID excludedTarget, double damage,
            double width, double range, Runnable acceptedHit, BooleanSupplier active) {
        this.owner = owner;
        this.world = activationLocation.getWorld();
        this.origin = activationLocation.clone().add(0, SPAWN_HEIGHT, 0);
        double yaw = Math.toRadians(origin.getYaw());
        this.direction = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        origin.setYaw(0);
        origin.setPitch(0);
        this.excludedTarget = excludedTarget;
        this.damage = damage;
        this.width = width;
        this.range = range;
        this.acceptedHit = acceptedHit;
        this.active = active;
    }

    public void strike() {
        if (struck) {
            return;
        }
        struck = true;
        if (!canStrike()) {
            return;
        }
        BoundingBox bounds = MoonSweepCollision.bounds(origin.toVector(), direction, width, range);
        if (!canAccess(bounds)) {
            return;
        }
        Set<UUID> hit = new HashSet<>();
        hit.add(owner.getUniqueId());
        hit.add(excludedTarget);
        for (Entity entity : world.getNearbyEntities(bounds)) {
            if (!canStrike() || !canAccess(bounds)) {
                return;
            }
            if (!(entity instanceof LivingEntity target) || !SchedulerCompat.isOwnedByCurrentRegion(target)
                    || !target.isValid() || target.isDead() || !hit.add(target.getUniqueId())) {
                continue;
            }
            Vector contact = MoonSweepCollision.contact(origin.toVector(), direction, width, range, target.getBoundingBox());
            if (contact == null || obstructed(contact)) {
                continue;
            }
            if (DamageFlow.damage(target, damage, owner).accepted() && canStrike()) {
                acceptedHit.run();
            }
        }
    }

    private boolean canStrike() {
        return active.getAsBoolean() && SchedulerCompat.isOwnedByCurrentRegion(owner)
                && owner.isOnline() && !owner.isDead() && owner.getWorld() == world;
    }

    private boolean obstructed(Vector contact) {
        Vector ray = contact.clone().subtract(origin.toVector());
        double distance = ray.length();
        return distance > 1.0E-9D && world.rayTraceBlocks(origin, ray.multiply(1 / distance), distance,
                FluidCollisionMode.NEVER, true) != null;
    }

    private boolean canAccess(BoundingBox bounds) {
        int minX = ((int) Math.floor(bounds.getMinX())) >> 4;
        int minZ = ((int) Math.floor(bounds.getMinZ())) >> 4;
        int maxX = ((int) Math.floor(bounds.getMaxX())) >> 4;
        int maxZ = ((int) Math.floor(bounds.getMaxZ())) >> 4;
        if (!SchedulerCompat.isOwnedByCurrentRegion(world, minX, minZ, maxX, maxZ)) {
            return false;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }
}
