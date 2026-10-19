package model.utility;

import javafx.scene.control.TextField;
import model.logger.ErrorLogger;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.Optional;

import static java.util.Objects.isNull;

public class Utility {
    private Utility() {
        /* This utility class should not be instantiated */
    }

    public static Optional<MultimediaInfo> getMetadata(File file) {
        if (isNull(file) || !file.exists()) return Optional.empty();
        try {
            return Optional.of(new MultimediaObject(file).getInfo());
        } catch (Exception e) {
            ErrorLogger.log(111, ErrorLogger.Level.ERROR, "Failed to get metadata", e);
            return Optional.empty();
        }
    }

    public static void setTextToTextField(TextField field, String color) {
        field.setText(color);
    }

    public static void setVoidToTextField(TextField field) {
        field.setText("");
    }
}
