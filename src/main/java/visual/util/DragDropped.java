package visual.util;

import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;

import static model.utility.checks.Checking.isSupportedMediaFile;

public class DragDropped {
    private DragDropped() {
        /* This utility class should not be instantiated */
    }

    public static void handleDragOver(DragEvent e, List<String> supportedExtensions, StackPane dropZone) {
        Dragboard db = e.getDragboard();
        if (e.getGestureSource() != dropZone && db.hasFiles()) {
            if (db.getFiles().stream().anyMatch(file -> isSupportedMediaFile(file, supportedExtensions))) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        }
        e.consume();
    }

    public static List<File> handleDragDropped(DragEvent e, StackPane dropZone, List<String> supportedExtensions) {
        Dragboard db = e.getDragboard();
        List<File> files = List.of();

        if (db.hasFiles()) {
            files = db.getFiles().stream()
                    .filter(file -> isSupportedMediaFile(file, supportedExtensions))
                    .toList();
        }

        final boolean success = !files.isEmpty();
        final boolean isContainsStyle = dropZone.getStyleClass().contains("drop-zone-filled");

        if (success && !isContainsStyle) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        e.setDropCompleted(success);
        e.consume();
        return files;
    }
}
