package model.utility;

import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Utility class providing helper methods for UI operations, file management,
 * and data parsing within the application.
 */
public class Util {

    /**
     * Opens a directory chooser dialog to let the user select a saving path.
     *
     * @param stage            the parent stage for the dialog
     * @param currentDirectory the directory to start browsing from
     * @return the selected {@link File} directory, or {@code null} if the user canceled the dialog
     */
    public static File setPathForSave(Stage stage, File currentDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory for saving");
        File initialDirectory = resolveInitialDirectory(currentDirectory);
        if (initialDirectory != null) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        return directoryChooser.showDialog(stage);
    }

    /**
     * Validates and returns the initial directory.
     * <p>
     * This method checks if the provided {@code File} object is not {@code null},
     * exists in the file system, and represents a directory.
     * </p>
     *
     * @param directory the {@link File} object to be validated
     * @return the same {@code directory} object if it is valid;
     *         {@code null} if the directory is null, does not exist, or is not a directory
     */
    public static File resolveInitialDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return directory;
        }

        return null;
    }

    /**
     * Configures a timer to automatically clear a label message upon completion.
     *
     * @param label the label to be cleared
     * @param timer the transition used as a delay timer
     */
    public static void setupClearMessageTimer(Label label, PauseTransition timer) {
        setupClearMessageTimer(label, null, timer);
    }

    /**
     * Configures a timer to automatically clear a label and reset a progress bar upon completion.
     *
     * @param label the label to be cleared
     * @param bar   the progress bar to be reset
     * @param timer the transition used as a delay timer
     */
    public static void setupClearMessageTimer(Label label, ProgressBar bar, PauseTransition timer) {
        timer.setOnFinished(_ -> hideSuccessMessage(label, bar, timer));
    }

    /**
     * Displays a success message in green and triggers the auto-hide timer.
     *
     * @param label  the label where the message will be displayed
     * @param format the format name (e.g., "pdf", "png") to include in the message
     * @param timer  the timer to start for hiding the message
     */
    public static void showSuccessMessage(Label label, String format, PauseTransition timer) {
        label.setStyle("-fx-text-fill: #32CD32;");
        label.setText("Successfully converted to " + format.toUpperCase());
        label.setVisible(true);
        timer.playFromStart();
    }

    /**
     * Sets the progress bar to 100% and starts the auto-hide timer.
     *
     * @param bar   the progress bar to update
     * @param timer the timer to start for resetting the bar
     */
    public static void showProgressBar(ProgressBar bar, PauseTransition timer) {
        bar.setProgress(1.0);
        timer.playFromStart();
    }

    /**
     * Displays an error message in red and triggers the auto-hide timer.
     *
     * @param label   the label where the error will be displayed
     * @param message the error message text
     * @param timer   the timer to start for hiding the message
     */
    public static void showErrorMessage(Label label, String message, PauseTransition timer) {
        label.setStyle("-fx-text-fill: RED;");
        label.setText(message);
        label.setVisible(true);
        timer.playFromStart();
    }

    /**
     * Displays an error message and highlights the progress bar border in red.
     *
     * @param label   the label where the error will be displayed
     * @param bar     the progress bar to highlight
     * @param message the error message text
     * @param timer   the timer to start for hiding the UI elements
     */
    public static void showErrorMessage(Label label, ProgressBar bar, String message, PauseTransition timer) {
        bar.setStyle("-fx-border-color: RED;");
        showErrorMessage(label, message, timer);
    }

    /**
     * Immediately hides the message label and stops the timer.
     *
     * @param label the label to hide
     * @param timer the timer to stop
     */
    public static void hideSuccessMessage(Label label, PauseTransition timer) {
        hideSuccessMessage(label, null, timer);
    }

    /**
     * Resets the UI state by hiding the label, resetting the progress bar, and stopping the timer.
     *
     * @param label the label to hide
     * @param bar   the progress bar to reset (can be {@code null})
     * @param timer the timer to stop
     */
    public static void hideSuccessMessage(Label label, ProgressBar bar, PauseTransition timer) {
        if (timer != null) {
            timer.stop();
        }
        label.setVisible(false);
        label.setText("");
        if (bar != null) {
            bar.setProgress(0.0);
            bar.setStyle(""); // Resets custom styles like red borders
        }
    }

    /**
     * Retrieves the current {@link Stage} from a given JavaFX control.
     *
     * @param control any active UI control
     * @return the {@link Stage} containing the control
     */
    public static Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }

    /**
     * Extracts and parses the first numeric value found in the selected ComboBox item.
     *
     * @param cb the ComboBox containing string values
     * @return the parsed integer value
     * @throws NumberFormatException if no digits are found in the string
     */
    public static int parseComboBoxStringToInt(ComboBox<String> cb) {
        return Integer.parseInt(cb.getValue().replaceAll("[^0-9]", ""));
    }
}
