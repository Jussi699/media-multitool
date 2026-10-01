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
import model.utility.TemplateCheck;
import viewHelp.Alerts;
import viewHelp.Message;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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

        if (nonNull(btnSubmitAndCopy)) {
            btnSubmitAndCopy.setTooltip(clipboardTooltip);
        }
        if (nonNull(btnReset)) {
            btnReset.setTooltip(resetTooltip);
        }
    }

    protected void setupImageClipboardButton(Supplier<BufferedImage> imageSupplier, String imageDescription) {
        if (isNull(btnSubmitAndCopy)) {
            return;
        }

        btnSubmitAndCopy.setOnAction(_ -> {
            try {
                BufferedImage image = imageSupplier.get();
                if (isNull(image)) {
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
        Task<?> task = currentTask.get();
        if (nonNull(task) && task.isRunning()) {
            task.cancel(true);
        }
    }

    protected <T> void executeMediaTask(Task<T> task) {
        currentTask.set(task);
        lockUI();
        ErrorLogger.info(getClass(), "Operation started.");
        
        if (nonNull(progressBar)) {
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
            ErrorLogger.info(getClass(), "Operation cancelled.");
            handleTaskCancelled();
        });

        task.setOnFailed(_ -> {
            if (!currentTask.compareAndSet(task, null)) {
                return;
            }
            unbindProgress();
            unlockUI();
            Throwable exception = task.getException();
            ErrorLogger.log(101, ErrorLogger.Level.ERROR,
                    getClass().getSimpleName() + " - Task failed", exception);
            handleTaskFailure(exception);
        });

        PathWorker.IO_EXECUTOR.execute(task);
    }

    private void unbindProgress() {
        if (nonNull(progressBar)) {
            progressBar.progressProperty().unbind();
        }
    }

    protected void handleTaskSuccess(Object result) {
        if (Boolean.FALSE.equals(result)) {
            ErrorLogger.warn(getClass(), "Operation failed: the task returned an unsuccessful result.");
            if (nonNull(progressBar)) {
                progressBar.setProgress(0);
            }
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", "The operation completed but did not produce the expected result. Please check the logs.");
            startSuccessTimer();
            return;
        }
        ErrorLogger.info(getClass(), "Operation completed successfully.");
        if (nonNull(labelSuccess)) {
            labelSuccess.setStyle("-fx-text-fill: #32CD32;");
            labelSuccess.setText("Operation successful!");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
        startSuccessTimer();
    }

    protected void handleTaskCancelled() {
        if (nonNull(labelSuccess)) {
            labelSuccess.setStyle("-fx-text-fill: orange;");
            labelSuccess.setText("Operation cancelled.");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
        if (nonNull(progressBar)) {
            progressBar.setProgress(0);
        }
        startSuccessTimer();
    }

    protected void handleTaskFailure(@NonNull Throwable exception) {
        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Operation failed", exception.getMessage());
        if (nonNull(labelSuccess)) {
            labelSuccess.setStyle("-fx-text-fill: RED;");
            labelSuccess.setText("Operation failed.");
            labelSuccess.setVisible(true);
            labelSuccess.setManaged(true);
        }
        if (nonNull(progressBar)) {
            progressBar.setProgress(0);
        }
        startSuccessTimer();
    }

    protected void startSuccessTimer() {
        MediaProperties props = getProperties();
        if (nonNull(props) && nonNull(props.getHideSuccessMessageTimer())) {
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
                    ErrorLogger.info(getClass(), "Output directory selected: " + selectedPath.getAbsolutePath());
                    Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
                });
    }

    protected boolean validateSelectedFile(File file) {
        return validateSelectedFile(file, false);
    }

    protected boolean validateSelectedFile(File file, boolean requireWriteAccess) {
        if (!TemplateCheck.isValidFile(file)) {
            ErrorLogger.warn(getClass(), "Rejected invalid input file.");
            return false;
        }
        if (requireWriteAccess ? !TemplateCheck.canReadAndWrite(file) : !TemplateCheck.canRead(file)) {
            ErrorLogger.warn(getClass(), "Rejected input file because the required permissions are unavailable: "
                    + file.getAbsolutePath());
            return false;
        }
        ErrorLogger.info(getClass(), "User selected file: " + file.getAbsolutePath());
        return true;
    }

    protected void setupDragAndDrop(@NonNull StackPane dropZone, @NonNull List<String> supportedFormats, @NonNull Consumer<File> fileProcessor) {
        dropZone.setOnDragOver(e -> DragDropped.handleDragOver(e, supportedFormats, dropZone));
        dropZone.setOnDragDropped(e -> DragDropped.handleDragDropped(e, dropZone, supportedFormats)
                .stream()
                .findFirst()
                .ifPresent(fileProcessor));
    }

    public static void showProgressBar(ProgressBar bar, PauseTransition timer) {
        if (nonNull(bar)) {
            bar.setVisible(true);
            bar.setManaged(true);
            bar.setProgress(1.0);
        }
        if (nonNull(timer)) timer.playFromStart();
    }

    public static Stage getStage(@NonNull Control control) {
        return (Stage) control.getScene().getWindow();
    }


    public static void reset(@NonNull MediaProperties properties, @NonNull ResetContext ctx, String defaultText) {
        properties.reset();

        if (nonNull(ctx.labelSelectFileName())) {
            ctx.labelSelectFileName().setText(defaultText != null ? defaultText : "Selected file: none");
        }

        if (nonNull(ctx.labelSuccess())) {
            ctx.labelSuccess().setVisible(true);
            ctx.labelSuccess().setText("");
            hideSuccessMessage(ctx.labelSuccess(), ctx.progressBar(), properties.getHideSuccessMessageTimer(), ctx.managed());
            ctx.labelSuccess().setManaged(ctx.managed());
        }

        resetDropZone(ctx.textDragZone(), ctx.dropZone(), ctx.textForDragZone());

        if (nonNull(ctx.imageViewPreview())) {
            ctx.imageViewPreview().setImage(null);
        }
        if (nonNull(ctx.labelPreviewPlaceholder())) {
            ctx.labelPreviewPlaceholder().setVisible(true);
        }
    }


    public static void resetDropZone(@NonNull Label textDragZone, @NonNull StackPane dropZone, String text) {
        textDragZone.setText("Drag " +  text + " here");

        dropZone.getStyleClass().removeAll(java.util.Collections.singleton("drop-zone-filled"));
    }

    public static void bindingImageViewToPreviewContainer(ImageView imageViewPreview, StackPane previewContainer) {
        if(nonNull(imageViewPreview) && nonNull(previewContainer)) {
            if (!imageViewPreview.fitWidthProperty().isBound()) {
                imageViewPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(10));
            }
            if (!imageViewPreview.fitHeightProperty().isBound()) {
                imageViewPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(10));
            }
        }
    }

    protected void setImagePreview(BufferedImage image, ImageView imageViewPreview) {
        if (nonNull(image) && nonNull(imageViewPreview)) {
            imageViewPreview.setImage(SwingFXUtils.toFXImage(image, null));
        }
    }
}
