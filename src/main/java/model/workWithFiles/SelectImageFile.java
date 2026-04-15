package model.workWithFiles;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

import static model.workWithFiles.Util.resolveInitialDirectory;

public class SelectImageFile implements SelectFile {
    @Override
    public File choiceFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select image");
        File initialDirectory = resolveInitialDirectory(new File(System.getProperty("user.home")));
        if (initialDirectory != null) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.ico", "*.webp")
        );

        return fileChooser.showOpenDialog(stage);
    }

}
