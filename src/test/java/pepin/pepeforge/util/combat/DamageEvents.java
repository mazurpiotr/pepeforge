package pepin.pepeforge.util.combat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Exercises production listeners in Bukkit priority order, including ignoreCancelled. */
public final class DamageEvents {
    private final List<Handler> handlers = new ArrayList<>();

    public DamageEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            for (Method method : listener.getClass().getMethods()) {
                EventHandler annotation = method.getAnnotation(EventHandler.class);
                if (annotation != null && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == EntityDamageByEntityEvent.class) {
                    handlers.add(new Handler(listener, method, annotation.priority(), annotation.ignoreCancelled()));
                }
            }
        }
        handlers.sort(Comparator.comparing(Handler::priority));
    }

    public void dispatch(EntityDamageByEntityEvent event) {
        for (Handler handler : handlers) {
            if (event.isCancelled() && handler.ignoreCancelled()) {
                continue;
            }
            try {
                handler.method().invoke(handler.listener(), event);
            } catch (IllegalAccessException | InvocationTargetException failure) {
                throw new AssertionError(failure);
            }
        }
    }

    private record Handler(Listener listener, Method method, EventPriority priority, boolean ignoreCancelled) {
    }
}
