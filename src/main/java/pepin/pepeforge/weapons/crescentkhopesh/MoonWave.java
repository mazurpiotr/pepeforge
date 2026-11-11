package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pepin.pepeforge.util.combat.DamageFlow;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;
import pepin.pepeforge.util.scheduler.ScheduledTaskCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

final class MoonWave implements Runnable {
    private static final double SPAWN_HEIGHT = 1.3D;
    private static final float MODEL_HEIGHT = 1.4F;

    private final JavaPlugin plugin;
    private final Player owner;
    private final UUID ownerId;
    private final World world;
    private final Location origin;
    private final Vector direction;
    private final double damage;
    private final double range;
    private final double width;
    private final double speed;
    private final Runnable acceptedHit;
    private final Consumer<MoonWave> finished;
    private final Set<UUID> hit = new HashSet<>();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Quaternionf rotation;
    private volatile ItemDisplay display;
    private volatile ScheduledTaskCompat task;
    private double traveled;

    MoonWave(JavaPlugin plugin, Player owner, UUID excludedTarget, double damage,
            double range, double width, double speed, Runnable acceptedHit, Consumer<MoonWave> finished) {
        this.plugin = plugin;
        this.owner = owner;
        this.ownerId = owner.getUniqueId();
        this.world = owner.getWorld();
        this.origin = owner.getLocation().add(0, SPAWN_HEIGHT, 0);
        double yaw = Math.toRadians(origin.getYaw());
        direction = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        origin.setYaw(0);
        origin.setPitch(0);
        this.damage = damage;
        this.range = range;
        this.width = width;
        this.speed = speed;
        this.acceptedHit = acceptedHit;
        this.finished = finished;
        hit.add(ownerId);
        hit.add(excludedTarget);
        float roll = (float) Math.toRadians(ThreadLocalRandom.current().nextDouble(-30, 30));
        rotation = new Quaternionf().rotateY((float) (Math.PI - yaw)).rotateZ(roll).rotateX((float) (Math.PI / 2));
    }

    UUID ownerId() {
        return ownerId;
    }

    void start() {
        if (stopped.get()) {
            return;
        }
        ItemStack visual = new ItemStack(Material.PAPER);
        var meta = visual.getItemMeta();
        ItemMetaCompat.setItemModel(meta, CrescentKhopeshDefinition.WAVE_MODEL_KEY);
        visual.setItemMeta(meta);
        display = world.spawn(origin, ItemDisplay.class, entity -> {
            entity.setItemStack(visual);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.setDisplayWidth(0);
            entity.setDisplayHeight(0);
            entity.setTransformation(transform(0));
            entity.setInterpolationDuration(1);
            entity.setInterpolationDelay(0);
        });
        if (stopped.get()) {
            removeDisplay(display);
            return;
        }
        if (!display.isValid()) {
            stop();
            return;
        }
        world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
        ScheduledTaskCompat scheduled = SchedulerCompat.runTimerForEntity(display, plugin, this,
                this::stop, 1L, 1L);
        task = scheduled;
        if (stopped.get()) {
            scheduled.cancel();
        }
    }

    @Override
    public void run() {
        if (stopped.get()) {
            return;
        }
        if (!SchedulerCompat.isOwnedByCurrentRegion(owner) || !owner.isOnline() || owner.isDead()
                || owner.getWorld() != world || !display.isValid() || traveled >= range) {
            stop();
            return;
        }
        double step = Math.min(speed, range - traveled);
        Vector center = origin.toVector().add(direction.clone().multiply(traveled));
        BoundingBox bounds = MoonWaveCollision.bounds(center, direction, step, width);
        if (!canAccess(bounds)) {
            stop();
            return;
        }
        double wall = firstWall(center, step, bounds);
        for (Entity entity : world.getNearbyEntities(bounds)) {
            if (stopped.get() || !SchedulerCompat.isOwnedByCurrentRegion(owner)) {
                stop();
                return;
            }
            if (!(entity instanceof LivingEntity target) || !SchedulerCompat.isOwnedByCurrentRegion(target)
                    || hit.contains(target.getUniqueId()) || !target.isValid() || target.isDead()) {
                continue;
            }
            double contact = MoonWaveCollision.contact(center, direction, step, width, target.getBoundingBox());
            if (contact <= step && contact < wall) {
                hit.add(target.getUniqueId());
                if (DamageFlow.damage(target, damage, owner).accepted() && !stopped.get()
                        && SchedulerCompat.isOwnedByCurrentRegion(owner) && owner.isOnline()
                        && !owner.isDead() && owner.getWorld() == world) {
                    acceptedHit.run();
                }
            }
        }
        if (stopped.get()) {
            return;
        }
        if (wall <= step) {
            stop();
            return;
        }
        if (!SchedulerCompat.isOwnedByCurrentRegion(owner) || !owner.isOnline() || owner.isDead()
                || owner.getWorld() != world) {
            stop();
            return;
        }
        traveled += step;
        // Keep the entity anchored to its scheduler's region; only the client model moves.
        display.setInterpolationDelay(0);
        display.setTransformation(transform(traveled));
    }

    private Transformation transform(double distance) {
        return new Transformation(new Vector3f((float) (direction.getX() * distance), 0,
                (float) (direction.getZ() * distance)), new Quaternionf(rotation),
                new Vector3f((float) width, MODEL_HEIGHT, 1),
                new Quaternionf());
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

    private double firstWall(Vector center, double step, BoundingBox bounds) {
        double nearest = Double.POSITIVE_INFINITY;
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(bounds.getMinY()) - 1);
        int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.floor(bounds.getMaxY()));
        for (int x = (int) Math.floor(bounds.getMinX()); x <= (int) Math.floor(bounds.getMaxX()); x++) {
            for (int z = (int) Math.floor(bounds.getMinZ()); z <= (int) Math.floor(bounds.getMaxZ()); z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid()) {
                        continue;
                    }
                    for (BoundingBox shape : block.getCollisionShape().getBoundingBoxes()) {
                        nearest = Math.min(nearest, MoonWaveCollision.contact(center, direction, step, width,
                                shape.clone().shift(x, y, z)));
                    }
                }
            }
        }
        return nearest;
    }

    void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        ScheduledTaskCompat scheduled = task;
        if (scheduled != null) {
            scheduled.cancel();
        }
        finished.accept(this);
        ItemDisplay visual = display;
        if (visual != null) {
            removeDisplay(visual);
        }
    }

    private void removeDisplay(ItemDisplay visual) {
        Runnable remove = () -> {
            if (visual.isValid()) {
                visual.remove();
            }
        };
        if (SchedulerCompat.isOwnedByCurrentRegion(visual)) {
            remove.run();
        } else if (!SchedulerCompat.isServerStopping() && plugin.isEnabled()) {
            SchedulerCompat.runForEntity(visual, plugin, remove);
        }
    }
}
