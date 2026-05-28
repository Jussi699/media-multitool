package media_multitool;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import model.logger.ErrorLogger;
import viewHelp.Alerts;
import static model.utility.Util.IO_EXECUTOR;

public abstract class AbstractMediaController {
    
    @FXML protected ProgressBar progressBar;
    @FXML protected Label labelSuccess;

    protected abstract void lockUI();
    protected abstract void unlockUI();

    protected <T> void executeMediaTask(Task<T> task) {
        lockUI();
        
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.progressProperty().bind(task.progressProperty());
        }

        task.setOnSucceeded(_ -> {
            unbindProgress();
            unlockUI();
            handleTaskSuccess(task.getValue());
        });

        task.setOnFailed(_ -> {
            unbindProgress();
            unlockUI();
            Throwable exception = task.getException();
            ErrorLogger.error("Task failed: " + exception.getMessage());
            handleTaskFailure(exception);
        });

        IO_EXECUTOR.execute(task);
    }

    private void unbindProgress() {
        if (progressBar != null) {
            progressBar.progressProperty().unbind();
        }
    }

    protected void handleTaskSuccess(Object result) {
        Platform.runLater(() -> {
            if (labelSuccess != null) {
                labelSuccess.setText("Operation successful!");
                labelSuccess.setVisible(true);
                labelSuccess.setManaged(true);
            }
        });
    }

    protected void handleTaskFailure(Throwable exception) {
        Platform.runLater(() -> {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", exception.getMessage());
            if (labelSuccess != null) {
                labelSuccess.setText("Operation failed.");
                labelSuccess.setVisible(true);
                labelSuccess.setManaged(true);
            }
        });
    }
}
