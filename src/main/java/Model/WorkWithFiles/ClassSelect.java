package Model.WorkWithFiles;

import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;

public class ClassSelect implements SelectFile{
    @Override
    public File choiceFile(MouseEvent event, Stage stage) {
        FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.ico"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        return fileChooser.showOpenDialog(stage);
    }

    public static File setPathForSave(MouseEvent event, Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            return selectedDirectory;
        }
        return Paths.get(System.getProperty("user.home"), "Desktop").toFile();
    }
}
