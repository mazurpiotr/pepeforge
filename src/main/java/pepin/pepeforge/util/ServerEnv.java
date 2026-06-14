package pepin.pepeforge.util;

public final class ServerEnv {

    private static final boolean DATA_COMPONENT_API_SUPPORTED = ServerEnv.class.getClassLoader()
            .getResource("io/papermc/paper/datacomponent/DataComponentTypes.class") != null;

    private ServerEnv() {
    }

    public static boolean hasDataComponentApi() {
        return DATA_COMPONENT_API_SUPPORTED;
    }
}
