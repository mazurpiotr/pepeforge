package pepin.pepeforge.weapons.crescentkhopesh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pepin.pepeforge.util.aura.TimedAuraEffect;
import pepin.pepeforge.util.itemmeta.ItemMetaCompat;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

final class MoonSweepAuraEffect implements TimedAuraEffect {
    private static final double SPAWN_HEIGHT = 1.3D;
    private static final int FRAME_TICKS = 2;
    private static final int FRAME_COUNT = 5;

    private final JavaPlugin plugin;
    private final List<Animation> animations = new ArrayList<>();
    private final List<ItemStack> frames = new ArrayList<>(FRAME_COUNT);
    private boolean stopped;

    MoonSweepAuraEffect(JavaPlugin plugin) {
        this.plugin = plugin;
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            ItemStack visual = new ItemStack(Material.PAPER);
            var meta = visual.getItemMeta();
            ItemMetaCompat.setItemModel(meta, new NamespacedKey("pepeforge", "moon_sweep_" + frame));
            visual.setItemMeta(meta);
            frames.add(visual);
        }
    }

    public synchronized boolean addSweep(Player player, Location activationLocation, double width, double range) {
        if (stopped || !SchedulerCompat.isOwnedByCurrentRegion(player)
                || !player.isOnline() || player.isDead() || player.getWorld() != activationLocation.getWorld()) {
            return false;
        }
        Location origin = activationLocation.clone().add(0, SPAWN_HEIGHT, 0);
        World world = origin.getWorld();
        double yaw = Math.toRadians(origin.getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        if (!canAccess(world, origin, forwardX, forwardZ, width, range)) {
            return false;
        }
        Location center = origin.clone().add(forwardX * range / 2, 0, forwardZ * range / 2);
        center.setYaw(0);
        center.setPitch(0);
        Quaternionf rotation = new Quaternionf().rotateY((float) -yaw).rotateX((float) (Math.PI / 2));
        Transformation transform = new Transformation(new Vector3f(), rotation,
                new Vector3f((float) width, (float) range, 1), new Quaternionf());
        long startedTick = world.getGameTime();
        ItemDisplay display = world.spawn(center, ItemDisplay.class, entity -> {
            entity.setItemStack(frames.getFirst());
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.setDisplayWidth(0);
            entity.setDisplayHeight(0);
            entity.setTransformation(transform);
        });
        if (stopped || !SchedulerCompat.isOwnedByCurrentRegion(display) || !display.isValid()
                || !SchedulerCompat.isOwnedByCurrentRegion(player)
                || !player.isOnline() || player.isDead() || player.getWorld() != world) {
            removeDisplay(display);
            return false;
        }
        animations.add(new Animation(world, display, startedTick));
        return true;
    }

    @Override
    public synchronized void tick(Player player) {
        if (stopped) {
            return;
        }
        if (!SchedulerCompat.isOwnedByCurrentRegion(player) || !player.isOnline() || player.isDead()) {
            stop();
            return;
        }
        for (Iterator<Animation> iterator = animations.iterator(); iterator.hasNext();) {
            Animation animation = iterator.next();
            ItemDisplay display = animation.display;
            if (!SchedulerCompat.isOwnedByCurrentRegion(display) || player.getWorld() != animation.world
                    || !display.isValid()) {
                iterator.remove();
                removeDisplay(display);
                continue;
            }
            long age = Math.max(0, animation.world.getGameTime() - animation.startedTick);
            if (age >= FRAME_TICKS * FRAME_COUNT) {
                iterator.remove();
                removeDisplay(display);
            } else {
                int frame = (int) (age / FRAME_TICKS);
                if (frame != animation.currentFrame) {
                    animation.currentFrame = frame;
                    display.setItemStack(frames.get(frame));
                }
            }
        }
    }

    @Override
    public int getTickInterval() {
        return 1;
    }

    @Override
    public synchronized boolean isExpired() {
        return stopped || animations.isEmpty();
    }

    @Override
    public void extendDuration(int ticks) {
        // Each activation retains its own ten-tick lifetime.
    }

    @Override
    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        for (Animation animation : animations) {
            removeDisplay(animation.display);
        }
        animations.clear();
    }

    private boolean canAccess(World world, Location origin, double forwardX, double forwardZ,
            double width, double range) {
        double lateralX = forwardZ * width / 2;
        double lateralZ = -forwardX * width / 2;
        double endX = origin.getX() + forwardX * range;
        double endZ = origin.getZ() + forwardZ * range;
        int minX = ((int) Math.floor(Math.min(origin.getX(), endX) - Math.abs(lateralX))) >> 4;
        int maxX = ((int) Math.floor(Math.max(origin.getX(), endX) + Math.abs(lateralX))) >> 4;
        int minZ = ((int) Math.floor(Math.min(origin.getZ(), endZ) - Math.abs(lateralZ))) >> 4;
        int maxZ = ((int) Math.floor(Math.max(origin.getZ(), endZ) + Math.abs(lateralZ))) >> 4;
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

    private void removeDisplay(ItemDisplay display) {
        Runnable remove = () -> {
            if (display.isValid()) {
                display.remove();
            }
        };
        if (SchedulerCompat.isOwnedByCurrentRegion(display)) {
            remove.run();
        } else if (plugin.isEnabled() && !SchedulerCompat.isServerStopping()) {
            SchedulerCompat.runForEntity(display, plugin, remove);
        }
    }

    private static final class Animation {
        private final World world;
        private final ItemDisplay display;
        private final long startedTick;
        private int currentFrame;

        private Animation(World world, ItemDisplay display, long startedTick) {
            this.world = world;
            this.display = display;
            this.startedTick = startedTick;
        }
    }
}
