package model.workWithFiles;

import javafx.animation.PauseTransition;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class Util {

    public static File setPathForSave(Stage stage, File currentDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory for saving");
        File initialDirectory = resolveInitialDirectory(currentDirectory);
        if (initialDirectory != null) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        return directoryChooser.showDialog(stage);
    }

    public static File resolveInitialDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return directory;
        }

        return null;
    }

    /**
     * Sets up the timer to clear the success message after it finished.
     */
    public static void setupClearMessageTimer(Label label, PauseTransition timer) {
        timer.setOnFinished(_ -> hideSuccessMessage(label, timer));
    }

    /**
     * Displays the success message and starts the timer to hide it.
     */
    public static void showSuccessMessage(Label label, String format, PauseTransition timer) {
        label.setText("Successfully converted to " + format.toUpperCase());
        label.setManaged(true);
        label.setVisible(true);
        timer.playFromStart();
    }

    /**
     * Immediately hides the success message and stops the timer.
     */
    public static void hideSuccessMessage(Label label, PauseTransition timer) {
        if (timer != null) {
            timer.stop();
        }
        label.setVisible(false);
        label.setManaged(false);
        label.setText("");
    }

    public static Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }

    public static int parseComboBoxStringToInt(ComboBox<String> cb) {
        return Integer.parseInt(cb.getValue().replaceAll("[^0-9]", ""));
    }
}
