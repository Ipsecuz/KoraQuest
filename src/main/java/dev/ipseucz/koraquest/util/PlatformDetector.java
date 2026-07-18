package dev.ipseucz.koraquest.util;

public final class PlatformDetector {
    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final String PLATFORM_NAME = FOLIA ? "Folia" : "Paper";

    private PlatformDetector() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static boolean isPaper() {
        return !FOLIA;
    }

    public static String name() {
        return PLATFORM_NAME;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, PlatformDetector.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
