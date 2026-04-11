package Model.WorkWithFiles;

import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.File;

public interface SelectFile {
    File choiceFile(MouseEvent event, Stage stage);
}
