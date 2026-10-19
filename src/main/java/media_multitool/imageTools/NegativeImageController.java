package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.fxml.FXML;
import media_multitool.AbstractImageToolController;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.InfoAlert;

import java.io.File;

import static java.util.Objects.isNull;
import static model.utility.PathWorker.getSavedPath;
import static visual.Message.*;

public class NegativeImageController extends AbstractImageToolController {
    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Negative");
        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Negative Image",
                """
                        3. Use the slider to set how much you want to lighten the image;
                        
                        4. Click "Negative and Download";
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imagePreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");
        originalImage = null;
        processedImage = null;
        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        ImageTools.toNegative(originalImage).ifPresent(negative -> {
            processedImage = negative;
            setImagePreview(processedImage, imagePreview);
        });
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image negative successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Negative image saved!", imageProperties.getHideSuccessMessageTimer());
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
}
