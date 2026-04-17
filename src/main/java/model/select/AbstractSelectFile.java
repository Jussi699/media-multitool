package model.select;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public abstract class AbstractSelectFile  {
    public abstract File choiceFile(Stage stage, FileChooser.ExtensionFilter filter, String title);
}
