package model.utility;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import viewHelp.Alerts;

import java.io.File;
import java.util.Locale;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class TemplateCheck {
    public static boolean isValidFile(File file) {
        if (isNull(file) || !file.exists() || !file.isFile()) {
            Alerts.alertDialog(
                    Alert.AlertType.ERROR,
                    "Error",
                    "File not found",
                    "The file is missing or does not exist.\nPlease verify the selected file."
            );
            ErrorLogger.warn("File selection failed: file is null, missing, or a directory");
            return false;
        }
        return true;
    }

    public static boolean canRead(File file) {
        if (!file.canRead()) {
            Alerts.alertDialog(
                    Alert.AlertType.ERROR,
                    "Error",
                    "The application cannot use the file",
                    "Check the selected file for read and write permissions."
            );
            ErrorLogger.error("Insufficient file permissions for '" + file.getAbsolutePath() + "': "
                    + "canRead=" + file.canRead());
            return false;
        }
        return true;
    }

    public static boolean canReadAndWrite(File file) {
        if (!file.canRead() || !file.canWrite()) {
            Alerts.alertDialog(
                    Alert.AlertType.ERROR,
                    "Error",
                    "The application cannot use the file",
                    "Check the selected file for read and write permissions."
            );
            ErrorLogger.error("Insufficient file permissions for '" + file.getAbsolutePath() + "': "
                    + "canRead=" + file.canRead() + ", canWrite=" + file.canWrite());
            return false;
        }
        return true;
    }

    public static boolean isAudioFile(File file) {
        return nonNull(file) && Global.getAllSupportedAudioFormats().stream()
                .anyMatch(format -> file.getName().toLowerCase(Locale.ROOT).endsWith(format));
    }
}
