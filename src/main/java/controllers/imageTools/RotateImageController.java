package controllers.imageTools;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import controllers.abstractControllers.AbstractImageToolController;
import model.enums.RotateSide;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.dialogs.InfoAlert;

import java.io.File;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class RotateImageController extends AbstractImageToolController {
    @FXML private Button btnFlipHorizontally, btnFlipVertically;
    @FXML private Button btnRotateImageRight, btnRotateImageLeft;

    private RotateSide rotateSide;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Rotated");
        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Rotate Image",
                """
                        3. Select which direction you want to rotate or flip the image by clicking the corresponding button;
                        
                        4. Click "Rotate and Download";
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

        processedImage = null;
        originalImage = null;
        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(processedImage)) {
            processedImage = originalImage;
            setImagePreview(processedImage, imagePreview);
            return;
        }

        if (nonNull(rotateSide)) {
            ImageTools.rotateImage(processedImage, rotateSide).ifPresent(rotated -> {
                processedImage = rotated;
                setImagePreview(processedImage, imagePreview);
            });
            rotateSide = null;
        }
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, btnFlipHorizontally, btnFlipVertically, btnRotateImageRight,
                btnRotateImageLeft, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, btnFlipHorizontally, btnFlipVertically, btnRotateImageRight,
                btnRotateImageLeft, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image rotation successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Rotated image saved!", imageProperties.getHideSuccessMessageTimer());
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

    public void onActionRotate(ActionEvent actionEvent) {
        Button button = (Button) actionEvent.getSource();

        switch (button.getId()) {
            case "btnRotateImageLeft"  -> rotateSide = RotateSide.LEFT;
            case "btnRotateImageRight" -> rotateSide = RotateSide.RIGHT;
            case "btnFlipHorizontally" -> rotateSide = RotateSide.HORIZONTALLY;
            case "btnFlipVertically"   -> rotateSide = RotateSide.VERTICALLY;
            default -> throw new IllegalArgumentException("Unexpected value: " + button.getId());
        }

        generatePreview();
     }
}
