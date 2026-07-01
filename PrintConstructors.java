import java.lang.reflect.Constructor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PrintConstructors {
    public static void main(String[] args) throws Exception {
        System.out.println("Constructors:");
        for (Constructor<?> c : EntityDamageByEntityEvent.class.getConstructors()) {
            System.out.println(c);
            for(java.lang.annotation.Annotation a : c.getAnnotations()) {
                System.out.println("  " + a);
            }
        }
    }
}
