package pepin.pepeforge.util;

public final class ServerEnv {

    private static final boolean DATA_COMPONENT_API_SUPPORTED;

    static {
        boolean supported;
        try {
            Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            supported = true;
        } catch (ClassNotFoundException e) {
            supported = false;
        }
        DATA_COMPONENT_API_SUPPORTED = supported;
    }

    private ServerEnv() {
    }

    public static boolean hasDataComponentApi() {
        return DATA_COMPONENT_API_SUPPORTED;
    }
}
