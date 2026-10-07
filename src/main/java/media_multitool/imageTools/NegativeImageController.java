package media_multitool.imageTools;

import com.imagetools.ImageTools;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
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
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class NegativeImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSubmit;
    @FXML private ImageView preview;
    @FXML private StackPane previewContainer;

    private List<Control> listControls;

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
        listControls = List.of(btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Negative");
        bindingImageViewToPreviewContainer(preview, previewContainer);

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
                dropZone, preview, progressBar, true, "image"
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
            setImagePreview(processedImage, preview);
        });
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
