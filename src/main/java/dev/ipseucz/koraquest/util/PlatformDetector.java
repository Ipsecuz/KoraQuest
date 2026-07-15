package dev.ipseucz.koraquest.util;

public final class PlatformDetector {
    private PlatformDetector() {
    }

    public static boolean isFolia() {
        return classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    public static boolean isPaper() {
        return isFolia() || classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration");
    }

    public static String name() {
        if (isFolia()) {
            return "Folia";
        }
        if (isPaper()) {
            return "Paper";
        }
        return "Spigot";
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
