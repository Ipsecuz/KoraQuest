package dev.ipseucz.koraquest.util;

public final class PlatformDetector {
<<<<<<< HEAD
    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final String PLATFORM_NAME = FOLIA ? "Folia" : "Paper";

=======
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
    private PlatformDetector() {
    }

    public static boolean isFolia() {
<<<<<<< HEAD
        return FOLIA;
    }

    public static boolean isPaper() {
        return !FOLIA;
    }

    public static String name() {
        return PLATFORM_NAME;
=======
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
>>>>>>> dd95e1cdbf70c284d2b8d6ce7b0dc22d4287233b
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
