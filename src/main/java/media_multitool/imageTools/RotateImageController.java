package media_multitool.imageTools;

import com.imagetools.ImageTools;
import com.imagetools.RotateSide;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import media_multitool.AbstractImageToolController;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;

import java.io.File;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class RotateImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnFlipHorizontally, btnFlipVertically;
    @FXML private Button btnRotateImageRight, btnRotateImageLeft, btnSubmit;
    @FXML private StackPane previewContainer;
    @FXML private ImageView preview;

    private List<Button> listControls;
    private RotateSide rotateSide;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return preview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(btnFlipHorizontally, btnFlipVertically, btnRotateImageRight, btnRotateImageLeft, btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Rotated");
        bindingImageViewToPreviewContainer(preview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
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
                dropZone, preview, progressBar, true, "image"
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
            setImagePreview(processedImage, preview);
            return;
        }

        if (nonNull(rotateSide)) {
            ImageTools.rotateImage(processedImage, rotateSide).ifPresent(rotated -> {
                processedImage = rotated;
                setImagePreview(processedImage, preview);
            });
            rotateSide = null;
        }
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
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
