package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import media_multitool.AbstractImageToolController;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.InfoAlert;
import visual.SliderSetup;

import java.io.File;

import static java.util.Objects.isNull;
import static model.utility.PathWorker.getSavedPath;
import static visual.Message.*;

public class LightenImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSubmit;
    @FXML private ImageView preview;
    @FXML private StackPane previewContainer;
    @FXML private Slider sliderLighten;
    @FXML private Label currentValueSlider;

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
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Lightened");
        sliderLighten.valueProperty().addListener((_, _, _) -> generatePreview());

        bindingImageViewToPreviewContainer(preview, previewContainer);

        sliderLighten.setMin(0);
        sliderLighten.setMax(255);
        sliderLighten.setValue(0);

        SliderSetup.bindPercentageLabel(sliderLighten, currentValueSlider, 255);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Lighten Image",
                """
                        3. Use the slider to set how much you want to lighten the image;
                        
                        4. Click "Lighten and Download";
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
        sliderLighten.setValue(0);
        currentValueSlider.setText("100%");
        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        int value = getSliderValue();

        if (value == 0) {
            processedImage = originalImage;
            setImagePreview(processedImage, preview);
            return;
        }

        ImageTools.brightnessImage(originalImage, value).ifPresent(lightened -> {
            processedImage = lightened;
            setImagePreview(processedImage, preview);
        });
    }


    @Override
    protected void disableControls() {
        setControlsDisabled(true, sliderLighten, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, sliderLighten, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @FXML
    private void handleSliderRelease() {
        generatePreview();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image lightening successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Lightened image saved!", imageProperties.getHideSuccessMessageTimer());
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

    private int getSliderValue() {
        return (int) sliderLighten.getValue();
    }
}
