package model.utility;

import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.List;

import static model.utility.Checking.isSupportedMediaFile;

public class DragDropped {
    
    public static void handleDragOver(DragEvent e, List<String> supportedExtensions, StackPane dropZone) {
        Dragboard db = e.getDragboard();
        if (e.getGestureSource() != dropZone && db.hasFiles()) {
            File file = db.getFiles().getFirst();
            if (isSupportedMediaFile(file, supportedExtensions)) {
                e.acceptTransferModes(TransferMode.COPY);
                e.consume();
            }
        }
        e.consume();
    }

    public static File handleDragDropped(DragEvent e, StackPane dropZone, Label textDragZone) {
        Dragboard db = e.getDragboard();
        boolean success = false;
        File file = null;

        if (db.hasFiles()) {
            file = db.getFiles().getFirst();
            success = true;
            
            if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }
        }

        e.setDropCompleted(success);
        e.consume();
        return file;
    }
}
