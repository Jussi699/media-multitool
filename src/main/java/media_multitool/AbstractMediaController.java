package media_multitool;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.utility.DragDropped;
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static model.utility.Util.directoryChooser;
import static model.utility.Util.getStage;

public abstract class AbstractMediaController {
    
    @FXML protected ProgressBar progressBar;
    @FXML protected Label labelSuccess;
    @FXML protected Button btnReset;

    protected abstract void lockUI();
    protected abstract void unlockUI();
    protected abstract MediaProperties getProperties();

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

        task.setOnCancelled(_ -> {
            unbindProgress();
            unlockUI();
            handleTaskCancelled();
        });

        task.setOnFailed(_ -> {
            unbindProgress();
            unlockUI();
            Throwable exception = task.getException();
            ErrorLogger.error("Task failed: " + exception.getMessage());
            handleTaskFailure(exception);
        });

        model.utility.Util.IO_EXECUTOR.execute(task);
    }

    private void unbindProgress() {
        if (progressBar != null) {
            progressBar.progressProperty().unbind();
        }
    }

    protected void handleTaskSuccess(Object result) {
        if (Boolean.FALSE.equals(result)) {
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", "The operation completed but did not produce the expected result. Please check the logs.");
            startSuccessTimer();
            return;
        }
        if (labelSuccess != null) {
            labelSuccess.setStyle("-fx-text-fill: #32CD32;");
            labelSuccess.setText("Operation successful!");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
    }

    protected void handleTaskCancelled() {
        if (labelSuccess != null) {
            labelSuccess.setStyle("-fx-text-fill: orange;");
            labelSuccess.setText("Operation cancelled.");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        startSuccessTimer();
    }

    protected void handleTaskFailure(Throwable exception) {
        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", exception.getMessage());
        if (labelSuccess != null) {
            labelSuccess.setStyle("-fx-text-fill: RED;");
            labelSuccess.setText("Operation failed.");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
        startSuccessTimer();
    }

    protected void startSuccessTimer() {
        MediaProperties props = getProperties();
        if (props != null && props.getHideSuccessMessageTimer() != null) {
            props.getHideSuccessMessageTimer().playFromStart();
        }
    }

    protected void selectFormat(String format, ToggleButton selectedBtn, List<ToggleButton> allButtons, Consumer<String> propertySetter) {
        propertySetter.accept(format);
        if (allButtons != null) {
            for (ToggleButton tb : allButtons) {
                tb.setSelected(tb == selectedBtn);
            }
        }
        viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
    }

    protected void selectOutputDirectory(Button triggerButton, File currentPath, Consumer<File> propertySetter, String title) {
        Stage stage = getStage(triggerButton);
        directoryChooser(stage, currentPath, title)
                .ifPresent(selectedPath -> {
                    propertySetter.accept(selectedPath);
                    viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
                });
    }

    protected void setupDragAndDrop(StackPane dropZone, Label textDragZone, List<String> supportedFormats, Consumer<File> fileProcessor) {
        if (dropZone == null) return;

        dropZone.setOnDragOver(e -> DragDropped.handleDragOver(e, supportedFormats, dropZone));
        dropZone.setOnDragDropped(e -> {
            File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
            if (droppedFile != null) {
                fileProcessor.accept(droppedFile);
            }
        });
    }
}
