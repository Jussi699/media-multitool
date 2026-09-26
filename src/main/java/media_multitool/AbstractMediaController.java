package media_multitool;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static model.utility.PathWorker.*;
import static viewHelp.Message.hideSuccessMessage;

public abstract class AbstractMediaController {
    
    @FXML protected ProgressBar progressBar;
    @FXML protected Label labelSuccess;
    @FXML protected Button btnReset;
    @FXML protected Button btnSubmitAndCopy;

    /** Currently running a media task — used for cancellation. */
    private final AtomicReference<Task<?>> currentTask = new AtomicReference<>();

    protected abstract void lockUI();
    protected abstract void unlockUI();
    protected abstract void disableControls();
    protected abstract void enableControls();
    protected abstract MediaProperties getProperties();

    protected void setupTooltips() {
        Tooltip clipboardTooltip = new Tooltip("Certain copied images may not show a preview in the Windows clipboard menu (Win + V).\nHowever, the image is still in the clipboard and can be pasted as usual.");
        Tooltip resetTooltip = new Tooltip("Resets everything to default values.");

        if (btnSubmitAndCopy != null) {
            btnSubmitAndCopy.setTooltip(clipboardTooltip);
        }
        if (btnReset != null) {
            btnReset.setTooltip(resetTooltip);
        }
    }

    protected void setupImageClipboardButton(Supplier<BufferedImage> imageSupplier, String imageDescription) {
        if (btnSubmitAndCopy == null) {
            return;
        }

        btnSubmitAndCopy.setOnAction(_ -> {
            try {
                BufferedImage image = imageSupplier.get();
                if (image == null) {
                    String message = "No image is available to copy.";
                    ErrorLogger.error(message);
                    Message.showErrorMessage(labelSuccess, message, getProperties().getHideSuccessMessageTimer());
                    return;
                }

                ClipboardContent content = new ClipboardContent();
                content.putImage(SwingFXUtils.toFXImage(image, null));
                if (!Clipboard.getSystemClipboard().setContent(content)) {
                    String message = "The image could not be copied to the clipboard. Please try again.";
                    ErrorLogger.error(message);
                    Message.showErrorMessage(labelSuccess, message, getProperties().getHideSuccessMessageTimer());
                    return;
                }

                ErrorLogger.info(imageDescription + " image copied to clipboard.");
                Message.showSuccessText(
                        labelSuccess,
                        imageDescription + " image copied to clipboard!",
                        getProperties().getHideSuccessMessageTimer()
                );
            } catch (RuntimeException exception) {
                ErrorLogger.error("Failed to copy image to clipboard: " + exception.getMessage());
                Message.showErrorMessage(
                        labelSuccess,
                        "Failed to copy image to clipboard: " + exception.getMessage(),
                        getProperties().getHideSuccessMessageTimer()
                );
            }
        });
    }

    /**
     * Cancel the currently running media task (if any).
     * Subclasses may call this from a "Cancel" button action.
     */
    protected void cancelCurrentTask() {
        Task<?> t = currentTask.get();
        if (t != null && t.isRunning()) {
            t.cancel(true);
        }
    }

    protected <T> void executeMediaTask(Task<T> task) {
        currentTask.set(task);
        lockUI();
        
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.progressProperty().bind(task.progressProperty());
        }

        task.setOnSucceeded(_ -> {
            if (!currentTask.compareAndSet(task, null)) {
                return;
            }
            unbindProgress();
            unlockUI();
            handleTaskSuccess(task.getValue());
        });

        task.setOnCancelled(_ -> {
            if (!currentTask.compareAndSet(task, null)) {
                return;
            }
            unbindProgress();
            unlockUI();
            handleTaskCancelled();
        });

        task.setOnFailed(_ -> {
            if (!currentTask.compareAndSet(task, null)) {
                return;
            }
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
        startSuccessTimer();
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

    protected void handleTaskFailure(@NonNull Throwable exception) {
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

    protected void selectFormat(String format , @NonNull Consumer<String> propertySetter) {
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

    public static Stage getStage(@NonNull Control control) {
        return (Stage) control.getScene().getWindow();
    }


    public static void reset(@NonNull MediaProperties properties, @NonNull ResetContext ctx, String defaultText) {
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

        resetDropZone(ctx.textDragZone(), ctx.dropZone(), ctx.textForDragZone());

        if (ctx.imageViewPreview() != null) {
            ctx.imageViewPreview().setImage(null);
        }
        if (ctx.labelPreviewPlaceholder() != null) {
            ctx.labelPreviewPlaceholder().setVisible(true);
        }
    }


    public static void resetDropZone(@NonNull Label textDragZone, @NonNull StackPane dropZone, String text) {
        textDragZone.setText("Drag " +  text + " here");

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

    protected void setImagePreview(BufferedImage image, ImageView imageViewPreview) {
        if (image != null && imageViewPreview != null) {
            imageViewPreview.setImage(SwingFXUtils.toFXImage(image, null));
        }
    }
}
