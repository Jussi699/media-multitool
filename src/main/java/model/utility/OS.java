package model.utility;

import java.io.File;

import static java.util.Objects.nonNull;

public class OS {
    public enum TypeOS {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    public static TypeOS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return TypeOS.WINDOWS;
        if (os.contains("mac")) return TypeOS.MACOS;
        if (os.contains("nix") || os.contains("nux")) return TypeOS.LINUX;
        return TypeOS.UNKNOWN;
    }

    public static String getAppConfigDir() {
        String home = System.getProperty("user.home");
        return switch (getOS()) {
            case WINDOWS -> {
                String appData = System.getenv("APPDATA");
                yield (nonNull(appData) ? appData : home) + File.separator + "media-multitool";
            }
            case MACOS -> home + "/Library/Application Support/media-multitool";
            case LINUX -> {
                String xdgData = System.getenv("XDG_DATA_HOME");
                yield (nonNull(xdgData) ? xdgData : home + "/.local/share") + "/media-multitool";
            }
            default -> home + "/.media-multitool";
        };
    }
}
