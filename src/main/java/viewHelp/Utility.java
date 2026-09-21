package viewHelp;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Utility {
    private Utility() {}

    public static Optional<MultimediaInfo> getMetadata(File file) {
        if (file == null || !file.exists()) return Optional.empty();
        try {
            return Optional.of(new MultimediaObject(file).getInfo());
        } catch (Exception e) {
            ErrorLogger.log(111, ErrorLogger.Level.ERROR, "Failed to get metadata", e);
            return Optional.empty();
        }
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static void cleanUp(Path path) throws IOException {
        Files.delete(path);
    }

    public static void cleanupFile(File file) {
        if (file != null && file.exists()) {
            try {
                cleanUp(file.toPath());
                ErrorLogger.info("File deleted: " + file.getName());
            } catch (IOException e) {
                ErrorLogger.error("Failed to delete file: " + file.getAbsolutePath() + " - " + e.getMessage());
                Platform.runLater(() ->
                        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed to delete file", "Could not delete temporary file. Check logs for details.")
                );
            }
        }
    }
}
