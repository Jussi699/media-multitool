package model.utility;

import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;

import static model.checks.Checking.isSupportedMediaFile;

public class DragDropped {
    public static void handleDragOver(DragEvent e, List<String> supportedExtensions, StackPane dropZone) {
        Dragboard db = e.getDragboard();
        if (e.getGestureSource() != dropZone && db.hasFiles()) {
            if (db.getFiles().stream().anyMatch(file -> isSupportedMediaFile(file, supportedExtensions))) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        }
        e.consume();
    }

    public static File handleDragDropped(DragEvent e, StackPane dropZone) {
        List<File> files = handleDragDropped(e, dropZone, List.of());
        return files.isEmpty() ? null : files.getFirst();
    }

    public static List<File> handleDragDropped(DragEvent e, StackPane dropZone, List<String> supportedExtensions) {
        Dragboard db = e.getDragboard();
        List<File> files = List.of();

        if (db.hasFiles()) {
            files = db.getFiles().stream()
                    .filter(file -> isSupportedMediaFile(file, supportedExtensions))
                    .toList();
        }

        boolean success = !files.isEmpty();
        if (success) {
            if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }
        }

        e.setDropCompleted(success);
        e.consume();
        return files;
    }
}
