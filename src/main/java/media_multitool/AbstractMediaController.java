package media_multitool;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.NonNull;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.utility.DragDropped;
import model.utility.PathWorker;
import model.utility.ResetContext;
import viewHelp.Alerts;
import viewHelp.Message;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static model.utility.PathWorker.*;
import static viewHelp.Message.hideSuccessMessage;

public abstract class AbstractMediaController {
    
    @FXML protected ProgressBar progressBar;
    @FXML protected Label labelSuccess;
    @FXML protected Button btnReset;

    protected abstract void lockUI();
    protected abstract void unlockUI();
    protected abstract void disableControls();
    protected abstract void enableControls();
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

        PathWorker.IO_EXECUTOR.execute(task);
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
        if (selectedBtn != null && !selectedBtn.isSelected()) {
            propertySetter.accept(null);
            if (allButtons != null) {
                allButtons.forEach(tb -> tb.setSelected(false));
            }
        } else {
            propertySetter.accept(format);
            if (allButtons != null) {
                for (ToggleButton tb : allButtons) {
                    tb.setSelected(tb == selectedBtn);
                }
            }
        }
        Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
    }

    protected void selectFormat(String format ,Consumer<String> propertySetter) {
        propertySetter.accept(format);

        Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
    }

    protected void selectOutputDirectory(Button triggerButton, File currentPath, Consumer<File> propertySetter, String title) {
        Stage stage = getStage(triggerButton);
        directoryChooser(stage, currentPath, title)
                .ifPresent(selectedPath -> {
                    propertySetter.accept(selectedPath);
                    Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
                });
    }

    protected void setupDragAndDrop(@NonNull StackPane dropZone, @NonNull List<String> supportedFormats, @NonNull Consumer<File> fileProcessor) {
        dropZone.setOnDragOver(e -> DragDropped.handleDragOver(e, supportedFormats, dropZone));
        dropZone.setOnDragDropped(e -> {
            File droppedFile = DragDropped.handleDragDropped(e, dropZone);
            if (droppedFile != null) {
                fileProcessor.accept(droppedFile);
            }
        });
    }

    public static void showProgressBar(ProgressBar bar, PauseTransition timer) {
        if (bar != null) {
            bar.setVisible(true);
            bar.setManaged(true);
            bar.setProgress(1.0);
        }
        if (timer != null) timer.playFromStart();
    }

    public static Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }


    public static void reset(MediaProperties properties, ResetContext ctx, String defaultText) {
        properties.reset();

        if (ctx.labelSelectFileName() != null) {
            ctx.labelSelectFileName().setText(defaultText != null ? defaultText : "Selected file: none");
        }

        if (ctx.labelSuccess() != null) {
            ctx.labelSuccess().setVisible(true);
            ctx.labelSuccess().setText("");
            hideSuccessMessage(ctx.labelSuccess(), ctx.progressBar(), properties.getHideSuccessMessageTimer(), ctx.managed());
            ctx.labelSuccess().setManaged(ctx.managed());
        }

        resetDropZone(ctx.textDragZone(), ctx.dropZone());

        if (ctx.imageViewPreview() != null) {
            ctx.imageViewPreview().setImage(null);
        }
        if (ctx.labelPreviewPlaceholder() != null) {
            ctx.labelPreviewPlaceholder().setVisible(true);
        }
    }


    public static void resetDropZone(Label textDragZone, StackPane dropZone) {
        textDragZone.setText("Drag files here");

        dropZone.getStyleClass().removeAll(java.util.Collections.singleton("drop-zone-filled"));
    }

    public static void bindingImageViewToPreviewContainer(ImageView imageViewPreview, StackPane previewContainer) {
        if(imageViewPreview != null && previewContainer != null) {
            if (!imageViewPreview.fitWidthProperty().isBound()) {
                imageViewPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(10));
            }
            if (!imageViewPreview.fitHeightProperty().isBound()) {
                imageViewPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(10));
            }
        }
    }
}
