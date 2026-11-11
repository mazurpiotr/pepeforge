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

}
