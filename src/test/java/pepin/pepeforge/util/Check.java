package pepin.pepeforge.util;
import java.lang.reflect.Constructor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class Check {
    public static void main(String[] args) throws Exception {
        for (Constructor<?> c : EntityDamageByEntityEvent.class.getConstructors()) {
            boolean isDeprecated = false;
            for(java.lang.annotation.Annotation a : c.getAnnotations()) {
                if (a.annotationType().getName().equals("java.lang.Deprecated")) isDeprecated = true;
            }
            if (!isDeprecated) {
                System.out.println("Constructor:");
                for(Class<?> pt : c.getParameterTypes()) {
                    System.out.println("  " + pt.getName());
                }
            }
        }
    }
}
