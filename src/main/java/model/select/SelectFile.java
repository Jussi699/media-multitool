package model.select;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static model.utility.PathWorker.*;


public class SelectFile extends AbstractSelectFile {
    private final FileChooser fileChooser = new FileChooser();

    @Override
    public Optional<File> choiceFile(Stage stage, FileChooser.ExtensionFilter filter, String title) {
        fileChooser.setTitle(title);
        
        resolveInitialDirectory(getSavedInputPath()).ifPresent(fileChooser::setInitialDirectory);

        fileChooser.getExtensionFilters().setAll(filter);

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            saveInputPath(selectedFile);
        }
        
        return Optional.ofNullable(selectedFile);
    }

    public Optional<List<File>> showOpenMultipleDialog(Stage stage, FileChooser.ExtensionFilter filter, String title) {
        fileChooser.setTitle(title);

        resolveInitialDirectory(getSavedInputPath()).ifPresent(fileChooser::setInitialDirectory);

        fileChooser.getExtensionFilters().setAll(filter);

        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        return Optional.ofNullable(files);
    }
}
