package model.utility.io;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import visual.dialogs.Alerts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class FileUtils {
    private FileUtils() {
        /* This utility class should not be instantiated */
    }

    public static void cleanUp(Path path) throws IOException {
        Files.delete(path);
    }

    public static void cleanupFile(File file) {
        if (nonNull(file) && file.exists()) {
            try {
                cleanUp(file.toPath());
                ErrorLogger.info("File deleted: " + file.getName());
            } catch (IOException e) {
                ErrorLogger.error("Failed to delete file: " + file.getAbsolutePath() + " - " + e.getMessage());
                Platform.runLater(() ->
                        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to delete file",
                                "Could not delete temporary file. Check logs for details.")
                );
            }
        }
    }

    public static boolean cleanupFile(File file, boolean showAlertOnError) {
        if (isNull(file) || !file.exists()) {
            return true;
        }
        try {
            cleanUp(file.toPath());
            ErrorLogger.info("File deleted: " + file.getName());
            return true;
        } catch (IOException e) {
            ErrorLogger.warn("Failed to delete file: " + file.getAbsolutePath() + " - " + e.getMessage());
            if (showAlertOnError) {
                Platform.runLater(() ->
                        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to delete file",
                                "Could not delete temporary file. Check logs for details.")
                );
            }
            return false;
        }
    }

    public static boolean checkingFile(File file) {
        return nonNull(file) && file.exists();
    }

    public static String getFileExtension(File file) {
        if (isNull(file)) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    public static List<File> getFilesFromFolder(File path, List<String> filter) {

        File[] matches = path.listFiles((_, name) -> {
            String lowerName = name.toLowerCase();
            for(String ext : filter) {
                String suffix = ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
                if(lowerName.endsWith(suffix)) return true;
            }
            return false;
        });

        if(nonNull(matches)){
            return new ArrayList<>(Arrays.asList(matches));
        }

        return new ArrayList<>();
    }
}
