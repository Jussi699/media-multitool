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
    
    @FXML protected ProgressBar progressBarConvert;
    @FXML protected Label labelSuccessConvert;

    protected abstract void lockUI();
    protected abstract void unlockUI();

    protected <T> void executeMediaTask(Task<T> task) {
        lockUI();
        
        if (progressBarConvert != null) {
            progressBarConvert.setVisible(true);
            progressBarConvert.setManaged(true);
            progressBarConvert.progressProperty().bind(task.progressProperty());
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
        if (progressBarConvert != null) {
            progressBarConvert.progressProperty().unbind();
        }
    }

    protected void handleTaskSuccess(Object result) {
        Platform.runLater(() -> {
            if (labelSuccessConvert != null) {
                labelSuccessConvert.setText("Operation successful!");
                labelSuccessConvert.setVisible(true);
                labelSuccessConvert.setManaged(true);
            }
        });
    }

    protected void handleTaskFailure(Throwable exception) {
        Platform.runLater(() -> {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", exception.getMessage());
            if (labelSuccessConvert != null) {
                labelSuccessConvert.setText("Operation failed.");
                labelSuccessConvert.setVisible(true);
                labelSuccessConvert.setManaged(true);
            }
        });
    }
}
