package viewHelp;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class Message {
    public static void setupClearMessageTimer(Label label, PauseTransition timer, boolean manage) {
        setupClearMessageTimer(label, null, timer, manage);
    }

    public static void setupClearMessageTimer(Label label, ProgressBar bar, PauseTransition timer, boolean manage) {
        timer.setOnFinished(_ -> hideSuccessMessage(label, bar, timer, manage));
    }

    public static void showSuccessMessage(Label label, String format, PauseTransition timer) {
        showSuccessText(label, "Successfully converted to " + format.toUpperCase(), timer);
    }

    public static void showSuccessText(Label label, String message, PauseTransition timer) {
        label.setStyle("-fx-text-fill: #32CD32;");
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        timer.playFromStart();
    }

    public static void showErrorMessage(Label label, ProgressBar bar, String message, PauseTransition timer) {
        if (bar != null) {
            bar.setStyle("-fx-border-color: RED; -fx-border-radius: 10px");
        }
        showErrorMessage(label, message, timer);
    }

    public static void hideSuccessMessage(Label label, PauseTransition timer, boolean managed) {
        hideSuccessMessage(label, null, timer, managed);
    }

    public static void hideSuccessMessage(Label label, ProgressBar bar, PauseTransition timer, boolean managed) {
        if (timer != null) timer.stop();
        label.setVisible(false);
        label.setManaged(managed);
        label.setText("");
        if (bar != null) {
            bar.setProgress(0.0);
            bar.setStyle("");
            bar.setVisible(false);
            bar.setManaged(false);
        }
    }

    public static void showErrorMessage(Label label, String message, PauseTransition timer) {
        label.setStyle("-fx-text-fill: RED; -fx-border-radius: 10px");
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        timer.playFromStart();
    }
}
