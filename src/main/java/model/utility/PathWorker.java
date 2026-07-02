package model.utility;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import static org.apache.commons.io.FilenameUtils.getBaseName;

public class PathWorker {
    private static final String KEY_INPUT_PATH = "last_input_path";
    private static final Preferences prefs = Preferences.userNodeForPackage(PathWorker.class);

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

    public static File createOutputFile(File image, File pathForSave, String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = getBaseName(image.getName())
                + "_" + shortId
                + "." + normalizedExtension;

        return new File(pathForSave, fileName);
    }

    public static File createOutputFile(File image, File pathForSave, String endText ,String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = getBaseName(image.getName())
                + "_" + shortId + "_" + endText
                + "." + normalizedExtension;

        return new File(pathForSave, fileName);
    }

    public static Optional<File> directoryChooser(Stage stage, File currentDirectory, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        resolveInitialDirectory(currentDirectory).ifPresent(directoryChooser::setInitialDirectory);

        return Optional.ofNullable(directoryChooser.showDialog(stage));
    }

    public static Optional<File> resolveInitialDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return Optional.of(directory);
        }
        return Optional.empty();
    }

    public static File generateUniquePdfOutputFile(String outputDirectory, String baseName) {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        File outputFile = new File(outputDirectory + File.separator + baseName + "_" + shortId + ".pdf");
        int counter = 1;
        while (outputFile.exists()) {
            outputFile = new File(outputDirectory + File.separator + baseName + "_" + shortId + "_" + counter + ".pdf");
            counter++;
        }
        return outputFile;
    }
}
