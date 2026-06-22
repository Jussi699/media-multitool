package model.checks;

import javafx.scene.control.Alert;

import model.properties.ImageProperties;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class Checking {
    public static boolean checkImageAndOutputOnNull(ImageProperties imageProperties) {
        if (imageProperties.getImage() == null || imageProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return true;
        }
        return false;
    }

    public static boolean checkImageAndOutputOnNull(ImageProperties props, BufferedImage image) {
        return image == null || checkImageAndOutputOnNull(props);
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
}
