package model.select;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public abstract class AbstractSelectFile  {
    public abstract Optional<File> choiceFile(Stage stage, FileChooser.ExtensionFilter filter);
}
