package model.utility.io;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPathManager {
    private AppPathManager() {
        /* This utility class should not be instantiated */
    }

    public static boolean isPortable() {
        File marker = new File("portable.mode");
        return marker.exists();
    }

    public static Path getBaseDir() {
        if (isPortable()) {
            return Paths.get(System.getProperty("user.dir"), "runtime");
        } else {
            String appData = System.getenv("APPDATA");
            return Paths.get(appData, "media-multitool");
        }
    }
}