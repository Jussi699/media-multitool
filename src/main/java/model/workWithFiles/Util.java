package model.workWithFiles;

import javafx.animation.PauseTransition;
import javafx.scene.control.*;
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
        setupClearMessageTimer(label, null, timer);
    }

    /**
     * Sets up the timer to clear the success message and progress bar after it finished.
     */
    public static void setupClearMessageTimer(Label label, ProgressBar bar, PauseTransition timer) {
        timer.setOnFinished(_ -> hideSuccessMessage(label, bar, timer));
    }

    /**
     * Displays the success message and starts the timer to hide it.
     */
    public static void showSuccessMessage(Label label, String format, PauseTransition timer) {
        label.setStyle("-fx-text-fill: #32CD32;");
        label.setText("Successfully converted to " + format.toUpperCase());
        label.setVisible(true);
        timer.playFromStart();
    }

    public static void showProgressBar(ProgressBar bar, PauseTransition timer) {
        bar.setProgress(1.0);
        timer.playFromStart();
    }

    /**
     * Displays the error message and starts the timer to hide it.
     */
    public static void showErrorMessage(Label label, String message, PauseTransition timer) {
        label.setStyle("-fx-text-fill: RED;");
        label.setText(message);
        label.setVisible(true);
        timer.playFromStart();
    }

    public static void showErrorMessage(Label label, ProgressBar bar ,String message, PauseTransition timer) {
        bar.setStyle("-fx-border-color: RED;");
        showErrorMessage(label, message, timer);
    }

    /**
     * Immediately hides the success message and stops the timer.
     */
    public static void hideSuccessMessage(Label label, PauseTransition timer) {
        hideSuccessMessage(label, null, timer);
    }

    /**
     * Immediately hides the success message, resets progress bar and stops the timer.
     */
    public static void hideSuccessMessage(Label label, ProgressBar bar, PauseTransition timer) {
        if (timer != null) {
            timer.stop();
        }
        label.setVisible(false);
        label.setText("");
        if (bar != null) {
            bar.setProgress(0.0);
        }
    }

    public static Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }

    public static int parseComboBoxStringToInt(ComboBox<String> cb) {
        return Integer.parseInt(cb.getValue().replaceAll("[^0-9]", ""));
    }
}
