package pepin.pepeforge.util.combat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pepin.pepeforge.util.scheduler.SchedulerCompat;

/** Tracks the real event produced by a secondary weapon hit, without dispatching a probe. */
public final class DamageFlow implements Listener {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void capture(EntityDamageByEntityEvent event) {
        Attempt attempt = CURRENT.get();
        if (attempt != null && attempt.event == null
                && event.getEntity() == attempt.target && event.getDamager() == attempt.source) {
            attempt.event = event;
        }
    }

    public static boolean isSecondaryDamage(EntityDamageByEntityEvent event) {
        Attempt attempt = CURRENT.get();
        return attempt != null && attempt.event == event;
    }

    public static boolean isCounterattack(EntityDamageByEntityEvent event) {
        Attempt attempt = CURRENT.get();
        return attempt != null && attempt.event == event && attempt.counterattack;
    }

    public static Result counterattack(LivingEntity target, double amount, Entity source) {
        return damage(target, amount, source, false, true);
    }

    public static Result damage(LivingEntity target, double amount, Entity source) {
        return damage(target, amount, source, false);
    }

    public static Result damage(LivingEntity target, double amount, Entity source, boolean resetInvulnerability) {
        return damage(target, amount, source, resetInvulnerability, false);
    }

    private static Result damage(LivingEntity target, double amount, Entity source,
            boolean resetInvulnerability, boolean counterattack) {
        if (!SchedulerCompat.isOwnedByCurrentRegion(target) || !SchedulerCompat.isOwnedByCurrentRegion(source)) {
            return Result.REJECTED;
        }
        if (!target.isValid() || target.isDead() || !Double.isFinite(amount) || amount <= 0.0D) {
            return Result.REJECTED;
        }
        Attempt previous = CURRENT.get();
        Attempt attempt = new Attempt(target, source, counterattack);
        double beforeHealth = target.getHealth();
        int previousNoDamageTicks = target.getNoDamageTicks();
        CURRENT.set(attempt);
        boolean accepted = false;
        try {
            if (resetInvulnerability) {
                target.setNoDamageTicks(0);
            }
            target.damage(amount, source);
            accepted = attempt.event != null && !attempt.event.isCancelled();
            return accepted
                    ? new Result(true, SchedulerCompat.isOwnedByCurrentRegion(target)
                            ? Math.max(0.0D, beforeHealth - target.getHealth()) : 0.0D)
                    : Result.REJECTED;
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
            if (resetInvulnerability && !accepted && SchedulerCompat.isOwnedByCurrentRegion(target)) {
                target.setNoDamageTicks(previousNoDamageTicks);
            }
        }
    }

    public record Result(boolean accepted, double healthLost) {
        private static final Result REJECTED = new Result(false, 0.0D);
    }

    private static final class Attempt {
        private final LivingEntity target;
        private final Entity source;
        private final boolean counterattack;
        private EntityDamageByEntityEvent event;

        private Attempt(LivingEntity target, Entity source, boolean counterattack) {
            this.target = target;
            this.source = source;
            this.counterattack = counterattack;
        }
    }
}
