package model.utility;

import javafx.animation.PauseTransition;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Control;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import model.logger.ErrorLogger;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import static org.apache.commons.io.FilenameUtils.getBaseName;

public class Util {
    private static final String KEY_INPUT_PATH = "last_input_path";
    private static final Preferences prefs = Preferences.userNodeForPackage(Util.class);

    public enum OS {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return OS.WINDOWS;
        if (os.contains("mac")) return OS.MACOS;
        if (os.contains("nix") || os.contains("nux")) return OS.LINUX;
        return OS.UNKNOWN;
    }

    public static String getAppConfigDir() {
        String home = System.getProperty("user.home");
        return switch (getOS()) {
            case WINDOWS -> {
                String appData = System.getenv("APPDATA");
                yield (appData != null ? appData : home) + File.separator + "media-multitool";
            }
            case MACOS -> home + "/Library/Application Support/media-multitool";
            case LINUX -> {
                String xdgData = System.getenv("XDG_DATA_HOME");
                yield (xdgData != null ? xdgData : home + "/.local/share") + "/media-multitool";
            }
            default -> home + "/.media-multitool";
        };
    }

    public static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("IO-Executor");
                return t;
            }
    );

    public static File getSavedPath() {
        String home = System.getProperty("user.home");
        File desktop = new File(home, "Desktop");
        String defaultPath = desktop.exists() ? desktop.getAbsolutePath() : home;
        return new File(defaultPath);
    }

    public static File getSavedInputPath() {
        String defaultPath = System.getProperty("user.home");
        String savedPath = prefs.get(KEY_INPUT_PATH, defaultPath);
        File file = new File(savedPath);
        return (file.exists() && file.isDirectory()) ? file : new File(defaultPath);
    }

    public static void saveInputPath(File file) {
        if (file != null) {
            File dir = file.isDirectory() ? file : file.getParentFile();
            if (dir != null && dir.exists()) {
                prefs.put(KEY_INPUT_PATH, dir.getAbsolutePath());
            }
        }
    }

    public static File directoryChooser(Stage stage, File currentDirectory, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        File initialDirectory = resolveInitialDirectory(currentDirectory);

        if (initialDirectory != null) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        return directoryChooser.showDialog(stage);
    }

    public static File resolveInitialDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return directory;
        }
        return null;
    }

    public static void showProgressBar(ProgressBar bar, PauseTransition timer) {
        bar.setProgress(1.0);
        timer.playFromStart();
    }

    public static Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }

    public static MultimediaInfo getMetadata(File file) {
        if (file == null || !file.exists()) return null;
        try {
            return new MultimediaObject(file).getInfo();
        } catch (Exception e) {
            ErrorLogger.log(111, ErrorLogger.Level.ERROR, "Failed to get metadata", e);
            return null;
        }
    }

    public static boolean isSupportedMediaFile(File file, List<String> list) {
        String fileName = file.getName().toLowerCase();

        for (String ext : list) {
            String suffix = ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public static File createOutputFile(File image, File pathForSave, String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        String shortId = UUID.randomUUID().toString().split("-")[0];
        String fileName = getBaseName(image.getName())
                + "_"
                + shortId
                + "."
                + normalizedExtension;

        return new File(pathForSave, fileName);
    }
}
