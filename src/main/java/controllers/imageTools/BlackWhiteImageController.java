package controllers.imageTools;

import javafx.application.Platform;
import javafx.fxml.FXML;
import controllers.abstractControllers.AbstractImageToolController;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.dialogs.InfoAlert;

import java.io.File;

import static java.util.Objects.isNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class BlackWhiteImageController extends AbstractImageToolController {
    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupTooltips();

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Black-white");
        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        ImageTools.blackAndWhiteImage(originalImage).ifPresent(bw -> {
            processedImage = bw;
            setImagePreview(processedImage, imagePreview);
        });
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Black-White Image",
                "3. Click \"Download\" to apply the effect;",
                "This tool will make your image black and white."
        );
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, btnSubmit, btnReset, btnSubmitAndCopy);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, btnSubmit, btnReset, btnSubmitAndCopy);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image black-white successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Black-White image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imagePreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        disableControls();
    }
}
