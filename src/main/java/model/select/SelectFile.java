package model.select;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

import static model.utility.Util.resolveInitialDirectory;

public class SelectFile extends AbstractSelectFile {
    @Override
    public File choiceFile(Stage stage, FileChooser.ExtensionFilter filter, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        File initialDirectory = resolveInitialDirectory(new File(System.getProperty("user.home")));
        if (initialDirectory != null) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                filter
        );

        return fileChooser.showOpenDialog(stage);
    }
}
