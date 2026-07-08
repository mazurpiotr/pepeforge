package pepin.pepeforge.util.env;

public final class AdventureReflect {

    private static final boolean ADVENTURE_SUPPORTED;

    static {
        boolean temp;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            temp = true;
        } catch (ClassNotFoundException e) {
            temp = false;
        }
        ADVENTURE_SUPPORTED = temp;
    }

    private AdventureReflect() {
    }

    public static boolean isSupported() {
        return ADVENTURE_SUPPORTED;
    }

    public static Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!ADVENTURE_SUPPORTED) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getDeclaredMethod(methodName, parameterTypes).invoke(null, args);
        } catch (Throwable t) {
            return null;
        }
    }
}
