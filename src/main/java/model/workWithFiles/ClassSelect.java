package model.workWithFiles;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ClassSelect implements SelectFile {
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

    public static File setPathForSave(Stage stage, File currentDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory for saving");
        File initialDirectory = resolveInitialDirectory(currentDirectory);
        if (initialDirectory != null) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        return directoryChooser.showDialog(stage);
    }

    private static File resolveInitialDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return directory;
        }

        return null;
    }
}
