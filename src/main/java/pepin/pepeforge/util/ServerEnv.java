package pepin.pepeforge.util;

public final class ServerEnv {

    private ServerEnv() {
    }

    public static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
